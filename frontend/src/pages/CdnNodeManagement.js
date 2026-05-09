import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  Table, Button, Space, Tag, Modal, Form, Input, Select, message,
  Popconfirm, Tabs, Descriptions, Spin, Row, Col, Progress, Card,
  Statistic, InputNumber, Switch, Divider, Alert, Tooltip
} from 'antd';
import {
  PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined,
  SearchOutlined, CloudOutlined, ExperimentOutlined,
  ExclamationCircleOutlined, CheckCircleOutlined, CloseCircleOutlined,
  SyncOutlined, RadarChartOutlined
} from '@ant-design/icons';
import { Line } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale, LinearScale, PointElement, LineElement,
  Title, Tooltip as ChartTooltip, Legend, Filler
} from 'chart.js';
import { cdnNodeApi, regionApi } from '../utils/api';
import PageContainer from '../components/PageContainer';
import '../styles/CdnNodeManagement.css';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, ChartTooltip, Legend, Filler);

const { Option } = Select;
const { TabPane } = Tabs;

const CdnNodeManagement = () => {
  const [cdnNodes, setCdnNodes] = useState([]);
  const [regions, setRegions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [connectivityModalVisible, setConnectivityModalVisible] = useState(false);
  const [editingNode, setEditingNode] = useState(null);
  const [nodeDetail, setNodeDetail] = useState(null);
  const [loadHistory, setLoadHistory] = useState([]);
  const [connectivityResult, setConnectivityResult] = useState(null);
  const [nodeStats, setNodeStats] = useState(null);
  const [form] = Form.useForm();
  const [searchKeyword, setSearchKeyword] = useState('');
  const [filterStatus, setFilterStatus] = useState(undefined);
  const [filterRegionId, setFilterRegionId] = useState(undefined);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });
  const chartRef = useRef(null);

  const fetchCdnNodes = useCallback(async () => {
    setLoading(true);
    try {
      const params = {
        page: pagination.current - 1,
        size: pagination.pageSize,
      };
      if (searchKeyword) params.keyword = searchKeyword;
      if (filterStatus) params.status = filterStatus;
      if (filterRegionId) params.regionId = filterRegionId;

      const response = await cdnNodeApi.getCdnNodes(params);
      const content = response.data?.content || [];
      const totalElements = response.data?.totalElements || 0;

      setCdnNodes(Array.isArray(content) ? content : []);
      setPagination(prev => ({
        ...prev,
        total: totalElements,
      }));
    } catch (error) {
      console.error('获取CDN节点列表失败:', error);
      message.error('获取CDN节点列表失败');
      setCdnNodes([]);
    } finally {
      setLoading(false);
    }
  }, [searchKeyword, filterStatus, filterRegionId, pagination]);

  const fetchRegions = async () => {
    try {
      const response = await regionApi.getRegionsFlat();
      setRegions(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error('获取地区列表失败:', error);
    }
  };

  const fetchNodeDetail = async (id) => {
    setDetailLoading(true);
    try {
      const [detailRes, statsRes] = await Promise.all([
        cdnNodeApi.getCdnNode(id),
        cdnNodeApi.getCdnNodeStats(id)
      ]);
      setNodeDetail(detailRes.data);
      setNodeStats(statsRes.data);
    } catch (error) {
      console.error('获取节点详情失败:', error);
      message.error('获取节点详情失败');
    } finally {
      setDetailLoading(false);
    }
  };

  const fetchLoadHistory = async (id) => {
    try {
      const response = await cdnNodeApi.getCdnNodeLoad(id, { hours: 24 });
      setLoadHistory(response.data || []);
    } catch (error) {
      console.error('获取负载历史失败:', error);
      setLoadHistory([]);
    }
  };

  useEffect(() => {
    fetchCdnNodes();
    fetchRegions();
  }, [fetchCdnNodes]);

  const handleSearch = () => {
    setPagination(prev => ({ ...prev, current: 1 }));
    fetchCdnNodes();
  };

  const handleReset = () => {
    setSearchKeyword('');
    setFilterStatus(undefined);
    setFilterRegionId(undefined);
    setPagination(prev => ({ ...prev, current: 1 }));
  };

  const handleAdd = () => {
    setEditingNode(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (node) => {
    setEditingNode(node);
    form.setFieldsValue({
      name: node.name,
      ipAddress: node.ipAddress,
      port: node.port,
      location: node.location,
      regionId: node.regionId,
      capacity: node.capacity,
      weight: node.weight,
      priority: node.priority,
      healthCheckUrl: node.healthCheckUrl,
      connectTimeout: node.connectTimeout,
      readTimeout: node.readTimeout,
      isEnabled: node.isEnabled,
    });
    setModalVisible(true);
  };

  const handleViewDetail = async (record) => {
    setNodeDetail(record);
    await fetchNodeDetail(record.id);
    await fetchLoadHistory(record.id);
    setDetailModalVisible(true);
  };

  const handleDelete = async (id) => {
    try {
      await cdnNodeApi.deleteCdnNode(id);
      message.success('删除成功');
      fetchCdnNodes();
    } catch (error) {
      console.error('删除失败:', error);
      message.error('删除失败: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleSubmit = async (values) => {
    try {
      if (editingNode) {
        await cdnNodeApi.updateCdnNode(editingNode.id, values);
        message.success('更新成功');
      } else {
        await cdnNodeApi.createCdnNode(values);
        message.success('添加成功');
      }
      setModalVisible(false);
      fetchCdnNodes();
    } catch (error) {
      console.error('操作失败:', error);
      message.error('操作失败: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleTestConnectivity = async (node) => {
    setConnectivityResult(null);
    setConnectivityModalVisible(true);
    try {
      const response = await cdnNodeApi.testConnectivity(node.id);
      setConnectivityResult(response.data);
    } catch (error) {
      console.error('连通性测试失败:', error);
      setConnectivityResult({
        reachable: false,
        latency: null,
        error: error.response?.data?.message || error.message,
      });
    }
  };

  const handleBatchHealthCheck = async () => {
    try {
      message.loading({ content: '正在执行批量健康检查...', key: 'healthCheck' });
      await cdnNodeApi.batchHealthCheck();
      message.success({ content: '批量健康检查完成', key: 'healthCheck' });
      fetchCdnNodes();
    } catch (error) {
      console.error('批量健康检查失败:', error);
      message.error('批量健康检查失败');
    }
  };

  const getStatusTag = (status) => {
    const statusMap = {
      ONLINE: { color: 'green', text: '在线', icon: <CheckCircleOutlined /> },
      OFFLINE: { color: 'red', text: '离线', icon: <CloseCircleOutlined /> },
      MAINTENANCE: { color: 'orange', text: '维护中', icon: <ExclamationCircleOutlined /> },
      DEGRADED: { color: 'gold', text: '降级', icon: <ExclamationCircleOutlined /> },
      UPGRADING: { color: 'blue', text: '升级中', icon: <SyncOutlined /> },
    };
    const config = statusMap[status] || { color: 'default', text: status, icon: null };
    return (
      <Tag color={config.color} icon={config.icon}>
        {config.text}
      </Tag>
    );
  };

  const getLoadPercentage = (currentLoad, capacity) => {
    if (!capacity || capacity === 0) return 0;
    return Math.round((currentLoad / capacity) * 100);
  };

  const columns = [
    {
      title: '节点名称',
      dataIndex: 'name',
      key: 'name',
      render: (name, record) => (
        <Space>
          <CloudOutlined style={{ color: 'var(--color-accent)' }} />
          <Button type="link" onClick={() => handleViewDetail(record)}>{name}</Button>
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: getStatusTag,
    },
    {
      title: 'IP地址',
      dataIndex: 'ipAddress',
      key: 'ipAddress',
    },
    {
      title: '端口',
      dataIndex: 'port',
      key: 'port',
    },
    {
      title: '所属区域',
      dataIndex: 'regionName',
      key: 'regionName',
      render: (text, record) => text || record.regionId || '-',
    },
    {
      title: '负载',
      key: 'load',
      render: (_, record) => {
        const percentage = getLoadPercentage(record.currentLoad, record.capacity);
        return (
          <Tooltip title={`${record.currentLoad || 0} / ${record.capacity || 0}`}>
            <Progress
              percent={percentage}
              size="small"
              status={percentage > 90 ? 'exception' : percentage > 70 ? 'warning' : 'normal'}
              style={{ width: 80 }}
            />
          </Tooltip>
        );
      },
    },
    {
      title: 'CPU',
      dataIndex: 'cpuUsage',
      key: 'cpuUsage',
      render: (value) => value !== undefined && value !== null ? (
        <Progress
          percent={Math.round(value)}
          size="small"
          status={value > 90 ? 'exception' : value > 70 ? 'warning' : 'normal'}
          style={{ width: 60 }}
        />
      ) : '-',
    },
    {
      title: '内存',
      dataIndex: 'memoryUsage',
      key: 'memoryUsage',
      render: (value) => value !== undefined && value !== null ? (
        <Progress
          percent={Math.round(value)}
          size="small"
          status={value > 90 ? 'exception' : value > 70 ? 'warning' : 'normal'}
          style={{ width: 60 }}
        />
      ) : '-',
    },
    {
      title: '心跳',
      dataIndex: 'lastHeartbeat',
      key: 'lastHeartbeat',
      render: (time) => time ? new Date(time).toLocaleString() : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => handleViewDetail(record)}>详情</Button>
          <Button type="link" size="small" onClick={() => handleTestConnectivity(record)} icon={<ExperimentOutlined />}>测试</Button>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>编辑</Button>
          <Popconfirm
            title="确认删除"
            description="确定要删除这个CDN节点吗？"
            onConfirm={() => handleDelete(record.id)}
            okText="确认"
            cancelText="取消"
          >
            <Button type="link" size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  // 统计卡片数据
  const statsData = {
    total: cdnNodes.length,
    online: cdnNodes.filter(n => n.status === 'ONLINE').length,
    offline: cdnNodes.filter(n => n.status === 'OFFLINE').length,
    degraded: cdnNodes.filter(n => ['DEGRADED', 'MAINTENANCE', 'UPGRADING'].includes(n.status)).length,
  };

  // 负载历史图表配置
  const loadChartData = {
    labels: loadHistory.map(h => new Date(h.recordedAt).toLocaleTimeString()),
    datasets: [
      {
        label: '负载',
        data: loadHistory.map(h => h.currentLoad),
        borderColor: 'rgb(75, 192, 192)',
        backgroundColor: 'rgba(75, 192, 192, 0.2)',
        fill: true,
        tension: 0.4,
      },
    ],
  };

  const loadChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: true, position: 'top' },
      title: { display: true, text: '负载趋势 (24小时)' },
    },
    scales: {
      y: { beginAtZero: true },
    },
  };

  const cpuChartData = {
    labels: loadHistory.map(h => new Date(h.recordedAt).toLocaleTimeString()),
    datasets: [
      {
        label: 'CPU使用率',
        data: loadHistory.map(h => h.cpuUsage),
        borderColor: 'rgb(255, 99, 132)',
        backgroundColor: 'rgba(255, 99, 132, 0.2)',
        fill: true,
        tension: 0.4,
      },
    ],
  };

  const cpuChartOptions = {
    ...loadChartOptions,
    plugins: {
      ...loadChartOptions.plugins,
      title: { display: true, text: 'CPU使用率趋势 (24小时)' },
    },
    scales: {
      y: { beginAtZero: true, max: 100 },
    },
  };

  return (
    <PageContainer
      title="CDN节点管理"
      icon={<CloudOutlined />}
      actions={
        <Space>
          <Button icon={<ReloadOutlined />} onClick={fetchCdnNodes}>
            刷新
          </Button>
          <Button icon={<RadarChartOutlined />} onClick={handleBatchHealthCheck}>
            批量健康检查
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            添加节点
          </Button>
        </Space>
      }
    >
      {/* 统计卡片 */}
      <Row gutter={16} className="cdn-stats-row">
        <Col span={6}>
          <Card className="cdn-stat-card">
            <Statistic
              title="节点总数"
              value={statsData.total}
              prefix={<CloudOutlined />}
              valueStyle={{ color: 'var(--color-accent)' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card className="cdn-stat-card">
            <Statistic
              title="在线节点"
              value={statsData.online}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card className="cdn-stat-card">
            <Statistic
              title="离线节点"
              value={statsData.offline}
              prefix={<CloseCircleOutlined />}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card className="cdn-stat-card">
            <Statistic
              title="异常节点"
              value={statsData.degraded}
              prefix={<ExclamationCircleOutlined />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 搜索区域 */}
      <Card className="cdn-search-card">
        <Space wrap>
          <Input
            placeholder="搜索节点名称或IP地址"
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            onPressEnter={handleSearch}
            style={{ width: 200 }}
            prefix={<SearchOutlined />}
          />
          <Select
            placeholder="状态"
            value={filterStatus}
            onChange={(val) => {
              setFilterStatus(val);
              setPagination(prev => ({ ...prev, current: 1 }));
            }}
            style={{ width: 140 }}
            allowClear
          >
            <Option value="ONLINE">在线</Option>
            <Option value="OFFLINE">离线</Option>
            <Option value="MAINTENANCE">维护中</Option>
            <Option value="DEGRADED">降级</Option>
            <Option value="UPGRADING">升级中</Option>
          </Select>
          <Select
            placeholder="区域"
            value={filterRegionId}
            onChange={(val) => {
              setFilterRegionId(val);
              setPagination(prev => ({ ...prev, current: 1 }));
            }}
            style={{ width: 150 }}
            allowClear
          >
            {Array.isArray(regions) && regions.map(region => (
              <Option key={region.id} value={region.id}>{region.name}</Option>
            ))}
          </Select>
          <Button type="primary" onClick={handleSearch}>搜索</Button>
          <Button onClick={handleReset}>重置</Button>
        </Space>
      </Card>

      {/* 节点列表 */}
      <Table
        columns={columns}
        dataSource={cdnNodes}
        loading={loading}
        rowKey="id"
        pagination={{
          ...pagination,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条`,
          onChange: (page, pageSize) => setPagination({ current: page, pageSize, total: pagination.total }),
        }}
        className="cdn-nodes-table"
      />

      {/* 添加/编辑节点弹窗 */}
      <Modal
        title={editingNode ? '编辑CDN节点' : '添加CDN节点'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={() => form.submit()}
        width={600}
        destroyOnClose
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{ isEnabled: true, port: 8080, weight: 100, priority: 1 }}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="name"
                label="节点名称"
                rules={[{ required: true, message: '请输入节点名称' }]}
              >
                <Input placeholder="例如: CDN-Node-Beijing-01" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="ipAddress"
                label="IP地址"
                rules={[{ required: true, message: '请输入IP地址' }]}
              >
                <Input placeholder="例如: 192.168.1.100" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="port"
                label="端口"
                rules={[{ required: true, message: '请输入端口' }]}
              >
                <InputNumber min={1} max={65535} style={{ width: '100%' }} placeholder="8080" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="regionId"
                label="所属区域"
              >
                <Select placeholder="请选择区域（可选）" allowClear>
                  {Array.isArray(regions) && regions.map(region => (
                    <Option key={region.id} value={region.id}>{region.name}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="location"
                label="详细地址"
              >
                <Input placeholder="例如: XX街道XX号（可选）" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                name="capacity"
                label="最大容量"
                rules={[{ required: true, message: '请输入最大容量' }]}
              >
                <InputNumber min={1} style={{ width: '100%' }} placeholder="100" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="weight"
                label="权重(WLC)"
                tooltip="用于加权最小连接数算法"
              >
                <InputNumber min={1} max={1000} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="priority"
                label="优先级"
                tooltip="数字越小优先级越高"
              >
                <InputNumber min={1} max={100} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="healthCheckUrl"
            label="健康检查URL"
            tooltip="用于连通性测试的HTTP URL"
          >
            <Input placeholder="例如: http://192.168.1.100:8080/health" />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="connectTimeout"
                label="连接超时(ms)"
              >
                <InputNumber min={100} max={30000} style={{ width: '100%' }} placeholder="5000" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="readTimeout"
                label="读取超时(ms)"
              >
                <InputNumber min={100} max={60000} style={{ width: '100%' }} placeholder="10000" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="isEnabled"
            label="是否启用"
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>
        </Form>
      </Modal>

      {/* 节点详情弹窗 */}
      <Modal
        title={
          <Space>
            <CloudOutlined />
            {nodeDetail?.name || '节点详情'}
          </Space>
        }
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={null}
        width={900}
      >
        {detailLoading ? (
          <div style={{ textAlign: 'center', padding: '50px' }}>
            <Spin size="large" />
          </div>
        ) : nodeDetail ? (
          <Tabs defaultActiveKey="overview">
            <TabPane tab="概览" key="overview">
              <Row gutter={[16, 16]}>
                <Col span={12}>
                  <Descriptions column={1} bordered size="small">
                    <Descriptions.Item label="节点ID">{nodeDetail.id}</Descriptions.Item>
                    <Descriptions.Item label="节点名称">{nodeDetail.name}</Descriptions.Item>
                    <Descriptions.Item label="IP地址">{nodeDetail.ipAddress}</Descriptions.Item>
                    <Descriptions.Item label="端口">{nodeDetail.port}</Descriptions.Item>
                    <Descriptions.Item label="状态">{getStatusTag(nodeDetail.status)}</Descriptions.Item>
                    <Descriptions.Item label="所属区域">{nodeDetail.regionName || '-'}</Descriptions.Item>
                    <Descriptions.Item label="详细地址">{nodeDetail.location || '-'}</Descriptions.Item>
                  </Descriptions>
                </Col>
                <Col span={12}>
                  <Descriptions column={1} bordered size="small">
                    <Descriptions.Item label="最大容量">{nodeDetail.capacity || '-'}</Descriptions.Item>
                    <Descriptions.Item label="当前负载">{nodeDetail.currentLoad || 0}</Descriptions.Item>
                    <Descriptions.Item label="负载率">
                      <Progress
                        percent={getLoadPercentage(nodeDetail.currentLoad, nodeDetail.capacity)}
                        size="small"
                        status={getLoadPercentage(nodeDetail.currentLoad, nodeDetail.capacity) > 90 ? 'exception' : 'normal'}
                      />
                    </Descriptions.Item>
                    <Descriptions.Item label="权重">{nodeDetail.weight || 100}</Descriptions.Item>
                    <Descriptions.Item label="优先级">{nodeDetail.priority || 1}</Descriptions.Item>
                    <Descriptions.Item label="是否启用">
                      {nodeDetail.isEnabled ? <Tag color="green">是</Tag> : <Tag color="red">否</Tag>}
                    </Descriptions.Item>
                  </Descriptions>
                </Col>
              </Row>

              <Divider>系统指标</Divider>
              <Row gutter={[16, 16]}>
                <Col span={6}>
                  <Card size="small" className="metric-card">
                    <Statistic
                      title="CPU使用率"
                      value={nodeDetail.cpuUsage || 0}
                      suffix="%"
                      valueStyle={{ color: (nodeDetail.cpuUsage || 0) > 90 ? '#ff4d4f' : (nodeDetail.cpuUsage || 0) > 70 ? '#faad14' : '#52c41a' }}
                    />
                    <Progress
                      percent={Math.round(nodeDetail.cpuUsage || 0)}
                      showInfo={false}
                      strokeColor={(nodeDetail.cpuUsage || 0) > 90 ? '#ff4d4f' : (nodeDetail.cpuUsage || 0) > 70 ? '#faad14' : '#52c41a'}
                    />
                  </Card>
                </Col>
                <Col span={6}>
                  <Card size="small" className="metric-card">
                    <Statistic
                      title="内存使用率"
                      value={nodeDetail.memoryUsage || 0}
                      suffix="%"
                      valueStyle={{ color: (nodeDetail.memoryUsage || 0) > 90 ? '#ff4d4f' : (nodeDetail.memoryUsage || 0) > 70 ? '#faad14' : '#52c41a' }}
                    />
                    <Progress
                      percent={Math.round(nodeDetail.memoryUsage || 0)}
                      showInfo={false}
                      strokeColor={(nodeDetail.memoryUsage || 0) > 90 ? '#ff4d4f' : (nodeDetail.memoryUsage || 0) > 70 ? '#faad14' : '#52c41a'}
                    />
                  </Card>
                </Col>
                <Col span={6}>
                  <Card size="small" className="metric-card">
                    <Statistic
                      title="带宽使用率"
                      value={nodeDetail.bandwidthUsage || 0}
                      suffix="%"
                      valueStyle={{ color: (nodeDetail.bandwidthUsage || 0) > 90 ? '#ff4d4f' : (nodeDetail.bandwidthUsage || 0) > 70 ? '#faad14' : '#52c41a' }}
                    />
                    <Progress
                      percent={Math.round(nodeDetail.bandwidthUsage || 0)}
                      showInfo={false}
                      strokeColor={(nodeDetail.bandwidthUsage || 0) > 90 ? '#ff4d4f' : (nodeDetail.bandwidthUsage || 0) > 70 ? '#faad14' : '#52c41a'}
                    />
                  </Card>
                </Col>
                <Col span={6}>
                  <Card size="small" className="metric-card">
                    <Statistic
                      title="存储使用率"
                      value={nodeDetail.storageUsage || 0}
                      suffix="%"
                      valueStyle={{ color: (nodeDetail.storageUsage || 0) > 90 ? '#ff4d4f' : (nodeDetail.storageUsage || 0) > 70 ? '#faad14' : '#52c41a' }}
                    />
                    <Progress
                      percent={Math.round(nodeDetail.storageUsage || 0)}
                      showInfo={false}
                      strokeColor={(nodeDetail.storageUsage || 0) > 90 ? '#ff4d4f' : (nodeDetail.storageUsage || 0) > 70 ? '#faad14' : '#52c41a'}
                    />
                  </Card>
                </Col>
              </Row>

              <Divider>带宽信息</Divider>
              <Row gutter={[16, 16]}>
                <Col span={12}>
                  <Descriptions column={1} bordered size="small">
                    <Descriptions.Item label="上行带宽">
                      {nodeDetail.upBandwidth ? `${nodeDetail.upBandwidth} Mbps` : '-'}
                    </Descriptions.Item>
                    <Descriptions.Item label="下行带宽">
                      {nodeDetail.downBandwidth ? `${nodeDetail.downBandwidth} Mbps` : '-'}
                    </Descriptions.Item>
                  </Descriptions>
                </Col>
                <Col span={12}>
                  <Descriptions column={1} bordered size="small">
                    <Descriptions.Item label="最后心跳">
                      {nodeDetail.lastHeartbeat ? new Date(nodeDetail.lastHeartbeat).toLocaleString() : '-'}
                    </Descriptions.Item>
                    <Descriptions.Item label="创建时间">
                      {nodeDetail.createdAt ? new Date(nodeDetail.createdAt).toLocaleString() : '-'}
                    </Descriptions.Item>
                  </Descriptions>
                </Col>
              </Row>
            </TabPane>

            <TabPane tab="负载趋势" key="load">
              {loadHistory.length > 0 ? (
                <div style={{ height: 300 }}>
                  <Line ref={chartRef} data={loadChartData} options={loadChartOptions} />
                </div>
              ) : (
                <Alert message="暂无负载历史数据" type="info" showIcon />
              )}
            </TabPane>

            <TabPane tab="CPU趋势" key="cpu">
              {loadHistory.length > 0 ? (
                <div style={{ height: 300 }}>
                  <Line data={cpuChartData} options={cpuChartOptions} />
                </div>
              ) : (
                <Alert message="暂无CPU历史数据" type="info" showIcon />
              )}
            </TabPane>

            <TabPane tab="统计信息" key="stats">
              {nodeStats ? (
                <Row gutter={[16, 16]}>
                  <Col span={8}>
                    <Card size="small">
                      <Statistic title="总请求数" value={nodeStats.totalRequests || 0} />
                    </Card>
                  </Col>
                  <Col span={8}>
                    <Card size="small">
                      <Statistic title="活跃连接数" value={nodeStats.activeConnections || 0} />
                    </Card>
                  </Col>
                  <Col span={8}>
                    <Card size="small">
                      <Statistic title="缓存命中率" value={nodeStats.cacheHitRate || 0} suffix="%" />
                    </Card>
                  </Col>
                  <Col span={8}>
                    <Card size="small">
                      <Statistic title="平均响应时间" value={nodeStats.avgResponseTime || 0} suffix="ms" />
                    </Card>
                  </Col>
                  <Col span={8}>
                    <Card size="small">
                      <Statistic title="错误率" value={nodeStats.errorRate || 0} suffix="%" />
                    </Card>
                  </Col>
                  <Col span={8}>
                    <Card size="small">
                      <Statistic title="请求速率" value={nodeStats.requestRate || 0} suffix="req/s" />
                    </Card>
                  </Col>
                </Row>
              ) : (
                <Alert message="暂无统计信息" type="info" showIcon />
              )}
            </TabPane>

            <TabPane tab="操作" key="actions">
              <Space wrap>
                <Button
                  icon={<ExperimentOutlined />}
                  onClick={() => {
                    setDetailModalVisible(false);
                    handleTestConnectivity(nodeDetail);
                  }}
                >
                  测试连通性
                </Button>
                <Button
                  icon={<EditOutlined />}
                  onClick={() => {
                    setDetailModalVisible(false);
                    handleEdit(nodeDetail);
                  }}
                >
                  编辑节点
                </Button>
                <Popconfirm
                  title="确认删除"
                  description="确定要删除这个CDN节点吗？"
                  onConfirm={() => {
                    setDetailModalVisible(false);
                    handleDelete(nodeDetail.id);
                  }}
                  okText="确认"
                  cancelText="取消"
                >
                  <Button danger icon={<DeleteOutlined />}>
                    删除节点
                  </Button>
                </Popconfirm>
              </Space>
            </TabPane>
          </Tabs>
        ) : null}
      </Modal>

      {/* 连通性测试结果弹窗 */}
      <Modal
        title={
          <Space>
            <ExperimentOutlined />
            连通性测试结果
          </Space>
        }
        open={connectivityModalVisible}
        onCancel={() => setConnectivityModalVisible(false)}
        footer={<Button onClick={() => setConnectivityModalVisible(false)}>关闭</Button>}
        width={500}
      >
        {connectivityResult ? (
          <div>
            <Alert
              type={connectivityResult.reachable ? 'success' : 'error'}
              message={
                connectivityResult.reachable ? (
                  <Space>
                    <CheckCircleOutlined />
                    <span>节点可连通</span>
                  </Space>
                ) : (
                  <Space>
                    <CloseCircleOutlined />
                    <span>节点不可连通</span>
                  </Space>
                )
              }
              style={{ marginBottom: 16 }}
            />
            <Descriptions column={1} bordered>
              <Descriptions.Item label="响应时间">
                {connectivityResult.latency ? `${connectivityResult.latency} ms` : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="HTTP状态码">
                {connectivityResult.httpStatus || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="响应内容">
                {connectivityResult.responseBody || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="错误信息">
                {connectivityResult.error || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="测试时间">
                {connectivityResult.testTime ? new Date(connectivityResult.testTime).toLocaleString() : '-'}
              </Descriptions.Item>
            </Descriptions>
          </div>
        ) : (
          <div style={{ textAlign: 'center', padding: '30px' }}>
            <Spin tip="正在测试连通性..." />
          </div>
        )}
      </Modal>
    </PageContainer>
  );
};

export default CdnNodeManagement;
