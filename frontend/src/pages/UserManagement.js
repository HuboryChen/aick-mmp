import React, { useState, useEffect } from 'react';
import { message, Modal, Button, Table, Space, Input, Select, Tag, Popconfirm, Form, Drawer, InputNumber } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, LockOutlined, UnlockOutlined, SearchOutlined } from '@ant-design/icons';
import { userApi } from '../utils/api';

const { Option } = Select;

const UserManagement = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [filterRole, setFilterRole] = useState(undefined);
  const [filterStatus, setFilterStatus] = useState(undefined);
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [modalVisible, setModalVisible] = useState(false);
  const [passwordModalVisible, setPasswordModalVisible] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [roles, setRoles] = useState([]);
  const [form] = Form.useForm();
  const [passwordForm] = Form.useForm();

  useEffect(() => {
    fetchUsers();
    fetchRoles();
  }, [pagination.current, pagination.pageSize, searchKeyword, filterRole, filterStatus]);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      let params = {
        page: pagination.current - 1,
        size: pagination.pageSize,
      };

      if (searchKeyword) params.keyword = searchKeyword;
      if (filterRole) params.role = filterRole;
      if (filterStatus) params.status = filterStatus;

      const response = await userApi.searchUsers(params);
      setUsers(response.content);
      setPagination({
        ...pagination,
        total: response.totalElements,
      });
    } catch (error) {
      message.error('获取用户列表失败: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const fetchRoles = async () => {
    try {
      const response = await userApi.getRoles();
      setRoles(response);
    } catch (error) {
      message.error('获取角色列表失败: ' + error.message);
    }
  };

  const handleSearch = () => {
    setPagination({ ...pagination, current: 1 });
    fetchUsers();
  };

  const handleReset = () => {
    setSearchKeyword('');
    setFilterRole(undefined);
    setFilterStatus(undefined);
    setPagination({ ...pagination, current: 1 });
  };

  const handleAdd = () => {
    setEditingUser(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (user) => {
    setEditingUser(user);
    form.setFieldsValue({
      ...user,
      role: user.role,
      status: user.status,
    });
    setModalVisible(true);
  };

  const handleDelete = async (id) => {
    try {
      await userApi.deleteUser(id);
      message.success('删除成功');
      fetchUsers();
    } catch (error) {
      message.error('删除失败: ' + error.message);
    }
  };

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields();
      if (editingUser) {
        await userApi.updateUser(editingUser.id, values);
        message.success('更新成功');
      } else {
        await userApi.createUser(values);
        message.success('创建成功');
      }
      setModalVisible(false);
      fetchUsers();
    } catch (error) {
      message.error('操作失败: ' + error.message);
    }
  };

  const handleChangePassword = (user) => {
    setEditingUser(user);
    passwordForm.resetFields();
    setPasswordModalVisible(true);
  };

  const handlePasswordOk = async () => {
    try {
      const values = await passwordForm.validateFields();
      await userApi.changePassword(editingUser.id, values);
      message.success('修改密码成功');
      setPasswordModalVisible(false);
    } catch (error) {
      message.error('修改密码失败: ' + error.message);
    }
  };

  const handleBatchDelete = async () => {
    try {
      await userApi.batchDeleteUsers({ userIds: selectedRowKeys });
      message.success('批量删除成功');
      setSelectedRowKeys([]);
      fetchUsers();
    } catch (error) {
      message.error('批量删除失败: ' + error.message);
    }
  };

  const handleBatchEnable = async (enabled) => {
    try {
      await userApi.batchEnableUsers({ userIds: selectedRowKeys }, enabled);
      message.success(`批量${enabled ? '启用' : '禁用'}成功`);
      setSelectedRowKeys([]);
      fetchUsers();
    } catch (error) {
      message.error('批量操作失败: ' + error.message);
    }
  };

  const handleBatchUpdateRole = async (role) => {
    try {
      await userApi.batchUpdateUserRole({ userIds: selectedRowKeys }, role);
      message.success('批量更新角色成功');
      setSelectedRowKeys([]);
      fetchUsers();
    } catch (error) {
      message.error('批量更新角色失败: ' + error.message);
    }
  };

  const rowSelection = {
    selectedRowKeys,
    onChange: setSelectedRowKeys,
  };

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
    },
    {
      title: '姓名',
      dataIndex: 'fullName',
      key: 'fullName',
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
    },
    {
      title: '部门',
      dataIndex: 'department',
      key: 'department',
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      render: (role) => {
        const roleMap = {
          ADMIN: { text: '管理员', color: 'red' },
          OPERATOR: { text: '操作员', color: 'blue' },
          VIEWER: { text: '查看者', color: 'green' },
        };
        const { text, color } = roleMap[role] || { text: role, color: 'default' };
        return <Tag color={color}>{text}</Tag>;
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        const statusMap = {
          ACTIVE: { text: '活跃', color: 'green' },
          INACTIVE: { text: '未激活', color: 'gray' },
          LOCKED: { text: '锁定', color: 'red' },
          EXPIRED: { text: '已过期', color: 'orange' },
        };
        const { text, color } = statusMap[status] || { text: status, color: 'default' };
        return <Tag color={color}>{text}</Tag>;
      },
    },
    {
      title: '是否启用',
      dataIndex: 'enabled',
      key: 'enabled',
      render: (enabled) => (enabled ? <Tag color="green">是</Tag> : <Tag color="red">否</Tag>),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="small">
          <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Button type="link" icon={<LockOutlined />} onClick={() => handleChangePassword(record)}>
            修改密码
          </Button>
          <Popconfirm
            title="确定删除该用户吗？"
            onConfirm={() => handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" danger icon={<DeleteOutlined()}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: '24px' }}>
      <div style={{ marginBottom: '16px' }}>
        <Space>
          <Input
            placeholder="搜索用户名、邮箱或姓名"
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            onPressEnter={handleSearch}
            style={{ width: 200 }}
            prefix={<SearchOutlined />}
          />
          <Select
            placeholder="角色"
            value={filterRole}
            onChange={setFilterRole}
            style={{ width: 120 }}
            allowClear
          >
            {roles.map((role) => (
              <Option key={role} value={role}>
                {role === 'ADMIN' ? '管理员' : role === 'OPERATOR' ? '操作员' : '查看者'}
              </Option>
            ))}
          </Select>
          <Select
            placeholder="状态"
            value={filterStatus}
            onChange={setFilterStatus}
            style={{ width: 120 }}
            allowClear
          >
            <Option value="ACTIVE">活跃</Option>
            <Option value="INACTIVE">未激活</Option>
            <Option value="LOCKED">锁定</Option>
            <Option value="EXPIRED">已过期</Option>
          </Select>
          <Button type="primary" onClick={handleSearch}>
            搜索
          </Button>
          <Button onClick={handleReset}>重置</Button>
        </Space>
      </div>

      <div style={{ marginBottom: '16px' }}>
        <Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            添加用户
          </Button>
          {selectedRowKeys.length > 0 && (
            <>
              <Popconfirm
                title="确定批量删除选中的用户吗？"
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
              <Select
                placeholder="批量更新角色"
                style={{ width: 150 }}
                onChange={handleBatchUpdateRole}
                allowClear
              >
                {roles.map((role) => (
                  <Option key={role} value={role}>
                    {role === 'ADMIN' ? '管理员' : role === 'OPERATOR' ? '操作员' : '查看者'}
                  </Option>
                ))}
              </Select>
            </>
          )}
        </Space>
      </div>

      <Table
        columns={columns}
        dataSource={users}
        rowKey="id"
        loading={loading}
        rowSelection={rowSelection}
        pagination={{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条`,
          onChange: (page, pageSize) => setPagination({ ...pagination, current: page, pageSize }),
        }}
      />

      <Modal
        title={editingUser ? '编辑用户' : '添加用户'}
        visible={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="用户名"
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="请输入用户名" />
          </Form.Item>
          <Form.Item
            label="邮箱"
            name="email"
            rules={[
              { required: true, message: '请输入邮箱' },
              { type: 'email', message: '请输入有效的邮箱地址' },
            ]}
          >
            <Input placeholder="请输入邮箱" />
          </Form.Item>
          <Form.Item
            label="密码"
            name="password"
            rules={
              !editingUser
                ? [
                    { required: true, message: '请输入密码' },
                    { min: 6, message: '密码长度不能少于6位' },
                  ]
                : []
            }
          >
            <Input.Password placeholder="请输入密码" />
          </Form.Item>
          <Form.Item label="姓名" name="fullName">
            <Input placeholder="请输入姓名" />
          </Form.Item>
          <Form.Item label="手机号" name="phone">
            <Input placeholder="请输入手机号" />
          </Form.Item>
          <Form.Item label="部门" name="department">
            <Input placeholder="请输入部门" />
          </Form.Item>
          <Form.Item
            label="角色"
            name="role"
            rules={[{ required: true, message: '请选择角色' }]}
          >
            <Select placeholder="请选择角色">
              {roles.map((role) => (
                <Option key={role} value={role}>
                  {role === 'ADMIN' ? '管理员' : role === 'OPERATOR' ? '操作员' : '查看者'}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item
            label="状态"
            name="status"
            rules={[{ required: true, message: '请选择状态' }]}
          >
            <Select placeholder="请选择状态">
              <Option value="ACTIVE">活跃</Option>
              <Option value="INACTIVE">未激活</Option>
              <Option value="LOCKED">锁定</Option>
              <Option value="EXPIRED">已过期</Option>
            </Select>
          </Form.Item>
          <Form.Item label="是否启用" name="enabled" valuePropName="checked">
            <Select placeholder="请选择是否启用">
              <Option value={true}>是</Option>
              <Option value={false}>否</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="修改密码"
        visible={passwordModalVisible}
        onOk={handlePasswordOk}
        onCancel={() => setPasswordModalVisible(false)}
        width={500}
      >
        <Form form={passwordForm} layout="vertical">
          <Form.Item
            label="旧密码"
            name="oldPassword"
            rules={[{ required: true, message: '请输入旧密码' }]}
          >
            <Input.Password placeholder="请输入旧密码" />
          </Form.Item>
          <Form.Item
            label="新密码"
            name="newPassword"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 6, message: '新密码长度不能少于6位' },
            ]}
          >
            <Input.Password placeholder="请输入新密码" />
          </Form.Item>
          <Form.Item
            label="确认新密码"
            name="confirmPassword"
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
            <Input.Password placeholder="请再次输入新密码" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default UserManagement;
