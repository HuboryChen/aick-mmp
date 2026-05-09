import React, { useState, useEffect } from 'react';
import { Card, Button, Space, Select, Input, Switch, InputNumber, Form, Tag, Divider, message, Popconfirm } from 'antd';
import { PlusOutlined, DeleteOutlined, MailOutlined, MessageOutlined, BellOutlined, ApiOutlined } from '@ant-design/icons';

const { Option } = Select;
const { TextArea } = Input;

/**
 * 通知渠道配置组件
 */
const NotificationChannelConfig = ({ notifications = [], onChange, disabled = false }) => {
  const [localNotifications, setLocalNotifications] = useState(notifications);

  useEffect(() => {
    setLocalNotifications(notifications);
  }, [notifications]);

  // 渠道类型
  const channelTypes = [
    { value: 'IN_APP', label: '应用内通知', icon: <BellOutlined />, color: 'blue' },
    { value: 'EMAIL', label: '邮件通知', icon: <MailOutlined />, color: 'green' },
    { value: 'SMS', label: '短信通知', icon: <MessageOutlined />, color: 'orange' },
    { value: 'WEBHOOK', label: 'Webhook', icon: <ApiOutlined />, color: 'purple' },
    { value: 'DINGTALK', label: '钉钉', color: 'cyan' },
    { value: 'WECHAT', label: '企业微信', color: 'green' },
    { value: 'FEISHU', label: '飞书', color: 'blue' },
  ];

  // 优先级选项
  const priorityOptions = [
    { value: 'LOW', label: '低', color: 'default' },
    { value: 'NORMAL', label: '普通', color: 'blue' },
    { value: 'HIGH', label: '高', color: 'orange' },
    { value: 'URGENT', label: '紧急', color: 'red' },
  ];

  const addNotification = () => {
    const newNotification = {
      id: `temp_${Date.now()}`,
      channelType: 'IN_APP',
      target: '',
      titleTemplate: '[告警通知]',
      contentTemplate: '',
      priority: 'NORMAL',
      maxRetry: 3,
      retryInterval: 60,
      timeoutSeconds: 30,
      isEnabled: true,
      levelFilter: null,
      escalationEnabled: false,
      escalationDelayMinutes: 30,
    };
    const newNotifications = [...localNotifications, newNotification];
    setLocalNotifications(newNotifications);
    onChange?.(newNotifications);
  };

  const updateNotification = (index, field, value) => {
    const newNotifications = [...localNotifications];
    newNotifications[index] = { ...newNotifications[index], [field]: value };
    setLocalNotifications(newNotifications);
    onChange?.(newNotifications);
  };

  const removeNotification = (index) => {
    const newNotifications = localNotifications.filter((_, i) => i !== index);
    setLocalNotifications(newNotifications);
    onChange?.(newNotifications);
  };

  const getChannelIcon = (type) => {
    const channel = channelTypes.find(c => c.value === type);
    return channel?.icon || <BellOutlined />;
  };

  const getChannelColor = (type) => {
    const channel = channelTypes.find(c => c.value === type);
    return channel?.color || 'default';
  };

  const renderNotification = (notification, index) => {
    const isEmail = notification.channelType === 'EMAIL';
    const isWebhook = notification.channelType === 'WEBHOOK';
    const isSms = notification.channelType === 'SMS';

    return (
      <Card 
        key={notification.id || index} 
        size="small" 
        className="notification-card"
        title={
          <Space>
            <Tag icon={getChannelIcon(notification.channelType)} color={getChannelColor(notification.channelType)}>
              {channelTypes.find(c => c.value === notification.channelType)?.label}
            </Tag>
            <Switch 
              size="small" 
              checked={notification.isEnabled}
              onChange={(checked) => updateNotification(index, 'isEnabled', checked)}
              disabled={disabled}
            />
          </Space>
        }
        extra={
          !disabled && (
            <Popconfirm
              title="确定删除此通知配置？"
              onConfirm={() => removeNotification(index)}
              okText="确定"
              cancelText="取消"
            >
              <Button type="text" danger size="small" icon={<DeleteOutlined />} />
            </Popconfirm>
          )
        }
      >
        <div className="notification-form">
          {/* 通知目标 */}
          <Form.Item label="通知目标" className="mb-2">
            {isEmail && (
              <Input
                placeholder="example@domain.com，多个用逗号分隔"
                value={notification.target}
                onChange={(e) => updateNotification(index, 'target', e.target.value)}
                disabled={disabled}
                prefix={<MailOutlined />}
              />
            )}
            {isSms && (
              <Input
                placeholder="手机号，多个用逗号分隔"
                value={notification.target}
                onChange={(e) => updateNotification(index, 'target', e.target.value)}
                disabled={disabled}
                prefix={<MessageOutlined />}
              />
            )}
            {isWebhook && (
              <Input
                placeholder="https://your-webhook-url.com/notify"
                value={notification.target}
                onChange={(e) => updateNotification(index, 'target', e.target.value)}
                disabled={disabled}
                prefix={<ApiOutlined />}
              />
            )}
            {(!isEmail && !isSms && !isWebhook) && (
              <Input
                placeholder="通知目标"
                value={notification.target}
                onChange={(e) => updateNotification(index, 'target', e.target.value)}
                disabled={disabled}
              />
            )}
          </Form.Item>

          <Space size="middle" wrap>
            {/* 优先级 */}
            <Form.Item label="优先级" className="mb-2">
              <Select
                value={notification.priority}
                onChange={(val) => updateNotification(index, 'priority', val)}
                disabled={disabled}
                style={{ width: 100 }}
              >
                {priorityOptions.map(opt => (
                  <Option key={opt.value} value={opt.value}>
                    <Tag color={opt.color}>{opt.label}</Tag>
                  </Option>
                ))}
              </Select>
            </Form.Item>

            {/* 最大重试 */}
            <Form.Item label="最大重试" className="mb-2">
              <InputNumber
                value={notification.maxRetry}
                onChange={(val) => updateNotification(index, 'maxRetry', val)}
                disabled={disabled}
                min={0}
                max={10}
                style={{ width: 80 }}
              />
            </Form.Item>

            {/* 重试间隔 */}
            <Form.Item label="重试间隔" className="mb-2">
              <InputNumber
                value={notification.retryInterval}
                onChange={(val) => updateNotification(index, 'retryInterval', val)}
                disabled={disabled}
                min={10}
                max={3600}
                addonAfter="秒"
                style={{ width: 120 }}
              />
            </Form.Item>

            {/* 超时时间 */}
            <Form.Item label="超时时间" className="mb-2">
              <InputNumber
                value={notification.timeoutSeconds}
                onChange={(val) => updateNotification(index, 'timeoutSeconds', val)}
                disabled={disabled}
                min={5}
                max={300}
                addonAfter="秒"
                style={{ width: 120 }}
              />
            </Form.Item>
          </Space>

          <Divider className="my-2" />

          {/* 升级配置 */}
          <Space align="center">
            <span>启用升级：</span>
            <Switch
              checked={notification.escalationEnabled}
              onChange={(checked) => updateNotification(index, 'escalationEnabled', checked)}
              disabled={disabled}
            />
            {notification.escalationEnabled && (
              <InputNumber
                value={notification.escalationDelayMinutes}
                onChange={(val) => updateNotification(index, 'escalationDelayMinutes', val)}
                disabled={disabled}
                min={1}
                max={1440}
                addonAfter="分钟后升级"
                style={{ width: 180 }}
              />
            )}
          </Space>
        </div>
      </Card>
    );
  };

  return (
    <div className="notification-channel-config">
      <div className="notification-list">
        {localNotifications.length === 0 ? (
          <div className="empty-placeholder">
            <BellOutlined style={{ fontSize: 32, color: '#ccc' }} />
            <p>暂无通知渠道配置</p>
          </div>
        ) : (
          localNotifications.map((notification, index) => renderNotification(notification, index))
        )}
      </div>

      {!disabled && (
        <Button
          type="dashed"
          icon={<PlusOutlined />}
          onClick={addNotification}
          style={{ marginTop: 16, width: '100%' }}
        >
          添加通知渠道
        </Button>
      )}

      <style>{`
        .notification-channel-config .notification-card {
          margin-bottom: 12px;
        }
        .notification-channel-config .notification-form .ant-form-item {
          margin-bottom: 8px;
        }
        .notification-channel-config .notification-form .ant-form-item-label {
          padding-bottom: 4px;
        }
        .notification-channel-config .notification-form .ant-form-item-label > label {
          font-size: 12px;
          color: #666;
        }
        .notification-channel-config .mb-2 {
          margin-bottom: 8px;
        }
        .notification-channel-config .my-2 {
          margin: 12px 0;
        }
        .notification-channel-config .empty-placeholder {
          text-align: center;
          padding: 24px;
          color: #999;
        }
        .notification-channel-config .empty-placeholder p {
          margin-top: 8px;
        }
      `}</style>
    </div>
  );
};

export default NotificationChannelConfig;
