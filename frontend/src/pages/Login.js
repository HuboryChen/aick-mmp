import React, { useState, useEffect, useRef } from 'react';
import { Form, Input, Button, Card, Typography, message, Row, Col, Checkbox } from 'antd';
import { UserOutlined, LockOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import './Login.css';

const { Title, Text } = Typography;

const Login = () => {
  const [loading, setLoading] = useState(false);
  const [remember, setRemember] = useState(false);
  const navigate = useNavigate();
  const { login, userInfo } = useAuth();
  const loginSuccessRef = useRef(false);

  const onFinish = async (values) => {
    setLoading(true);
    loginSuccessRef.current = false;
    try {
      const result = await login(values);

      if (result.success) {
        message.success('登录成功！');
        loginSuccessRef.current = true;
      } else {
        message.error(result.error || '登录失败，请检查用户名和密码');
        setLoading(false);
      }
    } catch (error) {
      console.error('Login error:', error);
      const errorMessage = error.response?.data?.message ||
                          error.response?.data?.error ||
                          error.message ||
                          '登录失败，请检查用户名和密码';
      message.error(errorMessage);
      setLoading(false);
    }
  };

  // 监听 userInfo 变化，登录成功且状态更新后跳转
  useEffect(() => {
    if (loginSuccessRef.current && userInfo) {
      loginSuccessRef.current = false;
      setLoading(false);
      navigate('/dashboard');
    }
  }, [userInfo, navigate]);

  return (
    <div
      className="login-container relative flex min-h-screen items-center justify-center overflow-hidden p-5"
      style={{ background: 'var(--login-bg)' }}
    >
      {/* Background Effects */}
      <div
        className="absolute inset-0 pointer-events-none"
        style={{ background: 'var(--login-radial-glow)' }}
      />

      {/* Grid Pattern */}
      <div
        className="absolute inset-0 pointer-events-none"
        style={{
          backgroundImage: `
            linear-gradient(var(--login-grid-color) 1px, transparent 1px),
            linear-gradient(90deg, var(--login-grid-color) 1px, transparent 1px)
          `,
          backgroundSize: '50px 50px',
        }}
      />

      <Row justify="center" className="relative z-10 w-full">
        <Col xs={22} sm={16} md={12} lg={8} xl={6}>
          {/* Logo Section */}
          <div className="mb-8 text-center" style={{ animation: 'fadeInDown 0.6s ease-out' }}>
            <div
              className="mx-auto mb-5 inline-flex h-20 w-20 items-center justify-center rounded-[20px]"
              style={{
                background: 'var(--login-logo-gradient)',
                boxShadow: 'var(--login-logo-shadow)',
              }}
            >
              <span className="text-2xl font-bold tracking-widest text-white">
                MMP
              </span>
            </div>
            <Title
              level={2}
              className="!mb-2 !tracking-widest !text-text-primary"
              style={{ textShadow: 'var(--login-title-shadow)' }}
            >
              多地区监控平台
            </Title>
            <Text type="secondary" className="text-sm">
              Industrial Command Center
            </Text>
          </div>

          <Card
            className="login-card"
            style={{
              borderRadius: 16,
              border: `1px solid var(--login-card-border)`,
              backdropFilter: 'blur(20px)',
              animation: 'fadeInUp 0.6s ease-out 0.2s both',
              background: 'var(--login-card-bg)',
              boxShadow: 'var(--login-card-shadow)',
            }}
            styles={{ body: { padding: 32 } }}
          >
            <div className="mb-6 text-center">
              <SafetyCertificateOutlined
                className="mb-3 block"
                style={{ fontSize: 32, color: 'var(--login-icon-color)' }}
              />
              <Title level={4} className="!m-0 !text-text-primary">
                身份验证
              </Title>
              <Text type="secondary">请输入您的登录凭据</Text>
            </div>

            <Form
              name="login"
              onFinish={onFinish}
              autoComplete="off"
              size="large"
              layout="vertical"
              requiredMark={false}
            >
              <Form.Item
                label={<span style={{ color: 'var(--color-text-secondary)' }}>用户名</span>}
                name="username"
                rules={[{ required: true, message: '请输入用户名!' }]}
                hasFeedback
              >
                <Input
                  prefix={<UserOutlined style={{ color: 'var(--color-text-muted)' }} />}
                  placeholder="请输入用户名"
                  size="large"
                  className="industrial-input"
                  style={{
                    borderRadius: 8,
                    background: 'var(--login-input-bg)',
                    borderColor: 'var(--login-input-border)',
                    color: 'var(--color-text-primary)',
                  }}
                />
              </Form.Item>

              <Form.Item
                label={<span style={{ color: 'var(--color-text-secondary)' }}>密码</span>}
                name="password"
                rules={[{ required: true, message: '请输入密码!' }]}
                hasFeedback
              >
                <Input.Password
                  prefix={<LockOutlined style={{ color: 'var(--color-text-muted)' }} />}
                  placeholder="请输入密码"
                  size="large"
                  className="industrial-input"
                  style={{
                    borderRadius: 8,
                    background: 'var(--login-input-bg)',
                    borderColor: 'var(--login-input-border)',
                    color: 'var(--color-text-primary)',
                  }}
                />
              </Form.Item>

              <Form.Item>
                <Form.Item name="remember" valuePropName="checked" noStyle>
                  <Checkbox
                    onChange={(e) => setRemember(e.target.checked)}
                    style={{ color: 'var(--color-text-secondary)' }}
                  >
                    记住我
                  </Checkbox>
                </Form.Item>
              </Form.Item>

              <Form.Item className="!mb-4">
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={loading}
                  block
                  className="login-button h-12 rounded-lg border-none text-base font-semibold tracking-wider"
                  style={{
                    background: 'var(--login-btn-gradient)',
                    boxShadow: 'var(--login-btn-shadow)',
                  }}
                >
                  登 录
                </Button>
              </Form.Item>
            </Form>
          </Card>

          {/* Footer Info */}
          <div className="mt-6 text-center" style={{ animation: 'fadeIn 0.6s ease-out 0.4s both' }}>
            <Text type="secondary" className="text-xs">
              默认账号: admin / admin123
            </Text>
            <div className="mt-2">
              <Text type="secondary" className="text-xs">
                {remember ? '\u2713 已启用记住我功能' : '未启用记住我功能'}
              </Text>
            </div>
          </div>
        </Col>
      </Row>
    </div>
  );
};

export default Login;
