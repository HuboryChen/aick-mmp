import React, { useState, useEffect } from 'react';
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Popconfirm, Empty, Row, Col } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, PlayCircleOutlined, StopOutlined, SearchOutlined, ReloadOutlined, SyncOutlined, VideoCameraOutlined, AppstoreOutlined, RadarChartOutlined, FolderAddOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { cameraApi, edgeNodeApi, streamingApi, regionApi } from '../utils/api';
import PageContainer from '../components/PageContainer';

const { Option } = Select;

const CameraManagement = () => {
  const navigate = useNavigate();
  const [cameras, setCameras] = useState([]);
  const [edgeNodes, setEdgeNodes] = useState([]);
  const [regions, setRegions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingCamera, setEditingCamera] = useState(null);
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [form] = Form.useForm();
  const [searchKeyword, setSearchKeyword] = useState('');
  const [filterStatus, setFilterStatus] = useState(undefined);
  const [filterRegionId, setFilterRegionId] = useState(undefined);
  const [filterEdgeNode, setFilterEdgeNode] = useState(undefined);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  useEffect(() => {
    fetchCameras();
    fetchEdgeNodes();
    fetchRegions();
  }, [pagination.current, pagination.pageSize, filterStatus, filterRegionId, filterEdgeNode]);

  const fetchCameras = async () => {
    setLoading(true);
    try {
      let params = {
        page: pagination.current - 1,
        size: pagination.pageSize,
      };

      if (searchKeyword) params.keyword = searchKeyword;
      if (filterStatus) params.status = filterStatus;
      if (filterRegionId) params.regionId = filterRegionId;
      if (filterEdgeNode) params.edgeNodeId = filterEdgeNode;

      const response = await cameraApi.getCameras(params);

      const content = response.data?.content || [];
      const totalElements = response.data?.totalElements || 0;
      const pageNumber = response.data?.number || 0;

      const processedCameras = Array.isArray(content) ? content.map(camera => ({
        id: camera.id || 0,
        name: camera.name || '未命名',
        location: camera.location || '',
        regionId: camera.regionId || undefined,
        regionName: camera.regionName || '',
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
      setCameras([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchEdgeNodes = async () => {
    try {
      const response = await edgeNodeApi.getEdgeNodes({ page: 0, size: 100 });
      // 后端返回 Page 对象，需要从 content 字段获取数组
      const data = response.data;
      let content = [];
      if (data && data.content) {
        content = data.content;
      } else if (Array.isArray(data)) {
        content = data;
      }
      setEdgeNodes(content);
    } catch (error) {
      console.error('获取边缘节点列表失败:', error);
      message.error('获取边缘节点列表失败: ' + (error.response?.data?.message || error.message || '未知错误'));
      setEdgeNodes([]);
    }
  };

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

  const handleSearch = () => {
    setPagination({ ...pagination, current: 1 });
    fetchCameras();
  };

  const handleReset = () => {
    setSearchKeyword('');
    setFilterStatus(undefined);
    setFilterRegionId(undefined);
    setFilterEdgeNode(undefined);
    setPagination({ ...pagination, current: 1 });
  };

  const handleTableChange = (pager) => {
    setPagination(pager);
    fetchCameras();
  };

  const rowSelection = {
    selectedRowKeys,
    onChange: setSelectedRowKeys,
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
      dataIndex: 'regionName',
      key: 'regionName',
      render: (text, record) => text || record.regionId || '未指定'
    },
    {
      title: '详细地址',
      dataIndex: 'location',
      key: 'location',
      render: (text) => text || '-'
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
      regionId: camera.regionId || undefined,
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

  const handleBatchDelete = async () => {
    try {
      await cameraApi.batchDeleteCameras({ cameraIds: selectedRowKeys });
      message.success('批量删除成功');
      setSelectedRowKeys([]);
      fetchCameras();
    } catch (error) {
      console.error('批量删除失败:', error);
      message.error('批量删除失败: ' + (error.response?.data?.message || error.message || '未知错误'));
    }
  };

  const handleBatchUpdateEdgeNode = async (edgeNodeId) => {
    try {
      await cameraApi.batchUpdateEdgeNode({ cameraIds: selectedRowKeys }, edgeNodeId);
      message.success('批量分配边缘节点成功');
      setSelectedRowKeys([]);
      fetchCameras();
    } catch (error) {
      console.error('批量分配边缘节点失败:', error);
      message.error('批量分配边缘节点失败: ' + (error.response?.data?.message || error.message || '未知错误'));
    }
  };

  const handleAutoAssign = async () => {
    try {
      await cameraApi.autoAssignCameras();
      message.success('自动分配摄像头成功');
      fetchCameras();
    } catch (error) {
      console.error('自动分配摄像头失败:', error);
      message.error('自动分配摄像头失败: ' + (error.response?.data?.message || error.message || '未知错误'));
    }
  };

  return (
    <PageContainer
      title="摄像头管理"
      icon={<VideoCameraOutlined />}
      actions={
        <Space>
          <Button icon={<AppstoreOutlined />} onClick={() => navigate('/cameras/templates')}>
            配置模板
          </Button>
          <Button icon={<RadarChartOutlined />} onClick={() => navigate('/cameras/discovery')}>
            网络发现
          </Button>
          <Button icon={<FolderAddOutlined />} onClick={() => navigate('/cameras/batch-import')}>
            批量导入
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            添加摄像头
          </Button>
        </Space>
      }
    >
      <div style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input
            placeholder="搜索摄像头名称或位置"
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            onPressEnter={handleSearch}
            style={{ width: 200 }}
            prefix={<SearchOutlined />}
          />
          <Select
            placeholder="状态"
            value={filterStatus}
            onChange={setFilterStatus}
            style={{ width: 120 }}
            allowClear
          >
            <Option value="ONLINE">在线</Option>
            <Option value="OFFLINE">离线</Option>
            <Option value="CONNECTING">连接中</Option>
            <Option value="ERROR">错误</Option>
            <Option value="MAINTENANCE">维护中</Option>
          </Select>
          <Select
            placeholder="地区"
            value={filterRegionId}
            onChange={setFilterRegionId}
            style={{ width: 150 }}
            allowClear
          >
            {Array.isArray(regions) && regions.map(region => (
              <Option key={region.id} value={region.id}>{region.name}</Option>
            ))}
          </Select>
          <Select
            placeholder="边缘节点"
            value={filterEdgeNode}
            onChange={setFilterEdgeNode}
            style={{ width: 200 }}
            allowClear
            dropdownRender={(menu) => (
              <>
                <div style={{ padding: '8px 12px', borderBottom: '1px solid var(--color-border)', fontSize: '12px', color: 'var(--color-text-secondary)' }}>
                  <strong>节点信息:</strong> 名称 | 状态 | 摄像头数
                </div>
                {menu}
              </>
            )}
          >
            <Option key="none" value={undefined} style={{ fontStyle: 'italic' }}>
              全部节点
            </Option>
            {Array.isArray(edgeNodes) && edgeNodes.map(node => {
              const isOnline = node.status === 'ONLINE';
              const statusColor = isOnline ? 'var(--status-online)' : 'var(--status-error)';
              
              return (
                <Option key={node.id} value={node.id}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                      <strong>{node.name}</strong>
                      <span style={{ 
                        display: 'inline-block', 
                        width: '6px', 
                        height: '6px', 
                        borderRadius: '50%', 
                        backgroundColor: statusColor,
                        marginLeft: '8px',
                        marginRight: '4px'
                      }}></span>
                      <span style={{ fontSize: '12px', color: 'var(--color-text-secondary)' }}>
                        {isOnline ? '在线' : '离线'}
                      </span>
                    </div>
                    <div style={{ fontSize: '12px', color: 'var(--color-text-secondary)' }}>
                      {node.currentCameraCount || 0}/{node.maxCameraSupport || 100}
                    </div>
                  </div>
                </Option>
              );
            })}
          </Select>
          <Button type="primary" onClick={handleSearch}>
            搜索
          </Button>
          <Button onClick={handleReset}>
            重置
          </Button>
          <Button icon={<ReloadOutlined />} onClick={fetchCameras}>
            刷新
          </Button>
        </Space>
      </div>

      {selectedRowKeys.length > 0 && (
        <div style={{ marginBottom: 16, padding: '12px', background: 'var(--color-bg-secondary)', borderRadius: '4px', border: '1px solid var(--color-border)' }}>
          <Space>
            <span>已选择 {selectedRowKeys.length} 项</span>
            <Popconfirm
              title="确定批量删除选中的摄像头吗？"
              onConfirm={handleBatchDelete}
              okText="确定"
              cancelText="取消"
            >
              <Button danger icon={<DeleteOutlined />}>
                批量删除
              </Button>
            </Popconfirm>
            <Select
              placeholder="批量分配边缘节点"
              style={{ width: 260 }}
              onChange={handleBatchUpdateEdgeNode}
              allowClear
              dropdownRender={(menu) => (
                <>
                  <div style={{ padding: '8px 12px', borderBottom: '1px solid var(--color-border)', fontSize: '12px', color: 'var(--color-text-secondary)' }}>
                    <strong>节点信息:</strong> 名称 | 状态 | 摄像头数 | 负载
                  </div>
                  {menu}
                </>
              )}
            >
              {Array.isArray(edgeNodes) && edgeNodes.map(node => {
                const isOnline = node.status === 'ONLINE';
                const isHealthy = node.cpuUsage < 80 && node.memoryUsage < 85;
                const statusColor = isOnline ? (isHealthy ? 'var(--status-online)' : 'var(--status-warning)') : 'var(--status-error)';
                const loadColor = node.cpuUsage > 80 ? 'var(--status-error)' : (node.cpuUsage > 60 ? 'var(--status-warning)' : 'var(--status-online)');
                
                return (
                  <Option key={node.id} value={node.id}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div>
                        <strong>{node.name}</strong>
                        <span style={{ 
                          display: 'inline-block', 
                          width: '6px', 
                          height: '6px', 
                          borderRadius: '50%', 
                          backgroundColor: statusColor,
                          marginLeft: '8px',
                          marginRight: '4px'
                        }}></span>
                        <span style={{ fontSize: '12px', color: 'var(--color-text-secondary)' }}>
                          {isOnline ? '在线' : '离线'}
                        </span>
                      </div>
                      <div style={{ fontSize: '12px', color: 'var(--color-text-secondary)' }}>
                        <span style={{ marginRight: '8px' }}>
                          {node.currentCameraCount || 0}/{node.maxCameraSupport || 100}
                        </span>
                        <span style={{ color: loadColor }}>
                          CPU: {node.cpuUsage || 0}%
                        </span>
                      </div>
                    </div>
                  </Option>
                );
              })}
            </Select>
            <Popconfirm
              title="自动分配所有未分配的摄像头到最优边缘节点？"
              onConfirm={handleAutoAssign}
              okText="确定"
              cancelText="取消"
            >
              <Button type="primary" icon={<SyncOutlined />}>
                自动分配
              </Button>
            </Popconfirm>
          </Space>
        </div>
      )}

      <Table
        columns={columns}
        dataSource={cameras}
        loading={loading}
        rowKey="id"
        rowSelection={rowSelection}
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
            name="regionId"
            label="所属地区"
          >
            <Select placeholder="请选择地区（可选）" allowClear>
              {Array.isArray(regions) && regions.map(region => (
                <Option key={region.id} value={region.id}>{region.name}</Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="location"
            label="详细地址"
          >
            <Input placeholder="例如: XX街道XX号（可选）" />
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
    </PageContainer>
  );
};

export default CameraManagement;