import React, { useState, useEffect } from 'react';
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Card, Typography, Popconfirm, Empty, Spin } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, PlayCircleOutlined, StopOutlined } from '@ant-design/icons';
import { cameraApi, edgeNodeApi, streamingApi, regionApi } from '../utils/api';

const { Title } = Typography;
const { Option } = Select;

const CameraManagement = () => {
  const [cameras, setCameras] = useState([]);
  const [edgeNodes, setEdgeNodes] = useState([]);
  const [regions, setRegions] = useState([]); // 添加地区列表状态
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingCamera, setEditingCamera] = useState(null);
  const [form] = Form.useForm();
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  useEffect(() => {
    fetchCameras();
    fetchEdgeNodes();
    fetchRegions(); // 获取地区列表
  }, []);

  const fetchCameras = async (params = {}) => {
    setLoading(true);
    try {
      const response = await cameraApi.getCameras({
        page: pagination.current - 1,
        size: pagination.pageSize,
        ...params
      });
      
      console.log('Camera API response:', response);
      
      // 确保数据结构正确
      // 处理摄像头数据，确保数据结构一致性
      const content = response.data?.content || [];
      const totalElements = response.data?.totalElements || 0;
      const pageNumber = response.data?.number || 0;
      
      // 确保摄像头数据结构正确
      const processedCameras = Array.isArray(content) ? content.map(camera => ({
        id: camera.id || 0,
        name: camera.name || '未命名',
        location: camera.location || '未指定',
        status: camera.status || 'UNKNOWN',
        edgeNodeName: camera.edgeNodeName || '未分配',
        resolution: camera.resolution || '未指定',
        protocol: camera.protocol || '未知',
        connectionUrl: camera.connectionUrl || '',
        edgeNodeId: camera.edgeNodeId || undefined
      })) : [];
      
      setCameras(processedCameras);
      setPagination({
        ...pagination,
        total: totalElements,
        current: pageNumber + 1,
      });
    } catch (error) {
      console.error('获取摄像头列表失败:', error);
      message.error('获取摄像头列表失败: ' + (error.response?.data?.message || error.message || '未知错误'));
      // 设置空数组以避免页面空白
      setCameras([]);
    } finally {
      setLoading(false);
    }
  };

  // 获取边缘节点列表
  const fetchEdgeNodes = async () => {
    try {
      const response = await edgeNodeApi.getEdgeNodes();
      setEdgeNodes(response.data || []);
    } catch (error) {
      console.error('获取边缘节点列表失败:', error);
      message.error('获取边缘节点列表失败: ' + (error.response?.data?.message || error.message || '未知错误'));
      setEdgeNodes([]);
    }
  };

  // 获取地区列表
  const fetchRegions = async () => {
    try {
      const response = await regionApi.getAllRegions();
      setRegions(response.data || []);
    } catch (error) {
      console.error('获取地区列表失败:', error);
      message.error('获取地区列表失败: ' + (error.response?.data?.message || error.message || '未知错误'));
      setRegions([]);
    }
  };

  const handleTableChange = (pager) => {
    setPagination(pager);
    fetchCameras({ page: pager.current - 1, size: pager.pageSize });
  };

  const columns = [
    {
      title: '摄像头名称',
      dataIndex: 'name',
      key: 'name',
      render: (text) => text || '未命名'
    },
    {
      title: '所属地区',
      dataIndex: 'location',
      key: 'location',
      render: (text) => text || '未指定'
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => (
        <Tag color={status === 'ONLINE' ? 'green' : status === 'OFFLINE' ? 'red' : 'orange'}>
          {status === 'ONLINE' ? '在线' : status === 'OFFLINE' ? '离线' : status || '未知'}
        </Tag>
      ),
    },
    {
      title: '边缘节点',
      dataIndex: 'edgeNodeName',
      key: 'edgeNodeName',
      render: (text) => text || '未分配'
    },
    {
      title: '分辨率',
      dataIndex: 'resolution',
      key: 'resolution',
      render: (text) => text || '未指定'
    },
    {
      title: '协议',
      dataIndex: 'protocol',
      key: 'protocol',
      render: (protocol) => <Tag>{protocol || '未知'}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="middle">
          {record.status === 'ONLINE' ? (
            <Button
              type="link"
              icon={<StopOutlined />}
              onClick={() => handleStop(record.id)}
            >
              停止
            </Button>
          ) : (
            <Button
              type="link"
              icon={<PlayCircleOutlined />}
              onClick={() => handleStart(record.id)}
            >
              启动
            </Button>
          )}
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确认删除"
            description="确定要删除这个摄像头吗？"
            onConfirm={() => handleDelete(record.id)}
            okText="确认"
            cancelText="取消"
          >
            <Button
              type="link"
              danger
              icon={<DeleteOutlined />}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const handleAdd = () => {
    setEditingCamera(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (camera) => {
    setEditingCamera(camera);
    form.setFieldsValue({
      ...camera,
      edgeNodeId: camera.edgeNodeId || undefined // 确保edgeNodeId为undefined而不是null
    });
    setModalVisible(true);
  };

  const handleDelete = async (id) => {
    try {
      await cameraApi.deleteCamera(id);
      message.success('删除成功');
      fetchCameras();
    } catch (error) {
      console.error('删除摄像头失败:', error);
      message.error('删除摄像头失败: ' + (error.response?.data?.message || error.message || '未知错误'));
    }
  };

  const handleStart = async (id) => {
    try {
      // 调用API启动摄像头
      await streamingApi.startStream(id);
      message.success('摄像头启动命令已发送');
      // 更新本地状态
      const updatedCameras = cameras.map(c => 
        c.id === id ? { ...c, status: 'ONLINE' } : c
      );
      setCameras(updatedCameras);
    } catch (error) {
      console.error('启动摄像头失败:', error);
      message.error('启动摄像头失败: ' + (error.response?.data?.message || error.message || '未知错误'));
    }
  };

  const handleStop = async (id) => {
    try {
      // 调用API停止摄像头
      await streamingApi.stopStream(id);
      message.success('摄像头停止命令已发送');
      // 更新本地状态
      const updatedCameras = cameras.map(c => 
        c.id === id ? { ...c, status: 'OFFLINE' } : c
      );
      setCameras(updatedCameras);
    } catch (error) {
      console.error('停止摄像头失败:', error);
      message.error('停止摄像头失败: ' + (error.response?.data?.message || error.message || '未知错误'));
    }
  };

  // 根据协议类型生成默认地址格式
  const getDefaultUrlByProtocol = (protocol) => {
    switch (protocol) {
      case 'RTSP':
        return 'rtsp://192.168.1.101:554/stream';
      case 'RTMP':
        return 'rtmp://rtsp-server:1935/live/stream1';
      case 'HTTP':
        return 'http://192.168.1.101/video/stream';
      case 'ONVIF':
        return 'http://192.168.1.101/onvif/device_service';
      case 'GB28181':
        return 'sip:34020000001320000001@192.168.1.101:5060';
      default:
        return '';
    }
  };

  // 处理协议类型变化
  const handleProtocolChange = (value) => {
    const currentUrl = form.getFieldValue('connectionUrl');
    // 如果当前URL为空或者是默认值，则更新为新协议的默认URL
    if (!currentUrl || Object.values({
      RTSP: 'rtsp://192.168.1.101:554/stream',
      RTMP: 'rtmp://rtsp-server:1935/live/stream1',
      HTTP: 'http://192.168.1.101/video/stream',
      ONVIF: 'http://192.168.1.101/onvif/device_service',
      GB28181: 'sip:34020000001320000001@192.168.1.101:5060'
    }).includes(currentUrl)) {
      form.setFieldsValue({
        connectionUrl: getDefaultUrlByProtocol(value)
      });
    }
  };

  const handleSubmit = async (values) => {
    try {
      if (editingCamera) {
        // 编辑
        await cameraApi.updateCamera(editingCamera.id, values);
        message.success('摄像头信息更新成功');
      } else {
        // 新增
        await cameraApi.createCamera(values);
        message.success('摄像头添加成功');
      }
      setModalVisible(false);
      fetchCameras();
    } catch (error) {
      console.error('保存摄像头失败:', error);
      message.error('保存摄像头失败: ' + (error.response?.data?.message || error.message || '未知错误'));
    }
  };

  return (
    <div>
      <Card>
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Title level={3} style={{ margin: 0 }}>摄像头管理</Title>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            添加摄像头
          </Button>
        </div>
        
        <Table
          columns={columns}
          dataSource={cameras}
          loading={loading}
          rowKey="id"
          pagination={pagination}
          onChange={handleTableChange}
          locale={{
            emptyText: (
              <Empty 
                image={Empty.PRESENTED_IMAGE_SIMPLE} 
                description="暂无摄像头数据" 
              />
            )
          }}
        />
      </Card>

      <Modal
        title={editingCamera ? '编辑摄像头' : '添加摄像头'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={() => form.submit()}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
        >
          <Form.Item
            name="name"
            label="摄像头名称"
            rules={[{ required: true, message: '请输入摄像头名称' }]}
          >
            <Input placeholder="例如: Camera-A-01" />
          </Form.Item>

          <Form.Item
            name="location"
            label="所属地区"
            rules={[{ required: true, message: '请选择所属地区' }]}
          >
            <Select placeholder="请选择地区">
              {Array.isArray(regions) && regions.map(region => (
                <Option key={region.id} value={region.name}>{region.name}</Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="protocol"
            label="协议类型"
            rules={[{ required: true, message: '请选择协议类型' }]}
          >
            <Select placeholder="请选择协议类型" onChange={handleProtocolChange}>
              <Option value="RTSP">RTSP</Option>
              <Option value="ONVIF">ONVIF</Option>
              <Option value="GB28181">GB28181</Option>
              <Option value="HTTP">HTTP</Option>
              <Option value="RTMP">RTMP</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="connectionUrl"
            label="视频流地址"
            rules={[{ required: true, message: '请输入视频流地址' }]}
          >
            <Input placeholder="根据选择的协议类型自动填充" />
          </Form.Item>

          <Form.Item
            name="edgeNodeId"
            label="边缘节点"
            rules={[{ required: true, message: '请选择边缘节点' }]}
          >
            <Select 
              placeholder="请选择边缘节点"
              showSearch
              optionFilterProp="children"
              filterOption={(input, option) =>
                option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }
            >
              {Array.isArray(edgeNodes) && edgeNodes.map(node => (
                <Option key={node.id} value={node.id}>
                  {node.name} ({node.location})
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="resolution"
            label="分辨率"
            rules={[{ required: true, message: '请选择分辨率' }]}
          >
            <Select placeholder="请选择分辨率">
              <Option value="640x480">640x480</Option>
              <Option value="1280x720">1280x720</Option>
              <Option value="1920x1080">1920x1080</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default CameraManagement;