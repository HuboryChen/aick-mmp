import React, { useState } from 'react';
import { Card, Typography, Form, Input, Select, Switch, Button, Row, Col, InputNumber, message } from 'antd';
import { SaveOutlined } from '@ant-design/icons';

const { Title } = Typography;
const { Option } = Select;
const { TextArea } = Input;

const SystemSettings = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const initialValues = {
    systemName: '多地区视频监控系统',
    maxConcurrentStreams: 50,
    defaultResolution: '1280x720',
    defaultFrameRate: 25,
    recordingEnabled: true,
    recordingDuration: 30,
    alertsEnabled: true,
    emailNotifications: true,
    smsNotifications: false,
    adminEmail: 'admin@example.com',
    smtpServer: 'smtp.example.com',
    smtpPort: 587,
    smtpUsername: 'notification@example.com',
    smtpPassword: '',
    rtspTimeout: 30,
    webrtcStunServer: 'stun:stun.l.google.com:19302',
    logLevel: 'INFO',
    logRetentionDays: 30
  };

  const handleSubmit = async (values) => {
    setLoading(true);
    try {
      // 模拟保存设置
      await new Promise(resolve => setTimeout(resolve, 1000));
      message.success('设置保存成功');
    } catch (error) {
      message.error('保存失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <Title level={2}>系统设置</Title>
      
      <Form
        form={form}
        layout="vertical"
        initialValues={initialValues}
        onFinish={handleSubmit}
      >
        <Row gutter={24}>
          <Col span={24}>
            {/* 基础设置 */}
            <Card title="基础设置" style={{ marginBottom: '24px' }}>
              <Row gutter={16}>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="systemName"
                    label="系统名称"
                    rules={[{ required: true, message: '请输入系统名称' }]}
                  >
                    <Input placeholder="请输入系统名称" />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="maxConcurrentStreams"
                    label="最大并发流数"
                    rules={[{ required: true, message: '请输入最大并发流数' }]}
                  >
                    <InputNumber
                      min={1}
                      max={1000}
                      style={{ width: '100%' }}
                      placeholder="请输入最大并发流数"
                    />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="defaultResolution"
                    label="默认分辨率"
                    rules={[{ required: true, message: '请选择默认分辨率' }]}
                  >
                    <Select placeholder="请选择默认分辨率">
                      <Option value="640x480">640x480</Option>
                      <Option value="1280x720">1280x720</Option>
                      <Option value="1920x1080">1920x1080</Option>
                    </Select>
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="defaultFrameRate"
                    label="默认帧率"
                    rules={[{ required: true, message: '请输入默认帧率' }]}
                  >
                    <InputNumber
                      min={1}
                      max={60}
                      style={{ width: '100%' }}
                      placeholder="请输入默认帧率"
                      addonAfter="fps"
                    />
                  </Form.Item>
                </Col>
              </Row>
            </Card>

            {/* 录像设置 */}
            <Card title="录像设置" style={{ marginBottom: '24px' }}>
              <Row gutter={16}>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="recordingEnabled"
                    label="启用录像"
                    valuePropName="checked"
                  >
                    <Switch />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="recordingDuration"
                    label="录像保存天数"
                    rules={[{ required: true, message: '请输入录像保存天数' }]}
                  >
                    <InputNumber
                      min={1}
                      max={365}
                      style={{ width: '100%' }}
                      placeholder="请输入录像保存天数"
                      addonAfter="天"
                    />
                  </Form.Item>
                </Col>
              </Row>
            </Card>

            {/* 通知设置 */}
            <Card title="通知设置" style={{ marginBottom: '24px' }}>
              <Row gutter={16}>
                <Col xs={24} sm={8}>
                  <Form.Item
                    name="alertsEnabled"
                    label="启用告警"
                    valuePropName="checked"
                  >
                    <Switch />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={8}>
                  <Form.Item
                    name="emailNotifications"
                    label="邮件通知"
                    valuePropName="checked"
                  >
                    <Switch />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={8}>
                  <Form.Item
                    name="smsNotifications"
                    label="短信通知"
                    valuePropName="checked"
                  >
                    <Switch />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="adminEmail"
                    label="管理员邮箱"
                    rules={[
                      { required: true, message: '请输入管理员邮箱' },
                      { type: 'email', message: '请输入有效的邮箱地址' }
                    ]}
                  >
                    <Input placeholder="请输入管理员邮箱" />
                  </Form.Item>
                </Col>
              </Row>
            </Card>

            {/* SMTP设置 */}
            <Card title="SMTP设置" style={{ marginBottom: '24px' }}>
              <Row gutter={16}>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="smtpServer"
                    label="SMTP服务器"
                  >
                    <Input placeholder="请输入SMTP服务器地址" />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="smtpPort"
                    label="SMTP端口"
                  >
                    <InputNumber
                      min={1}
                      max={65535}
                      style={{ width: '100%' }}
                      placeholder="请输入SMTP端口"
                    />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="smtpUsername"
                    label="SMTP用户名"
                  >
                    <Input placeholder="请输入SMTP用户名" />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="smtpPassword"
                    label="SMTP密码"
                  >
                    <Input.Password placeholder="请输入SMTP密码" />
                  </Form.Item>
                </Col>
              </Row>
            </Card>

            {/* 高级设置 */}
            <Card title="高级设置" style={{ marginBottom: '24px' }}>
              <Row gutter={16}>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="rtspTimeout"
                    label="RTSP连接超时"
                  >
                    <InputNumber
                      min={5}
                      max={300}
                      style={{ width: '100%' }}
                      placeholder="请输入RTSP连接超时时间"
                      addonAfter="秒"
                    />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="webrtcStunServer"
                    label="WebRTC STUN服务器"
                  >
                    <Input placeholder="请输入STUN服务器地址" />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="logLevel"
                    label="日志级别"
                  >
                    <Select placeholder="请选择日志级别">
                      <Option value="TRACE">TRACE</Option>
                      <Option value="DEBUG">DEBUG</Option>
                      <Option value="INFO">INFO</Option>
                      <Option value="WARN">WARN</Option>
                      <Option value="ERROR">ERROR</Option>
                    </Select>
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item
                    name="logRetentionDays"
                    label="日志保留天数"
                  >
                    <InputNumber
                      min={1}
                      max={365}
                      style={{ width: '100%' }}
                      placeholder="请输入日志保留天数"
                      addonAfter="天"
                    />
                  </Form.Item>
                </Col>
              </Row>
            </Card>

            <div style={{ textAlign: 'center' }}>
              <Button
                type="primary"
                icon={<SaveOutlined />}
                size="large"
                loading={loading}
                htmlType="submit"
              >
                保存设置
              </Button>
            </div>
          </Col>
        </Row>
      </Form>
    </div>
  );
};

export default SystemSettings;