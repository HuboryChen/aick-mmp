import React, { useState, useEffect, useRef } from 'react';
import { Card, Table, Button, Input, Progress, Tag, Modal, Form, Select, message, Space, Row, Col, Statistic, Descriptions, Alert, Spin } from 'antd';
import { SearchOutlined, StopOutlined, ReloadOutlined, HistoryOutlined, PlusOutlined, ThunderboltOutlined, ExperimentOutlined } from '@ant-design/icons';
import { cameraDiscoveryApi, cameraApi, regionApi } from '../utils/api';
import { Client } from '@stomp/stompjs';

const { Search } = Input;
const { Option } = Select;

const CameraDiscovery = () => {
  const [scanning, setScanning] = useState(false);
  const [networkSegment, setNetworkSegment] = useState('');
  const [currentTaskId, setCurrentTaskId] = useState(null);
  const [progress, setProgress] = useState({ progress: 0, totalIps: 0, scannedIps: 0, status: '' });
  const [foundDevices, setFoundDevices] = useState([]);
  const [history, setHistory] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyPagination, setHistoryPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [addModalVisible, setAddModalVisible] = useState(false);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const [regions, setRegions] = useState([]);
  const [addForm] = Form.useForm();
  const stompRef = useRef(null);

  useEffect(() => {
    fetchHistory();
    fetchRegions();
    return () => {
      if (stompRef.current) {
        stompRef.current.deactivate();
      }
    };
  }, []);

  const connectWebSocket = (taskId) => {
    if (stompRef.current) {
      stompRef.current.deactivate();
    }

    const client = new Client({
      brokerURL: `ws://${window.location.hostname}:8080/ws/discovery`,
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/discovery/${taskId}`, (msg) => {
          try {
            const data = JSON.parse(msg.body);
            setProgress({
              progress: data.progress || 0,
              totalIps: data.totalIps || 0,
              scannedIps: data.scannedIps || 0,
              status: data.status || '',
            });
            if (data.status === 'COMPLETED' || data.status === 'FAILED' || data.status === 'CANCELLED') {
              setScanning(false);
              fetchHistory();
            }
          } catch (e) {
            console.error('Failed to parse scan progress:', e);
          }
        });
      },
      onWebSocketError: (err) => {
        console.warn('WebSocket connection error, falling back to polling');
      },
    });

    client.activate();
    stompRef.current = client;
  };

  const startScan = async () => {
    if (!networkSegment) {
      message.warning('请输入网段');
      return;
    }

    setScanning(true);
    setFoundDevices([]);
    setProgress({ progress: 0, totalIps: 0, scannedIps: 0, status: 'RUNNING' });

    try {
      const response = await cameraDiscoveryApi.startScan(networkSegment);
      const taskId = response.data?.taskId;
      if (taskId) {
        setCurrentTaskId(taskId);
        connectWebSocket(taskId);
        startPolling(taskId);
      }
    } catch (err) {
      message.error('启动扫描失败');
      setScanning(false);
    }
  };

  const pollingRef = useRef(null);

  const startPolling = (taskId) => {
    if (pollingRef.current) clearInterval(pollingRef.current);
    pollingRef.current = setInterval(async () => {
      try {
        const response = await cameraDiscoveryApi.getScanProgress(taskId);
        const data = response.data;
        setProgress({
          progress: data.progress || 0,
          totalIps: data.totalIps || 0,
          scannedIps: data.scannedIps || 0,
          status: data.status || '',
        });
        if (data.status === 'COMPLETED' || data.status === 'FAILED' || data.status === 'CANCELLED') {
          clearInterval(pollingRef.current);
          setScanning(false);
          fetchHistory();
        }
      } catch (err) {
        console.warn('Polling failed:', err);
      }
    }, 2000);
  };

  const cancelScan = async () => {
    if (!currentTaskId) return;
    try {
      await cameraDiscoveryApi.cancelScan(currentTaskId);
      message.success('扫描已取消');
      setScanning(false);
      if (pollingRef.current) clearInterval(pollingRef.current);
      fetchHistory();
    } catch (err) {
      message.error('取消失败');
    }
  };

  const testConnectivity = async (ip, port) => {
    try {
      const response = await cameraDiscoveryApi.testConnectivity(ip, port);
      const result = response.data;
      if (result.connected) {
        message.success(`${ip}:${port} 连接成功 (${result.responseTimeMs}ms)`);
      } else {
        message.warning(`${ip}:${port} 连接失败`);
      }
    } catch (err) {
      message.error('测试失败');
    }
  };

  const identifyDevice = async (ip, port) => {
    try {
      const response = await cameraDiscoveryApi.identifyDevice(ip, port);
      const device = response.data;
      Modal.info({
        title: '设备识别结果',
        content: (
          <div>
            <p>IP: {device.ip}</p>
            <p>端口: {device.port}</p>
            <p>品牌: {device.brand}</p>
            <p>型号: {device.model}</p>
            <p>协议: {device.protocol}</p>
            <p>识别状态: {device.identified ? <Tag color="green">已识别</Tag> : <Tag color="orange">未识别</Tag>}</p>
          </div>
        ),
      });
    } catch (err) {
      message.error('识别失败');
    }
  };

  const fetchHistory = async () => {
    setHistoryLoading(true);
    try {
      const params = {
        page: historyPagination.current - 1,
        size: historyPagination.pageSize,
      };
      const response = await cameraDiscoveryApi.getScanHistory(params);
      setHistory(response.data?.content || []);
      setHistoryPagination(prev => ({ ...prev, total: response.data?.totalElements || 0 }));
    } catch (err) {
      console.error('Failed to fetch scan history:', err);
    } finally {
      setHistoryLoading(false);
    }
  };

  const fetchRegions = async () => {
    try {
      const response = await regionApi.getAllRegions();
      setRegions(response.data || []);
    } catch (err) {
      console.error('Failed to fetch regions:', err);
    }
  };

  const handleAddDevice = (device) => {
    setSelectedDevice(device);
    addForm.setFieldsValue({
      cameraName: `${device.brand || 'Unknown'} - ${device.ip}`,
      ip: device.ip,
      port: device.port,
      protocol: device.protocol || 'RTSP',
    });
    setAddModalVisible(true);
  };

  const doAddCamera = async () => {
    try {
      const values = await addForm.validateFields();
      await cameraApi.createCamera({
        name: values.cameraName,
        ip: values.ip,
        port: values.port,
        protocol: values.protocol || 'RTSP',
        regionId: values.regionId,
        username: values.username,
        password: values.password,
        status: 'OFFLINE',
      });
      message.success('摄像头添加成功');
      setAddModalVisible(false);
    } catch (err) {
      if (err.errorFields) return;
      message.error('添加失败');
    }
  };

  const statusTag = (status) => {
    const colors = { RUNNING: 'processing', COMPLETED: 'success', FAILED: 'error', CANCELLED: 'warning', PENDING: 'default' };
    const labels = { RUNNING: '扫描中', COMPLETED: '已完成', FAILED: '失败', CANCELLED: '已取消', PENDING: '等待中' };
    return <Tag color={colors[status] || 'default'}>{labels[status] || status}</Tag>;
  };

  const historyColumns = [
    { title: '网段', dataIndex: 'networkSegment', key: 'networkSegment', width: 150 },
    { title: '状态', dataIndex: 'status', key: 'status', width: 90, render: (s) => statusTag(s) },
    { title: '进度', dataIndex: 'progress', key: 'progress', width: 80, render: (p) => `${p || 0}%` },
    { title: '总IP数', dataIndex: 'totalIps', key: 'totalIps', width: 80 },
    { title: '开始时间', dataIndex: 'startedAt', key: 'startedAt', width: 160, render: (t) => t ? new Date(t).toLocaleString() : '-' },
    { title: '完成时间', dataIndex: 'completedAt', key: 'completedAt', width: 160, render: (t) => t ? new Date(t).toLocaleString() : '-' },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Card title="网络扫描" variant="outlined" className="mb-4">
        <Row gutter={[16, 16]} align="middle">
          <Col flex="auto">
            <Search
              placeholder="输入网段 (CIDR格式，如 192.168.1.0/24)"
              enterButton="开始扫描"
              size="large"
              value={networkSegment}
              onChange={e => setNetworkSegment(e.target.value)}
              onSearch={startScan}
              disabled={scanning}
              loading={scanning}
            />
          </Col>
          <Col>
            <Button icon={<StopOutlined />} onClick={cancelScan} danger disabled={!scanning}>取消</Button>
          </Col>
        </Row>
      </Card>

      {scanning && (
        <Card title="扫描进度" variant="outlined" style={{ marginBottom: 16 }}>
          <Spin spinning={scanning}>
            <Descriptions column={3} size="small">
              <Descriptions.Item label="状态">{statusTag(progress.status || 'RUNNING')}</Descriptions.Item>
              <Descriptions.Item label="已扫描">{progress.scannedIps} / {progress.totalIps}</Descriptions.Item>
              <Descriptions.Item label="进度">{progress.progress}%</Descriptions.Item>
            </Descriptions>
            <Progress percent={progress.progress} status={progress.status === 'FAILED' ? 'exception' : 'active'} style={{ marginTop: 12 }} />
          </Spin>
        </Card>
      )}

      <Card
        title="发现设备"
        variant="outlined"
        style={{ marginBottom: 16 }}
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={fetchHistory}>刷新</Button>
          </Space>
        }
      >
        {foundDevices.length === 0 && !scanning ? (
          <Alert message="输入网段并开始扫描，发现的设备将显示在此处" type="info" showIcon />
        ) : (
          <Table
            dataSource={foundDevices}
            columns={[
              { title: 'IP', dataIndex: 'ip', key: 'ip', width: 140 },
              { title: '端口', dataIndex: 'port', key: 'port', width: 70 },
              { title: '品牌', dataIndex: 'brand', key: 'brand', width: 100 },
              { title: '型号', dataIndex: 'model', key: 'model', width: 150 },
              { title: '协议', dataIndex: 'protocol', key: 'protocol', width: 80 },
              {
                title: '操作', key: 'action', width: 200,
                render: (_, record) => (
                  <Space size="small">
                    <Button size="small" icon={<ThunderboltOutlined />} onClick={() => testConnectivity(record.ip, record.port)}>连通测试</Button>
                    <Button size="small" icon={<ExperimentOutlined />} onClick={() => identifyDevice(record.ip, record.port)}>识别</Button>
                    <Button size="small" type="primary" icon={<PlusOutlined />} onClick={() => handleAddDevice(record)}>添加</Button>
                  </Space>
                ),
              },
            ]}
            rowKey={(r) => `${r.ip}-${r.port}`}
            size="small"
            pagination={false}
          />
        )}
      </Card>

      <Card title="扫描历史" variant="outlined">
        <Table
          columns={historyColumns}
          dataSource={history}
          rowKey="id"
          loading={historyLoading}
          pagination={{
            ...historyPagination,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
          }}
          onChange={(pag) => setHistoryPagination(prev => ({ ...prev, current: pag.current, pageSize: pag.pageSize }))}
          size="small"
        />
      </Card>

      <Modal
        title="添加摄像头"
        open={addModalVisible}
        onOk={doAddCamera}
        onCancel={() => setAddModalVisible(false)}
        width={500}
      >
        <Form form={addForm} layout="vertical">
          <Form.Item name="cameraName" label="摄像头名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="ip" label="IP地址">
                <Input disabled />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="port" label="端口">
                <Input disabled />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="regionId" label="所属区域">
            <Select placeholder="选择区域" allowClear>
              {regions.map(r => <Option key={r.id} value={r.id}>{r.name}</Option>)}
            </Select>
          </Form.Item>
          <Row gutter={[16, 16]}>
            <Col span={12}>
              <Form.Item name="username" label="用户名">
                <Input placeholder="admin" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="password" label="密码">
                <Input.Password placeholder="请输入密码" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
};

export default CameraDiscovery;
