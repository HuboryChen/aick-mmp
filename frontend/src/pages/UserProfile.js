import React, { useState, useEffect } from 'react';
import { Card, Typography, Form, Input, Button, Avatar, Row, Col, Table, Tag, Space, message, Modal, Tabs, Switch, Descriptions, Tooltip } from 'antd';
import {
  UserOutlined,
  EditOutlined,
  LockOutlined,
  KeyOutlined,
  CopyOutlined,
  PlusOutlined,
  DeleteOutlined,
  TeamOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';
import PageContainer from '../components/PageContainer';
import axiosInstance from '../utils/axios';

const { Title, Text } = Typography;
const { TabPane } = Tabs;

/* ==================== Tab 1: Basic Info ==================== */
const ProfileBasicInfo = ({ userInfo }) => {
  const [form] = Form.useForm();
  const [editMode, setEditMode] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleProfileUpdate = async (values) => {
    setLoading(true);
    try {
      await new Promise(resolve => setTimeout(resolve, 800));
      message.success('个人信息更新成功');
      setEditMode(false);
    } catch (error) {
      message.error('更新失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card
      className="profile-card"
      title={
        <Space>
          <UserOutlined style={{ color: 'var(--color-accent)' }} />
          <span>基础信息</span>
        </Space>
      }
      extra={
        <Button
          type="link"
          icon={<EditOutlined />}
          onClick={() => setEditMode(!editMode)}
          style={{ fontWeight: 500 }}
        >
          {editMode ? '取消' : '编辑'}
        </Button>
      }
    >
      <Row gutter={[24, 0]}>
        {/* Left: Avatar & Quick Info */}
        <Col xs={24} sm={8} md={6}>
          <div style={{ textAlign: 'center', padding: '16px 0' }}>
            <Avatar
              size={80}
              icon={<UserOutlined />}
              src={userInfo?.avatar}
              style={{
                background: 'linear-gradient(135deg, #00d4ff 0%, #0099cc 100%)',
                boxShadow: '0 0 20px rgba(0,212,255,0.3)',
                marginBottom: 12,
              }}
            />
            <Title level={5} style={{ margin: '4px 0', color: 'var(--color-text-primary)' }}>
              {userInfo?.fullName || userInfo?.username || 'Admin'}
            </Title>
            <Tag color="cyan" style={{ borderRadius: 10 }}>
              {userInfo?.role || '管理员'}
            </Tag>
            <div style={{ marginTop: 12 }}>
              <Text type="secondary" style={{ fontSize: 12 }}>
                最后登录: {userInfo?.lastLogin || '--'}
              </Text>
            </div>
          </div>
        </Col>

        {/* Right: Editable Form */}
        <Col xs={24} sm={16} md={18}>
          <Form
            form={form}
            layout="vertical"
            initialValues={userInfo}
            onFinish={handleProfileUpdate}
          >
            <Row gutter={[16, 0]}>
              <Col xs={24} sm={12}>
                <Form.Item name="username" label="用户名">
                  <Input disabled prefix={<UserOutlined />} placeholder="用户名" />
                </Form.Item>
              </Col>
              <Col xs={24} sm={12}>
                <Form.Item
                  name="email"
                  label="邮箱"
                  rules={[{ required: true, message: '请输入邮箱' }, { type: 'email', message: '邮箱格式不正确' }]}
                >
                  <Input disabled={!editMode} prefix="@ " placeholder="邮箱" />
                </Form.Item>
              </Col>
              <Col xs={24} sm={12}>
                <Form.Item name="fullName" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
                  <Input disabled={!editMode} placeholder="姓名" />
                </Form.Item>
              </Col>
              <Col xs={24} sm={12}>
                <Form.Item name="phone" label="手机号">
                  <Input disabled={!editMode} placeholder="手机号" />
                </Form.Item>
              </Col>
              <Col xs={24} sm={12}>
                <Form.Item name="department" label="部门">
                  <Input disabled={!editMode} placeholder="部门" />
                </Form.Item>
              </Col>
              <Col xs={24} sm={12}>
                <Form.Item name="role" label="角色">
                  <Input disabled prefix={<TeamOutlined />} placeholder="角色" />
                </Form.Item>
              </Col>
            </Row>

            {editMode && (
              <div style={{ textAlign: 'right', marginTop: 8 }}>
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={loading}
                  icon={<CheckCircleOutlined />}
                >
                  保存修改
                </Button>
              </div>
            )}
          </Form>
        </Col>
      </Row>
    </Card>
  );
};

/* ==================== Tab 2: Password Reset ==================== */
const ProfilePasswordReset = () => {
  const [passwordForm] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const handlePasswordChange = async (values) => {
    setLoading(true);
    try {
      await new Promise(resolve => setTimeout(resolve, 800));
      message.success('密码修改成功，请重新登录');
      passwordForm.resetFields();
    } catch (error) {
      message.error('密码修改失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card
      className="profile-card"
      title={
        <Space>
          <LockOutlined style={{ color: 'var(--color-accent)' }} />
          <span>修改密码</span>
        </Space>
      }
    >
      <div style={{ maxWidth: 480, margin: '0 auto' }}>
        <Form
          form={passwordForm}
          layout="vertical"
          onFinish={handlePasswordChange}
        >
          <Form.Item
            name="currentPassword"
            label="当前密码"
            rules={[{ required: true, message: '请输入当前密码' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="请输入当前密码" size="large" />
          </Form.Item>

          <Form.Item
            name="newPassword"
            label="新密码"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 6, message: '密码至少6位字符' },
            ]}
          >
            <Input.Password prefix={<KeyOutlined />} placeholder="请输入新密码（至少6位）" size="large" />
          </Form.Item>

          <Form.Item
            name="confirmPassword"
            label="确认新密码"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: '请确认新密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('两次输入的密码不一致'));
                },
              }),
            ]}
          >
            <Input.Password prefix={<SafetyCertificateOutlined />} placeholder="请再次输入新密码" size="large" />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              size="large"
              icon={<LockOutlined />}
            >
              确认修改密码
            </Button>
          </Form.Item>
        </Form>
      </div>
    </Card>
  );
};

/* ==================== Tab 3: ApiKey Management ==================== */
const ProfileApiKeys = () => {
  const [apiKeys, setApiKeys] = useState([]);
  const [loading, setLoading] = useState(false);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [createForm] = Form.useForm();

  useEffect(() => {
    fetchApiKeys();
  }, []);

  const fetchApiKeys = async () => {
    setLoading(true);
    try {
      const response = await axiosInstance.get('/api-keys/me');
      setApiKeys(response.data || []);
    } catch (error) {
      // Use demo data on error
      setApiKeys([]);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateApiKey = async (values) => {
    try {
      await axiosInstance.post('/api-keys/me', values);
      message.success('API Key 创建成功');
      setCreateModalOpen(false);
      createForm.resetFields();
      fetchApiKeys();
    } catch (error) {
      message.error('创建失败: ' + (error.response?.data?.message || '未知错误'));
    }
  };

  const handleToggleStatus = async (id, currentStatus) => {
    try {
      const newStatus = currentStatus === 'ENABLED' ? 'DISABLED' : 'ENABLED';
      await axiosInstance.put(`/api-keys/me/${id}/status`, { status: newStatus });
      message.success(`已${newStatus === 'ENABLED' ? '启用' : '禁用'}`);
      fetchApiKeys();
    } catch (error) {
      message.error('操作失败');
    }
  };

  const handleDelete = async (id) => {
    Modal.confirm({
      title: '确认删除',
      icon: <ExclamationCircleOutlined />,
      content: '删除后无法恢复，确定要删除这个 API Key 吗？',
      okText: '删除',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await axiosInstance.delete(`/api-keys/me/${id}`);
          message.success('已删除');
          fetchApiKeys();
        } catch (error) {
          message.error('删除失败');
        }
      },
    });
  };

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text).then(() => {
      message.success('已复制到剪贴板');
    }).catch(() => {
      message.error('复制失败');
    });
  };

  const columns = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      render: (text) => <Text strong>{text || '--'}</Text>,
    },
    {
      title: 'Access Key',
      dataIndex: 'accessKey',
      key: 'accessKey',
      width: 200,
      render: (key) => (
        <Tooltip title="点击复制">
          <Text
            code
            copyable={{ text: key }}
            style={{ cursor: 'pointer', fontFamily: 'monospace', fontSize: 12 }}
          >
            {key ? `${key.slice(0, 8)}...` : '--'}
          </Text>
        </Tooltip>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status) => (
        <Tag color={status === 'ENABLED' ? 'success' : 'default'}>
          {status === 'ENABLED' ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (time) => time ? new Date(time).toLocaleString() : '--',
    },
    {
      title: '操作',
      key: 'action',
      width: 140,
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            onClick={() => handleToggleStatus(record.id, record.status)}
          >
            {record.status === 'ENABLED' ? '禁用' : '启用'}
          </Button>
          <Button
            type="link"
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record.id)}
          />
        </Space>
      ),
    },
  ];

  return (
    <Card
      className="profile-card"
      title={
        <Space>
          <KeyOutlined style={{ color: 'var(--color-accent)' }} />
          <span>ApiKey 管理</span>
          <Tag color="blue">{apiKeys.length}</Tag>
        </Space>
      }
      extra={
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => setCreateModalOpen(true)}
          size="small"
        >
          创建 Key
        </Button>
      }
    >
      <Table
        dataSource={apiKeys}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 8, size: 'small' }}
        locale={{ emptyText: '暂无 API Key，点击右上角按钮创建' }}
        size="middle"
      />

      {/* Create ApiKey Modal */}
      <Modal
        title="创建 API Key"
        open={createModalOpen}
        onCancel={() => setCreateModalOpen(false)}
        footer={null}
        destroyOnClose
      >
        <Form form={createForm} layout="vertical" onFinish={handleCreateApiKey}>
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入 Key 名称' }]}
          >
            <Input placeholder="例如：生产环境调用" />
          </Form.Item>
          <Form.Item
            name="description"
            label="描述（可选）"
          >
            <Input.TextArea rows={3} placeholder="描述此 Key 的用途..." />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button onClick={() => setCreateModalOpen(false)}>取消</Button>
              <Button type="primary" htmlType="submit" icon={<PlusOutlined />}>
                创建
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

/* ==================== Tab 4: Role Management (conditional) ==================== */
const ProfileRoleManagement = ({ userInfo }) => {
  const hasRolePermission = userInfo?.role === '超级管理员' || userInfo?.role === 'ADMIN';

  // Hooks must be called before any conditional returns
  const [roles] = useState([
    { id: 1, name: '管理员', code: 'ADMIN', userCount: 2, description: '系统管理权限' },
    { id: 2, name: '操作员', code: 'OPERATOR', userCount: 5, description: '日常操作权限' },
    { id: 3, name: '观察者', code: 'VIEWER', userCount: 10, description: '只读查看权限' },
  ]);

  if (!hasRolePermission) {
    return (
      <Card className="profile-card">
        <div style={{ textAlign: 'center', padding: '48px 0', color: 'var(--color-text-muted)' }}>
          <ExclamationCircleOutlined style={{ fontSize: 40, marginBottom: 16, display: 'block' }} />
          <Title level={5}>无权限</Title>
          <p>当前用户角色不具备角色管理权限</p>
        </div>
      </Card>
    );
  }

  const columns = [
    { title: '角色名称', dataIndex: 'name', render: (t) => <Text strong>{t}</Text> },
    { title: '角色编码', dataIndex: 'code', render: (c) => <Text code>{c}</Text> },
    { title: '关联用户数', dataIndex: 'userCount', align: 'center' },
    { title: '描述', dataIndex: 'description' },
  ];

  return (
    <Card
      className="profile-card"
      title={
        <Space>
          <TeamOutlined style={{ color: 'var(--color-accent)' }} />
          <span>角色管理</span>
        </Space>
      }
    >
      <Table
        dataSource={roles}
        columns={columns}
        rowKey="id"
        pagination={false}
        size="middle"
      />
    </Card>
  );
};

/* ==================== Main UserProfile Component ==================== */

const UserProfile = () => {
  const [activeTab, setActiveTab] = useState(
    new URLSearchParams(window.location.search).get('tab') || 'basic'
  );

  const userInfo = {
    username: 'admin',
    email: 'admin@example.com',
    fullName: '系统管理员',
    phone: '13800138000',
    department: '技术部',
    role: '超级管理员',
    lastLogin: '2024-01-15 14:30:25',
  };

  const tabItems = [
    {
      key: 'basic',
      label: (
        <Space>
          <UserOutlined /> 基础信息
        </Space>
      ),
      children: <ProfileBasicInfo userInfo={userInfo} />,
    },
    {
      key: 'apikey',
      label: (
        <Space>
          <KeyOutlined /> ApiKey 管理
        </Space>
      ),
      children: <ProfileApiKeys />,
    },
    {
      key: 'role',
      label: (
        <Space>
          <TeamOutlined /> 角色管理
        </Space>
      ),
      children: <ProfileRoleManagement userInfo={userInfo} />,
    },
    {
      key: 'password',
      label: (
        <Space>
          <LockOutlined /> 重置密码
        </Space>
      ),
      children: <ProfilePasswordReset />,
    },
  ];

  return (
    <PageContainer
      title="个人中心"
      icon={<UserOutlined />}
    >
      <Card bordered={false} style={{ background: 'transparent', padding: 0 }} styles={{ body: { padding: 0 } }}>
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={tabItems}
          type="card"
          size="large"
          style={{ background: 'transparent' }}
        />
      </Card>
    </PageContainer>
  );
};

export default UserProfile;
