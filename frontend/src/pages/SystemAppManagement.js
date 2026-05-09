import React, { useState, useEffect } from 'react';
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Popconfirm, Empty, Checkbox, Row, Col, Alert, Descriptions } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, AppstoreOutlined, SearchOutlined, ReloadOutlined, CopyOutlined, KeyOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { systemAppApi } from '../utils/api';
import PageContainer from '../components/PageContainer';

const { Option } = Select;
const { TextArea } = Input;

const SystemAppManagement = () => {
  const [systemApps, setSystemApps] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [credentialsModalVisible, setCredentialsModalVisible] = useState(false);
  const [editingApp, setEditingApp] = useState(null);
  const [createdCredentials, setCreatedCredentials] = useState(null);
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [form] = Form.useForm();
  const [searchKeyword, setSearchKeyword] = useState('');
  const [filterStatus, setFilterStatus] = useState(undefined);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  // 可用权限列表
  const availablePermissions = [
    { value: 'EDGE_REGISTER', label: '边缘节点注册' },
    { value: 'EDGE_HEARTBEAT', label: '心跳上报' },
    { value: 'EDGE_CONFIG_UPDATE', label: '配置更新' },
  ];

  useEffect(() => {
    fetchSystemApps();
  }, [pagination.current, pagination.pageSize, filterStatus]);

  const fetchSystemApps = async () => {
    setLoading(true);
    try {
      let params = {
        page: pagination.current - 1,
        size: pagination.pageSize,
      };

      if (searchKeyword) params.keyword = searchKeyword;
      if (filterStatus) params.status = filterStatus;

      const response = await systemAppApi.list(params);
      
      const content = response.data?.content || [];
      const totalElements = response.data?.totalElements || 0;
      const pageNumber = response.data?.number || 0;

      const processedApps = Array.isArray(content) ? content.map(app => ({
        id: app.id,
        appKey: app.appKey,
        name: app.name || '未命名',
        description: app.description || '',
        ownerType: app.ownerType || 'SYSTEM',
        ownerId: app.ownerId,
        status: app.status || 'ACTIVE',
        permissions: app.permissions || [],
        createdBy: app.createdBy,
        createdAt: app.createdAt,
        updatedAt: app.updatedAt,
        hasCredentials: app.hasCredentials || false,
      })) : [];

      setSystemApps(processedApps);
      setPagination({
        ...pagination,
        total: totalElements,
        current: pageNumber + 1,
      });
    } catch (error) {
      console.error('获取系统应用列表失败:', error);
      message.error('获取系统应用列表失败: ' + (error.response?.data?.message || error.message || '未知错误'));
      setSystemApps([]);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    setPagination({ ...pagination, current: 1 });
    fetchSystemApps();
  };

  const handleReset = () => {
    setSearchKeyword('');
    setFilterStatus(undefined);
    setPagination({ ...pagination, current: 1 });
  };

  const handleTableChange = (pager) => {
    setPagination(pager);
    fetchSystemApps();
  };

  const rowSelection = {
    selectedRowKeys,
    onChange: setSelectedRowKeys,
  };

  const handleAdd = () => {
    setEditingApp(null);
    setCreatedCredentials(null);
    form.resetFields();
    // 设置默认值
    form.setFieldsValue({
      status: 'ACTIVE',
      ownerType: 'SYSTEM',
      permissions: [],
    });
    setModalVisible(true);
  };

  const handleEdit = (app) => {
    setEditingApp(app);
    setCreatedCredentials(null);
    form.setFieldsValue({
      name: app.name,
      description: app.description,
      status: app.status,
      ownerType: app.ownerType,
      ownerId: app.ownerId,
      permissions: app.permissions || [],
    });
    setModalVisible(true);
  };

  const handleDelete = async (id) => {
    try {
      await systemAppApi.delete(id);
      message.success('删除成功');
      fetchSystemApps();
    } catch (error) {
      console.error('删除系统应用失败:', error);
      message.error('删除系统应用失败: ' + (error.response?.data?.message || error.message || '未知错误'));
    }
  };

  const handleSubmit = async (values) => {
    try {
      if (editingApp) {
        // 编辑
        await systemAppApi.update(editingApp.id, values);
        message.success('系统应用更新成功');
        setModalVisible(false);
      } else {
        // 新增 - 返回凭证信息
        const response = await systemAppApi.create(values);
        message.success('系统应用创建成功');
        setModalVisible(false);
        
        // 显示凭证弹窗
        if (response.data) {
          setCreatedCredentials({
            id: response.data.id,
            name: response.data.name || values.name,
            appKey: response.data.appKey,
            appSecret: response.data.appSecret,
            createdAt: response.data.createdAt,
            warning: response.data.warning,
          });
          setCredentialsModalVisible(true);
        }
      }
      fetchSystemApps();
    } catch (error) {
      console.error('保存系统应用失败:', error);
      message.error('保存系统应用失败: ' + (error.response?.data?.message || error.message || '未知错误'));
    }
  };

  const handleBatchDelete = async () => {
    try {
      const response = await systemAppApi.batchDelete(selectedRowKeys);
      message.success(response.data?.message || '批量删除成功');
      setSelectedRowKeys([]);
      fetchSystemApps();
    } catch (error) {
      console.error('批量删除失败:', error);
      const errorMsg = error.response?.data?.message || error.message || '未知错误';
      message.error('批量删除失败: ' + errorMsg);
    }
  };

  const handleStatusChange = async (id, status) => {
    try {
      await systemAppApi.updateStatus(id, status);
      message.success('状态更新成功');
      fetchSystemApps();
    } catch (error) {
      console.error('状态更新失败:', error);
      message.error('状态更新失败: ' + (error.response?.data?.message || error.message || '未知错误'));
    }
  };

  // 获取凭证
  const handleGetCredentials = async (app) => {
    try {
      const response = await systemAppApi.getCredentials(app.id);
      setCreatedCredentials({
        id: app.id,
        name: app.name,
        appKey: response.data.appKey,
        appSecret: response.data.appSecret,
      });
      setCredentialsModalVisible(true);
    } catch (error) {
      console.error('获取凭证失败:', error);
      message.error('获取凭证失败: ' + (error.response?.data?.message || error.message || '未知错误'));
    }
  };

  // 重新生成凭证
  const handleRegenerateCredentials = async (app) => {
    Modal.confirm({
      title: '重新生成凭证',
      icon: <ExclamationCircleOutlined />,
      content: (
        <div>
          <p>确定要重新生成凭证吗？</p>
          <p style={{ color: '#ff4d4f' }}>警告：旧凭证将立即失效，请确保已备份！</p>
        </div>
      ),
      okText: '确认重新生成',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          const response = await systemAppApi.regenerateCredentials(app.id);
          setCreatedCredentials({
            id: app.id,
            name: app.name,
            appKey: response.data.appKey,
            appSecret: response.data.appSecret,
            warning: response.data.warning,
          });
          setCredentialsModalVisible(true);
          message.success('凭证重新生成成功');
          fetchSystemApps();
        } catch (error) {
          console.error('重新生成凭证失败:', error);
          message.error('重新生成凭证失败: ' + (error.response?.data?.message || error.message || '未知错误'));
        }
      },
    });
  };

  const handleCopyAppKey = (text, type = 'App Key') => {
    navigator.clipboard.writeText(text).then(() => {
      message.success(`${type} 已复制到剪贴板`);
    }).catch(() => {
      message.error('复制失败');
    });
  };

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 70,
      fixed: 'left',
      render: (id) => (
        <span style={{ 
          fontFamily: 'var(--font-mono)',
          fontSize: '12px',
          color: 'var(--color-text-secondary)'
        }}>
          #{id}
        </span>
      ),
    },
    {
      title: '应用信息',
      key: 'appInfo',
      width: 280,
      render: (_, record) => (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <AppstoreOutlined style={{ color: 'var(--color-accent)' }} />
            <strong style={{ color: 'var(--color-text-primary)', fontSize: '14px' }}>
              {record.name}
            </strong>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginLeft: '20px' }}>
            {record.appKey ? (
              <>
                <code style={{ 
                  fontSize: '11px', 
                  color: 'var(--color-text-secondary)',
                  fontFamily: 'var(--font-mono)',
                  background: 'var(--color-bg-secondary)',
                  padding: '2px 8px',
                  borderRadius: '4px',
                  maxWidth: '180px',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                }}>
                  {record.appKey.substring(0, 24)}...
                </code>
                <Button 
                  type="text" 
                  size="small" 
                  icon={<CopyOutlined />}
                  onClick={() => handleCopyAppKey(record.appKey)}
                  style={{ color: 'var(--color-text-secondary)' }}
                />
              </>
            ) : (
              <Tag color="warning">未生成凭证</Tag>
            )}
          </div>
        </div>
      ),
    },
    {
      title: '凭证状态',
      key: 'credentialStatus',
      width: 100,
      render: (_, record) => (
        record.hasCredentials ? (
          <Tag color="success" icon={<KeyOutlined />}>已配置</Tag>
        ) : (
          <Tag color="warning">未配置</Tag>
        )
      ),
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      render: (text) => (
        <span style={{ color: text ? 'var(--color-text-secondary)' : 'var(--color-text-muted)' }}>
          {text || '暂无描述'}
        </span>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      filters: [
        { text: '活跃', value: 'ACTIVE' },
        { text: '未激活', value: 'INACTIVE' },
        { text: '已暂停', value: 'SUSPENDED' },
      ],
      onFilter: (value, record) => record.status === value,
      render: (status) => {
        const statusMap = {
          ACTIVE: { text: '活跃', color: 'success' },
          INACTIVE: { text: '未激活', color: 'default' },
          SUSPENDED: { text: '已暂停', color: 'warning' },
        };
        const { text, color } = statusMap[status] || { text: status, color: 'default' };
        return <Tag color={color}>{text}</Tag>;
      },
    },
    {
      title: '权限',
      dataIndex: 'permissions',
      key: 'permissions',
      width: 180,
      render: (permissions) => {
        if (!permissions || permissions.length === 0) {
          return <Tag color="default">无权限</Tag>;
        }
        return (
          <Space direction="vertical" size={2}>
            {permissions.slice(0, 2).map(perm => {
              const permLabel = availablePermissions.find(p => p.value === perm)?.label || perm;
              return (
                <Tag 
                  key={perm} 
                  style={{ 
                    fontSize: '11px',
                    marginRight: '4px',
                    background: 'var(--color-bg-secondary)',
                    borderColor: 'var(--color-border)',
                  }}
                >
                  {permLabel}
                </Tag>
              );
            })}
            {permissions.length > 2 && (
              <Tag style={{ fontSize: '10px', color: 'var(--color-text-muted)' }}>
                +{permissions.length - 2} 更多
              </Tag>
            )}
          </Space>
        );
      },
    },
    {
      title: '所有者',
      dataIndex: 'ownerType',
      key: 'ownerType',
      width: 90,
      render: (type) => (
        <Tag color={type === 'SYSTEM' ? 'processing' : 'purple'}>
          {type === 'SYSTEM' ? '系统' : '用户'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      sorter: (a, b) => new Date(a.createdAt) - new Date(b.createdAt),
      render: (time) => (
        <span style={{ 
          fontSize: '12px', 
          color: 'var(--color-text-secondary)',
          fontFamily: 'var(--font-mono)',
        }}>
          {time ? new Date(time).toLocaleString('zh-CN', { 
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
          }) : '-'}
        </span>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 220,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="text"
            icon={<KeyOutlined />}
            onClick={() => handleGetCredentials(record)}
            style={{ color: 'var(--color-accent)' }}
            disabled={!record.appKey}
          >
            凭证
          </Button>
          {record.hasCredentials && (
            <Button
              type="text"
              icon={<ReloadOutlined />}
              onClick={() => handleRegenerateCredentials(record)}
              style={{ color: 'var(--status-warning)' }}
            >
              重置
            </Button>
          )}
          <Button
            type="text"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
            style={{ color: 'var(--color-accent)' }}
          >
            编辑
          </Button>
          <Popconfirm
            title="确认删除"
            description="确定要删除这个系统应用吗？"
            onConfirm={() => handleDelete(record.id)}
            okText="确认"
            cancelText="取消"
          >
            <Button
              type="text"
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

  return (
    <PageContainer
      title="系统应用管理"
      icon={<AppstoreOutlined />}
      actions={
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          创建应用
        </Button>
      }
    >
      <div style={{ 
        marginBottom: 16, 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '12px'
      }}>
        <Space wrap size="middle">
          <Input
            placeholder="搜索应用名称或App Key"
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            onPressEnter={handleSearch}
            style={{ width: 240 }}
            prefix={<SearchOutlined />}
            allowClear
          />
          <Select
            placeholder="筛选状态"
            value={filterStatus}
            onChange={setFilterStatus}
            style={{ width: 130 }}
            allowClear
          >
            <Option value="ACTIVE">活跃</Option>
            <Option value="INACTIVE">未激活</Option>
            <Option value="SUSPENDED">已暂停</Option>
          </Select>
          <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
            搜索
          </Button>
          <Button onClick={handleReset} icon={<ReloadOutlined />}>
            重置
          </Button>
        </Space>
        
        <Space>
          <span style={{ color: 'var(--color-text-secondary)', fontSize: '13px' }}>
            共 {pagination.total || 0} 个应用
          </span>
        </Space>
      </div>

      {selectedRowKeys.length > 0 && (
        <div style={{ 
          marginBottom: 16, 
          padding: '16px 20px', 
          background: 'linear-gradient(135deg, rgba(245, 108, 108, 0.1) 0%, rgba(245, 108, 108, 0.05) 100%)',
          borderRadius: '8px', 
          border: '1px solid rgba(245, 108, 108, 0.3)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}>
          <Space>
            <span style={{ 
              fontSize: '14px', 
              fontWeight: 500,
              color: '#cf1322'
            }}>
              已选择 <strong>{selectedRowKeys.length}</strong> 个应用
            </span>
          </Space>
          <Space>
            <Button onClick={() => setSelectedRowKeys([])}>
              取消选择
            </Button>
            <Popconfirm
              title="确认批量删除"
              description={`确定要删除选中的 ${selectedRowKeys.length} 个系统应用吗？此操作不可恢复。`}
              onConfirm={handleBatchDelete}
              okText="确定删除"
              cancelText="取消"
              okButtonProps={{ danger: true }}
            >
              <Button danger icon={<DeleteOutlined />}>
                批量删除
              </Button>
            </Popconfirm>
          </Space>
        </div>
      )}

      <Table
        columns={columns}
        dataSource={systemApps}
        loading={loading}
        rowKey="id"
        rowSelection={rowSelection}
        pagination={{
          ...pagination,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total, range) => `显示 ${range[0]}-${range[1]} 条，共 ${total} 条`,
          pageSizeOptions: ['10', '20', '50', '100'],
        }}
        onChange={handleTableChange}
        scroll={{ x: 1400 }}
        locale={{
          emptyText: (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="暂无系统应用数据"
            />
          )
        }}
        style={{
          background: 'transparent',
        }}
        rowClassName={(record, index) => 
          `table-row-${index % 2 === 0 ? 'even' : 'odd'}`
        }
      />
      
      <style>{`
        .table-row-even {
          background: var(--color-bg-card) !important;
        }
        .table-row-odd {
          background: rgba(0, 0, 0, 0.02) !important;
        }
        .table-row-even:hover > td,
        .table-row-odd:hover > td {
          background: var(--color-bg-secondary) !important;
        }
      `}</style>

      {/* 创建/编辑系统应用的 Modal */}
      <Modal
        title={editingApp ? '编辑系统应用' : '创建系统应用'}
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
          {editingApp && editingApp.appKey && (
            <Form.Item label="App Key">
              <Input 
                value={editingApp.appKey} 
                readOnly 
                addonAfter={
                  <CopyOutlined 
                    onClick={() => handleCopyAppKey(editingApp.appKey)}
                    style={{ cursor: 'pointer' }}
                  />
                }
              />
            </Form.Item>
          )}

          <Form.Item
            name="name"
            label="应用名称"
            rules={[{ required: true, message: '请输入应用名称' }]}
          >
            <Input placeholder="例如: Edge-Main" />
          </Form.Item>

          <Form.Item
            name="description"
            label="应用描述"
          >
            <TextArea rows={3} placeholder="请输入应用描述" />
          </Form.Item>

          <Form.Item
            name="ownerType"
            label="所有者类型"
            rules={[{ required: true, message: '请选择所有者类型' }]}
          >
            <Select placeholder="请选择所有者类型">
              <Option value="SYSTEM">系统</Option>
              <Option value="USER">用户</Option>
            </Select>
          </Form.Item>

          <Form.Item
            noStyle
            shouldUpdate={(prevValues, currentValues) => prevValues.ownerType !== currentValues.ownerType}
          >
            {({ getFieldValue }) => 
              getFieldValue('ownerType') === 'USER' && (
                <Form.Item
                  name="ownerId"
                  label="所有者ID"
                  rules={[{ required: true, message: '请输入所有者ID' }]}
                >
                  <Input type="number" placeholder="请输入用户ID" />
                </Form.Item>
              )
            }
          </Form.Item>

          <Form.Item
            name="status"
            label="状态"
            rules={[{ required: true, message: '请选择状态' }]}
          >
            <Select placeholder="请选择状态">
              <Option value="ACTIVE">活跃</Option>
              <Option value="INACTIVE">未激活</Option>
              <Option value="SUSPENDED">已暂停</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="permissions"
            label="权限配置"
          >
            <Checkbox.Group style={{ width: '100%' }}>
              <Row>
                {availablePermissions.map(perm => (
                  <Col span={8} key={perm.value}>
                    <Checkbox value={perm.value}>{perm.label}</Checkbox>
                  </Col>
                ))}
              </Row>
            </Checkbox.Group>
          </Form.Item>
        </Form>
      </Modal>

      {/* 显示凭证的 Modal */}
      <Modal
        title="系统应用凭证"
        open={credentialsModalVisible}
        onCancel={() => {
          setCredentialsModalVisible(false);
          setCreatedCredentials(null);
        }}
        footer={[
          <Button key="close" type="primary" onClick={() => {
            setCredentialsModalVisible(false);
            setCreatedCredentials(null);
          }}>
            我已保存
          </Button>,
        ]}
        width={600}
      >
        {createdCredentials && (
          <div>
            {createdCredentials.warning && (
              <Alert
                message={createdCredentials.warning}
                type="warning"
                showIcon
                style={{ marginBottom: 16 }}
              />
            )}

            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="应用名称">
                {createdCredentials.name}
              </Descriptions.Item>
              <Descriptions.Item label="App Key">
                <Space>
                  <code style={{ 
                    fontSize: '13px', 
                    color: 'var(--color-accent)',
                    fontFamily: 'var(--font-mono)',
                    background: 'var(--color-bg-secondary)',
                    padding: '4px 8px',
                    borderRadius: '4px',
                  }}>
                    {createdCredentials.appKey}
                  </code>
                  <Button 
                    size="small" 
                    icon={<CopyOutlined />}
                    onClick={() => handleCopyAppKey(createdCredentials.appKey, 'App Key')}
                  >
                    复制
                  </Button>
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="App Secret">
                <Space direction="vertical" style={{ width: '100%' }}>
                  <code style={{ 
                    fontSize: '13px', 
                    color: 'var(--status-warning)',
                    fontFamily: 'var(--font-mono)',
                    background: 'var(--color-bg-secondary)',
                    padding: '8px',
                    borderRadius: '4px',
                    display: 'block',
                    wordBreak: 'break-all',
                  }}>
                    {createdCredentials.appSecret}
                  </code>
                  <Button 
                    type="primary" 
                    danger
                    icon={<CopyOutlined />}
                    onClick={() => handleCopyAppKey(createdCredentials.appSecret, 'App Secret')}
                  >
                    复制 App Secret
                  </Button>
                </Space>
              </Descriptions.Item>
            </Descriptions>
          </div>
        )}
      </Modal>
    </PageContainer>
  );
};

export default SystemAppManagement;
