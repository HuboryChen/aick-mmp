import React, { useState, useEffect } from 'react';
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Card, Typography, Popconfirm, Tabs, Descriptions, Spin } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, PlayCircleOutlined, StopOutlined, SyncOutlined } from '@ant-design/icons';
import { edgeNodeApi, regionApi } from '../utils/api';

const { Title } = Typography;
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
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  useEffect(() => {
    fetchEdgeNodes();
    fetchRegions();
  }, []);

  const fetchEdgeNodes = async (params = {}) => {
    setLoading(true);
    try {
      const response = await edgeNodeApi.getEdgeNodes({
        page: pagination.current - 1,
        size: pagination.pageSize,
        ...params
      });
      
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

  const handleTableChange = (pager) => {
    setPagination(pager);
    fetchEdgeNodes({ page: pager.current - 1, size: pager.pageSize });
  };

  const columns = [
    {
      title: '节点名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '所属地区',
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
    form.setFieldsValue(node);
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
    <div>
      <Card>
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Title level={3} style={{ margin: 0 }}>边缘节点管理</Title>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            添加节点
          </Button>
        </div>
        
        <Table
          columns={columns}
          dataSource={Array.isArray(edgeNodes) ? edgeNodes : []}
          loading={loading}
          rowKey="id"
          pagination={pagination}
          onChange={handleTableChange}
        />
      </Card>

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
                <Descriptions.Item label="所属地区">{nodeDetail.location}</Descriptions.Item>
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
    </div>
  );
};

export default EdgeNodeManagement;
