import React, { useState, useEffect } from 'react';
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Popconfirm, Tabs, Descriptions, Spin, Row, Col, Progress, Card } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, PlayCircleOutlined, SyncOutlined, SearchOutlined, UnlockOutlined, LockOutlined, ReloadOutlined, DashboardOutlined, CloudServerOutlined } from '@ant-design/icons';
import { edgeNodeApi, regionApi } from '../utils/api';
import PageContainer from '../components/PageContainer';

const { Option } = Select;
const { TabPane } = Tabs;

const EdgeNodeManagement = () => {
  const [edgeNodes, setEdgeNodes] = useState([]);
  const [regions, setRegions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [editingNode, setEditingNode] = useState(null);
  const [nodeDetail, setNodeDetail] = useState(null);
  const [form] = Form.useForm();
  const [searchKeyword, setSearchKeyword] = useState('');
  const [filterStatus, setFilterStatus] = useState(undefined);
  const [filterRegionId, setFilterRegionId] = useState(undefined);
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [healthModalVisible, setHealthModalVisible] = useState(false);
  const [nodeHealth, setNodeHealth] = useState(null);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  useEffect(() => {
    fetchEdgeNodes();
    fetchRegions();
  }, [pagination.current, pagination.pageSize]);

  const fetchEdgeNodes = async () => {
    setLoading(true);
    try {
      let params = {
        page: pagination.current - 1,
        size: pagination.pageSize,
      };
      
      if (searchKeyword) {
        params.keyword = searchKeyword;
      }
      
      if (filterStatus) {
        params.status = filterStatus;
      }
      
      if (filterRegionId) {
        params.regionId = filterRegionId;
      }

      const response = await edgeNodeApi.getEdgeNodes(params);
      
      // 确保数据结构正确
      const content = response.data?.content || [];
      const totalElements = response.data?.totalElements || 0;
      const pageNumber = response.data?.number || 0;
      
      setEdgeNodes(Array.isArray(content) ? content : []);
      setPagination({
        ...pagination,
        total: totalElements,
        current: pageNumber + 1,
      });
    } catch (error) {
      console.error('获取边缘节点列表失败:', error);
      message.error('获取边缘节点列表失败: ' + (error.response?.data?.message || error.message));
      setEdgeNodes([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchRegions = async () => {
    try {
      const response = await regionApi.getAllRegions();
      setRegions(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error('获取地区列表失败:', error);
      message.error('获取地区列表失败: ' + (error.response?.data?.message || error.message));
      setRegions([]);
    }
  };

  const fetchNodeDetail = async (id) => {
    setDetailLoading(true);
    try {
      const response = await edgeNodeApi.getEdgeNode(id);
      setNodeDetail(response.data);
    } catch (error) {
      console.error('获取节点详情失败:', error);
      message.error('获取节点详情失败: ' + (error.response?.data?.message || error.message));
    } finally {
      setDetailLoading(false);
    }
  };

  const handleSearch = () => {
    setPagination({ ...pagination, current: 1 });
    fetchEdgeNodes();
  };

  const handleReset = () => {
    setSearchKeyword('');
    setFilterStatus(undefined);
    setFilterRegionId(undefined);
    setPagination({ ...pagination, current: 1 });
  };

  const handleTableChange = (pager) => {
    setPagination(pager);
    fetchEdgeNodes();
  };

  const rowSelection = {
    selectedRowKeys,
    onChange: setSelectedRowKeys,
  };

  const handleBatchDelete = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择要删除的节点');
      return;
    }

    try {
      await edgeNodeApi.batchDeleteEdgeNodes(selectedRowKeys);
      message.success('批量删除成功');
      setSelectedRowKeys([]);
      fetchEdgeNodes();
    } catch (error) {
      console.error('批量删除失败:', error);
      message.error('批量删除失败: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleBatchEnable = async (enabled) => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择要操作的节点');
      return;
    }

    try {
      await edgeNodeApi.batchEnableEdgeNodes(selectedRowKeys, enabled);
      message.success(`批量${enabled ? '启用' : '禁用'}成功`);
      setSelectedRowKeys([]);
      fetchEdgeNodes();
    } catch (error) {
      console.error('批量操作失败:', error);
      message.error('批量操作失败: ' + (error.response?.data?.message || error.message));
    }
  };

  const fetchNodeHealth = async (id) => {
    try {
      const response = await edgeNodeApi.getEdgeNodeHealthDetails(id);
      setNodeHealth(response.data);
      setHealthModalVisible(true);
    } catch (error) {
      console.error('获取健康信息失败:', error);
      message.error('获取健康信息失败: ' + (error.response?.data?.message || error.message));
    }
  };

  const columns = [
    {
      title: '节点名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '所属区域',
      dataIndex: 'regionName',
      key: 'regionName',
      render: (text, record) => text || record.regionId || '-',
    },
    {
      title: '详细地址',
      dataIndex: 'location',
      key: 'location',
    },
    {
      title: 'IP地址',
      dataIndex: 'ipAddress',
      key: 'ipAddress',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => (
        <Tag color={status === 'ONLINE' ? 'green' : status === 'OFFLINE' ? 'red' : 'orange'}>
          {status === 'ONLINE' ? '在线' : status === 'OFFLINE' ? '离线' : status}
        </Tag>
      ),
    },
    {
      title: '是否启用',
      dataIndex: 'enabled',
      key: 'enabled',
      render: (enabled) => (
        <Tag color={enabled ? 'green' : 'red'}>
          {enabled ? '是' : '否'}
        </Tag>
      ),
    },
    {
      title: '注册时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (createdAt) => new Date(createdAt).toLocaleString(),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="middle">
          <Button type="link" onClick={() => handleViewDetail(record.id)}>查看</Button>
          <Button type="link" onClick={() => fetchNodeHealth(record.id)} icon={<DashboardOutlined />}>健康检查</Button>
          <Button type="link" onClick={() => handleEnable(record.id, !record.enabled)}>
            {record.enabled ? '禁用' : '启用'}
          </Button>
          <Button type="link" onClick={() => handleEdit(record)}>编辑</Button>
          <Popconfirm
            title="确认删除"
            description="确定要删除这个边缘节点吗？"
            onConfirm={() => handleDelete(record.id)}
            okText="确认"
            cancelText="取消"
          >
            <Button type="link" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const handleAdd = () => {
    setEditingNode(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (node) => {
    setEditingNode(node);
    form.setFieldsValue({
      ...node,
      regionId: node.regionId,
    });
    setModalVisible(true);
  };

  const handleViewDetail = async (id) => {
    await fetchNodeDetail(id);
    setDetailModalVisible(true);
  };

  const handleDelete = async (id) => {
    try {
      await edgeNodeApi.deleteEdgeNode(id);
      message.success('删除成功');
      fetchEdgeNodes();
    } catch (error) {
      console.error('删除失败:', error);
      message.error('删除失败: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleEnable = async (id, enabled) => {
    try {
      await edgeNodeApi.enableEdgeNode(id, enabled);
      message.success(enabled ? '启用成功' : '禁用成功');
      fetchEdgeNodes();
    } catch (error) {
      console.error('操作失败:', error);
      message.error('操作失败: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleTestConnection = async (id) => {
    try {
      await edgeNodeApi.testConnection(id);
      message.success('连接测试成功');
      fetchEdgeNodes();
    } catch (error) {
      console.error('连接测试失败:', error);
      message.error('连接测试失败: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleRestart = async (id) => {
    try {
      await edgeNodeApi.restartNode(id);
      message.success('重启命令已发送');
      fetchEdgeNodes();
    } catch (error) {
      console.error('重启命令发送失败:', error);
      message.error('重启命令发送失败: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleSubmit = async (values) => {
    try {
      if (editingNode) {
        await edgeNodeApi.updateEdgeNode(editingNode.id, values);
        message.success('更新成功');
      } else {
        await edgeNodeApi.createEdgeNode(values);
        message.success('添加成功');
      }
      setModalVisible(false);
      fetchEdgeNodes();
    } catch (error) {
      console.error('操作失败:', error);
      message.error('操作失败: ' + (error.response?.data?.message || error.message));
    }
  };

  return (
    <PageContainer
      title="边缘节点管理"
      icon={<CloudServerOutlined />}
      actions={
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          添加节点
        </Button>
      }
    >
      {/* 搜索区域 */}
      <div className="mb-4">
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
            placeholder="区域"
            value={filterRegionId}
            onChange={setFilterRegionId}
            style={{ width: 150 }}
            allowClear
          >
            {Array.isArray(regions) && regions.map(region => (
              <Option key={region.id} value={region.id}>{region.name}</Option>
            ))}
          </Select>
          <Button type="primary" onClick={handleSearch}>
            搜索
          </Button>
          <Button onClick={handleReset}>
            重置
          </Button>
          <Button icon={<ReloadOutlined />} onClick={fetchEdgeNodes}>
            刷新
          </Button>
        </Space>
      </div>

      {/* 批量操作区域 */}
      {selectedRowKeys.length > 0 && (
        <div className="mb-4 p-3" style={{ background: 'var(--color-bg-secondary)', borderRadius: '4px', border: '1px solid var(--color-border)' }}>
          <Space>
            <span>已选择 {selectedRowKeys.length} 项</span>
            <Popconfirm
              title="确定批量删除选中的节点吗？"
              onConfirm={handleBatchDelete}
              okText="确定"
              cancelText="取消"
            >
              <Button danger icon={<DeleteOutlined />}>
                批量删除
              </Button>
            </Popconfirm>
            <Button icon={<UnlockOutlined />} onClick={() => handleBatchEnable(true)}>
              批量启用
            </Button>
            <Button icon={<LockOutlined />} onClick={() => handleBatchEnable(false)}>
              批量禁用
            </Button>
          </Space>
        </div>
      )}
      
      <Table
        columns={columns}
        dataSource={Array.isArray(edgeNodes) ? edgeNodes : []}
        loading={loading}
        rowKey="id"
        pagination={pagination}
        rowSelection={rowSelection}
        onChange={handleTableChange}
      />
      <Modal
        title={editingNode ? '编辑边缘节点' : '添加边缘节点'}
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
            label="节点名称"
            rules={[{ required: true, message: '请输入节点名称' }]}
          >
            <Input placeholder="例如: Edge-Node-01" />
          </Form.Item>

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

          <Form.Item
            name="location"
            label="详细地址"
          >
            <Input placeholder="例如: XX街道XX号（可选）" />
          </Form.Item>

          <Form.Item
            name="ipAddress"
            label="IP地址"
            rules={[{ required: true, message: '请输入IP地址' }]}
          >
            <Input placeholder="例如: 192.168.1.100" />
          </Form.Item>

          <Form.Item
            name="port"
            label="端口"
            rules={[{ required: true, message: '请输入端口' }]}
          >
            <Input placeholder="例如: 8080" type="number" />
          </Form.Item>

          <Form.Item
            name="description"
            label="描述"
          >
            <Input.TextArea placeholder="节点描述信息" rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="节点详情"
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={null}
        width={800}
      >
        {detailLoading ? (
          <div style={{ textAlign: 'center', padding: '50px' }}>
            <Spin size="large" />
          </div>
        ) : nodeDetail ? (
          <Tabs defaultActiveKey="1">
            <TabPane tab="基本信息" key="1">
              <Descriptions column={1} bordered>
                <Descriptions.Item label="节点ID">{nodeDetail.id}</Descriptions.Item>
                <Descriptions.Item label="节点名称">{nodeDetail.name}</Descriptions.Item>
                <Descriptions.Item label="所属区域">{nodeDetail.regionName || '-'}</Descriptions.Item>
                <Descriptions.Item label="详细地址">{nodeDetail.location || '-'}</Descriptions.Item>
                <Descriptions.Item label="IP地址">{nodeDetail.ipAddress}</Descriptions.Item>
                <Descriptions.Item label="端口">{nodeDetail.port}</Descriptions.Item>
                <Descriptions.Item label="状态">
                  <Tag color={nodeDetail.status === 'ONLINE' ? 'green' : 'red'}>
                    {nodeDetail.status === 'ONLINE' ? '在线' : '离线'}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="注册时间">
                  {new Date(nodeDetail.createdAt).toLocaleString()}
                </Descriptions.Item>
                <Descriptions.Item label="最后更新时间">
                  {new Date(nodeDetail.updatedAt).toLocaleString()}
                </Descriptions.Item>
                <Descriptions.Item label="描述">{nodeDetail.description || '无'}</Descriptions.Item>
              </Descriptions>
            </TabPane>
            <TabPane tab="操作" key="2">
              <Space>
                <Button 
                  icon={<SyncOutlined />} 
                  onClick={() => handleTestConnection(nodeDetail.id)}
                >
                  测试连接
                </Button>
                <Button 
                  icon={<PlayCircleOutlined />} 
                  onClick={() => handleRestart(nodeDetail.id)}
                >
                  重启节点
                </Button>
              </Space>
            </TabPane>
          </Tabs>
        ) : null}
      </Modal>

      <Modal
        title="节点健康检查"
        open={healthModalVisible}
        onCancel={() => setHealthModalVisible(false)}
        footer={null}
        width={800}
      >
        {nodeHealth ? (
          <Tabs defaultActiveKey="1">
            <TabPane tab="健康概览" key="1">
              <div style={{ padding: '20px 0' }}>
                <Row gutter={[16, 16]}>
                  <Col span={8}>
                    <Card size="small" title="健康状态">
                      <div style={{ textAlign: 'center', padding: '20px 0' }}>
                        <div style={{ fontSize: '48px', fontWeight: 'bold', color: getHealthColor(nodeHealth.healthScore) }}>
                          {nodeHealth.healthScore || 0}
                        </div>
                        <div style={{ fontSize: '16px', marginTop: '10px' }}>
                          {getHealthStatusText(nodeHealth.healthStatus)}
                        </div>
                      </div>
                    </Card>
                  </Col>
                  <Col span={8}>
                    <Card size="small" title="系统状态">
                      <div style={{ padding: '10px 0' }}>
                        <div>节点状态: <Tag color={nodeHealth.status === 'ONLINE' ? 'green' : 'red'}>{nodeHealth.status}</Tag></div>
                        <div>是否启用: {nodeHealth.enabled ? <Tag color="green">是</Tag> : <Tag color="red">否</Tag>}</div>
                        <div>最后心跳: {nodeHealth.lastHeartbeatTime ? new Date(nodeHealth.lastHeartbeatTime).toLocaleString() : '无'}</div>
                      </div>
                    </Card>
                  </Col>
                  <Col span={8}>
                    <Card size="small" title="摄像头负载">
                      <div style={{ padding: '10px 0' }}>
                        <div>当前摄像头: {nodeHealth.currentCameraCount || 0}</div>
                        <div>最大支持: {nodeHealth.maxCameraSupport || 0}</div>
                        <div>
                          负载率: 
                          {nodeHealth.cameraLoadPercentage ? (
                            <Progress 
                              percent={nodeHealth.cameraLoadPercentage} 
                              size="small" 
                              status={nodeHealth.cameraLoadPercentage > 80 ? "exception" : nodeHealth.cameraLoadPercentage > 60 ? "warning" : "normal"}
                              style={{ marginTop: '5px' }}
                            />
                          ) : 'N/A'}
                        </div>
                      </div>
                    </Card>
                  </Col>
                </Row>
              </div>
            </TabPane>
            <TabPane tab="系统指标" key="2">
              <Descriptions column={1} bordered>
                <Descriptions.Item label="CPU使用率">
                  {nodeHealth.cpuUsage !== undefined ? `${nodeHealth.cpuUsage}%` : 'N/A'}
                  {nodeHealth.cpuUsage !== undefined && (
                    <Progress 
                      percent={nodeHealth.cpuUsage} 
                      size="small" 
                      status={nodeHealth.cpuUsage > 90 ? "exception" : nodeHealth.cpuUsage > 70 ? "warning" : "normal"}
                      style={{ marginTop: '5px' }}
                    />
                  )}
                </Descriptions.Item>
                <Descriptions.Item label="内存使用率">
                  {nodeHealth.memoryUsage !== undefined ? `${nodeHealth.memoryUsage}%` : 'N/A'}
                  {nodeHealth.memoryUsage !== undefined && (
                    <Progress 
                      percent={nodeHealth.memoryUsage} 
                      size="small" 
                      status={nodeHealth.memoryUsage > 90 ? "exception" : nodeHealth.memoryUsage > 70 ? "warning" : "normal"}
                      style={{ marginTop: '5px' }}
                    />
                  )}
                </Descriptions.Item>
                <Descriptions.Item label="存储使用率">
                  {nodeHealth.storageUsage !== undefined ? `${nodeHealth.storageUsage}%` : 'N/A'}
                  {nodeHealth.storageUsage !== undefined && (
                    <Progress 
                      percent={nodeHealth.storageUsage} 
                      size="small" 
                      status={nodeHealth.storageUsage > 90 ? "exception" : nodeHealth.storageUsage > 70 ? "warning" : "normal"}
                      style={{ marginTop: '5px' }}
                    />
                  )}
                </Descriptions.Item>
                <Descriptions.Item label="最后心跳时间">
                  {nodeHealth.lastHeartbeatTime ? new Date(nodeHealth.lastHeartbeatTime).toLocaleString() : '无'}
                </Descriptions.Item>
                <Descriptions.Item label="心跳间隔">
                  {nodeHealth.secondsSinceLastHeartbeat !== undefined ? `${nodeHealth.secondsSinceLastHeartbeat} 秒` : 'N/A'}
                </Descriptions.Item>
                <Descriptions.Item label="硬件信息">
                  {nodeHealth.hardwareInfo || 'N/A'}
                </Descriptions.Item>
                <Descriptions.Item label="软件版本">
                  {nodeHealth.softwareVersion || 'N/A'}
                </Descriptions.Item>
                <Descriptions.Item label="网络带宽">
                  {nodeHealth.networkBandwidth || 'N/A'}
                </Descriptions.Item>
              </Descriptions>
            </TabPane>
            <TabPane tab="系统指标详情" key="3">
              {nodeHealth.systemMetrics && Object.keys(nodeHealth.systemMetrics).length > 0 ? (
                <Descriptions column={2} bordered>
                  {Object.entries(nodeHealth.systemMetrics).map(([key, value]) => (
                    <Descriptions.Item key={key} label={key}>
                      {typeof value === 'object' ? JSON.stringify(value) : String(value)}
                    </Descriptions.Item>
                  ))}
                </Descriptions>
              ) : (
                <div style={{ textAlign: 'center', padding: '40px' }}>无系统指标数据</div>
              )}
            </TabPane>
          </Tabs>
        ) : (
          <div style={{ textAlign: 'center', padding: '50px' }}>
            <Spin size="large" />
          </div>
        )}
      </Modal>
    </PageContainer>
  );
};

// 辅助函数
const getHealthColor = (score) => {
  if (score >= 90) return '#52c41a'; // 优秀
  if (score >= 70) return '#1890ff'; // 良好
  if (score >= 50) return '#faad14'; // 一般
  if (score >= 30) return '#ff4d4f'; // 差
  return '#ff4d4f'; // 严重
};

const getHealthStatusText = (status) => {
  const statusMap = {
    HEALTHY: '健康',
    ELEVATED_CPU: 'CPU偏高',
    ELEVATED_MEMORY: '内存偏高',
    ELEVATED_STORAGE: '存储偏高',
    HIGH_LOAD: '高负载',
    HIGH_CPU: 'CPU过高',
    HIGH_MEMORY: '内存过高',
    HIGH_STORAGE: '存储过高',
    HEARTBEAT_TIMEOUT: '心跳超时',
    OVERLOADED: '过载',
    OFFLINE: '离线',
    NO_HEARTBEAT: '无心跳',
    DISABLED: '已禁用',
  };
  return statusMap[status] || status;
};

export default EdgeNodeManagement;
