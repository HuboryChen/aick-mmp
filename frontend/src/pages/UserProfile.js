import React, { useState } from 'react';
import { Card, Typography, Form, Input, Button, Avatar, Row, Col, message, Space } from 'antd';
import { UserOutlined, EditOutlined, LockOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;

const UserProfile = () => {
  const [form] = Form.useForm();
  const [passwordForm] = Form.useForm();
  const [editMode, setEditMode] = useState(false);
  const [loading, setLoading] = useState(false);

  const userInfo = {
    username: 'admin',
    email: 'admin@example.com',
    fullName: '系统管理员',
    phone: '13800138000',
    department: '技术部',
    role: '超级管理员',
    lastLogin: '2024-01-15 14:30:25',
    createdAt: '2023-12-01 10:00:00'
  };

  const handleProfileUpdate = async (values) => {
    setLoading(true);
    try {
      // 模拟更新用户信息
      await new Promise(resolve => setTimeout(resolve, 1000));
      message.success('个人信息更新成功');
      setEditMode(false);
    } catch (error) {
      message.error('更新失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  const handlePasswordChange = async (values) => {
    setLoading(true);
    try {
      // 模拟修改密码
      await new Promise(resolve => setTimeout(resolve, 1000));
      message.success('密码修改成功');
      passwordForm.resetFields();
    } catch (error) {
      message.error('密码修改失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <Title level={2}>个人中心</Title>
      
      <Row gutter={24}>
        {/* 基本信息 */}
        <Col xs={24} lg={16}>
          <Card
            title="基本信息"
            extra={
              <Button
                type="link"
                icon={<EditOutlined />}
                onClick={() => setEditMode(!editMode)}
              >
                {editMode ? '取消编辑' : '编辑信息'}
              </Button>
            }
          >
            <Form
              form={form}
              layout="vertical"
              initialValues={userInfo}
              onFinish={handleProfileUpdate}
            >
              <Row gutter={16}>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="username"
                    label="用户名"
                  >
                    <Input disabled placeholder="用户名" />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="email"
                    label="邮箱"
                    rules={[
                      { required: true, message: '请输入邮箱' },
                      { type: 'email', message: '请输入有效的邮箱地址' }
                    ]}
                  >
                    <Input disabled={!editMode} placeholder="邮箱" />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="fullName"
                    label="姓名"
                    rules={[{ required: true, message: '请输入姓名' }]}
                  >
                    <Input disabled={!editMode} placeholder="姓名" />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="phone"
                    label="手机号"
                  >
                    <Input disabled={!editMode} placeholder="手机号" />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="department"
                    label="部门"
                  >
                    <Input disabled={!editMode} placeholder="部门" />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="role"
                    label="角色"
                  >
                    <Input disabled placeholder="角色" />
                  </Form.Item>
                </Col>
              </Row>
              
              {editMode && (
                <div style={{ textAlign: 'center' }}>
                  <Button
                    type="primary"
                    htmlType="submit"
                    loading={loading}
                  >
                    保存修改
                  </Button>
                </div>
              )}
            </Form>
          </Card>
        </Col>

        {/* 侧边栏 */}
        <Col xs={24} lg={8}>
          {/* 头像卡片 */}
          <Card style={{ marginBottom: '24px', textAlign: 'center' }}>
            <Avatar size={80} icon={<UserOutlined />} style={{ marginBottom: '16px' }} />
            <Title level={4} style={{ margin: 0 }}>{userInfo.fullName}</Title>
            <Text type="secondary">{userInfo.role}</Text>
            <div style={{ marginTop: '16px' }}>
              <Space direction="vertical" style={{ width: '100%' }}>
                <Text type="secondary">最后登录: {userInfo.lastLogin}</Text>
                <Text type="secondary">注册时间: {userInfo.createdAt}</Text>
              </Space>
            </div>
          </Card>

          {/* 修改密码 */}
          <Card title="修改密码">
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
                <Input.Password
                  prefix={<LockOutlined />}
                  placeholder="请输入当前密码"
                />
              </Form.Item>
              
              <Form.Item
                name="newPassword"
                label="新密码"
                rules={[
                  { required: true, message: '请输入新密码' },
                  { min: 6, message: '密码至少6位' }
                ]}
              >
                <Input.Password
                  prefix={<LockOutlined />}
                  placeholder="请输入新密码"
                />
              </Form.Item>
              
              <Form.Item
                name="confirmPassword"
                label="确认密码"
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
                <Input.Password
                  prefix={<LockOutlined />}
                  placeholder="请确认新密码"
                />
              </Form.Item>
              
              <Form.Item>
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={loading}
                  style={{ width: '100%' }}
                >
                  修改密码
                </Button>
              </Form.Item>
            </Form>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default UserProfile;