import React, { useState, useEffect } from 'react';
import { Card, Form, Input, Select, Switch, Button, Row, Col, InputNumber, message, Tabs, Table, Tag, Space, Popconfirm, Descriptions, Alert, Divider, Badge } from 'antd';
import { SaveOutlined, ReloadOutlined, HistoryOutlined, UndoOutlined, SettingOutlined, VideoCameraOutlined, FileTextOutlined, ExperimentOutlined, SafetyOutlined, BellOutlined, CloudOutlined, RobotOutlined } from '@ant-design/icons';
import { systemConfigApi } from '../utils/api';
import PageContainer from '../components/PageContainer';
import { Typography } from 'antd';
const { Title, Text } = Typography;
const { Option } = Select;
const { TextArea } = Input;

// 配置分类
const CONFIG_CATEGORIES = {
  VIDEO: { key: 'VIDEO_QUALITY', label: '视频参数', icon: <VideoCameraOutlined /> },
  RECORDING: { key: 'RECORDING_SCHEDULE', label: '录像设置', icon: <FileTextOutlined /> },
  LOAD_BALANCE: { key: 'LOAD_BALANCING', label: '负载均衡', icon: <CloudOutlined /> },
  SECURITY: { key: 'SECURITY_POLICY', label: '安全策略', icon: <SafetyOutlined /> },
  NOTIFICATION: { key: 'ALERT_SETTINGS', label: '通知设置', icon: <BellOutlined /> },
  EDGE: { key: 'EDGE_NODES', label: '边缘节点', icon: <RobotOutlined /> },
};

const SystemSettings = () => {
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [configs, setConfigs] = useState({});
  const [configList, setConfigList] = useState([]);
  const [groups, setGroups] = useState([]);
  const [activeTab, setActiveTab] = useState('video');
  const [historyVisible, setHistoryVisible] = useState(false);
  const [historyData, setHistoryData] = useState([]);
  const [selectedConfigKey, setSelectedConfigKey] = useState(null);
  const [form] = Form.useForm();

  useEffect(() => {
    fetchConfigs();
    fetchGroups();
  }, []);

  const fetchConfigs = async () => {
    setLoading(true);
    try {
      const response = await systemConfigApi.getAllConfigs();
      const configData = response.data || [];
      setConfigList(configData);
      
      // 转换为key-value格式
      const configMap = {};
      configData.forEach(config => {
        configMap[config.configKey] = config.configValue;
      });
      setConfigs(configMap);
      form.setFieldsValue(configMap);
    } catch (error) {
      console.error('获取配置失败:', error);
      message.error('获取配置失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchGroups = async () => {
    try {
      const response = await systemConfigApi.getAllGroups();
      setGroups(response.data || []);
    } catch (error) {
      console.error('获取分组失败:', error);
    }
  };

  const fetchConfigHistory = async (configKey) => {
    try {
      const response = await systemConfigApi.getConfigHistory(configKey);
      setHistoryData(response.data || []);
      setSelectedConfigKey(configKey);
      setHistoryVisible(true);
    } catch (error) {
      console.error('获取配置历史失败:', error);
      message.error('获取配置历史失败');
    }
  };

  const handleSaveConfigs = async (groupConfigs) => {
    setSaving(true);
    try {
      const updateData = {};
      groupConfigs.forEach(key => {
        updateData[key] = configs[key];
      });
      await systemConfigApi.batchUpdate(updateData);
      message.success('保存成功');
      fetchConfigs();
    } catch (error) {
      console.error('保存失败:', error);
      message.error('保存失败: ' + (error.response?.data?.message || error.message));
    } finally {
      setSaving(false);
    }
  };

  const handleResetConfig = async (configKey) => {
    try {
      await systemConfigApi.resetConfig(configKey);
      message.success('重置成功');
      fetchConfigs();
    } catch (error) {
      console.error('重置失败:', error);
      message.error('重置失败');
    }
  };

  const handleRollback = async (configKey) => {
    try {
      await systemConfigApi.rollbackConfig(configKey);
      message.success('回滚成功');
      fetchConfigs();
      if (selectedConfigKey === configKey) {
        fetchConfigHistory(configKey);
      }
    } catch (error) {
      console.error('回滚失败:', error);
      message.error('回滚失败: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleConfigChange = (key, value) => {
    setConfigs(prev => ({ ...prev, [key]: value }));
  };

  // 按Tab分组获取配置
  const getConfigsByTab = (tab) => {
    const categoryMap = {
      'video': ['VIDEO_QUALITY', 'VIDEO_STREAMING'],
      'recording': ['RECORDING_SCHEDULE', 'RECORDING_STORAGE', 'RECORDING_RETENTION'],
      'loadbalance': ['LOAD_BALANCING', 'CDN_NODES'],
      'security': ['SECURITY_POLICY', 'AUTHENTICATION'],
      'notification': ['ALERT_SETTINGS', 'NOTIFICATION_CHANNELS'],
      'edge': ['EDGE_NODES', 'EDGE_FAILOVER'],
    };
    const categories = categoryMap[tab] || [];
    return configList.filter(c => categories.includes(c.category));
  };

  // 渲染配置项
  const renderConfigItem = (config) => {
    const value = configs[config.configKey] || config.defaultValue;
    
    switch (config.valueType) {
      case 'BOOLEAN':
        return (
          <Form.Item
            key={config.configKey}
            name={config.configKey}
            label={config.configName}
            valuePropName="checked"
            extra={config.description}
          >
            <Switch 
              checked={value === 'true'}
              onChange={(checked) => handleConfigChange(config.configKey, String(checked))}
            />
          </Form.Item>
        );
      case 'NUMBER':
        return (
          <Form.Item
            key={config.configKey}
            name={config.configKey}
            label={config.configName}
            extra={config.description}
          >
            <InputNumber
              style={{ width: '100%' }}
              min={config.minValue}
              max={config.maxValue}
              value={value ? Number(value) : undefined}
              onChange={(val) => handleConfigChange(config.configKey, val !== null ? String(val) : '')}
              addonAfter={config.configKey.includes('seconds') ? '秒' : config.configKey.includes('days') ? '天' : ''}
            />
          </Form.Item>
        );
      case 'SELECT':
        const options = config.options ? config.options.split(',').map(o => o.trim()) : [];
        return (
          <Form.Item
            key={config.configKey}
            name={config.configKey}
            label={config.configName}
            extra={config.description}
          >
            <Select
              value={value}
              onChange={(val) => handleConfigChange(config.configKey, val)}
              style={{ width: '100%' }}
            >
              {options.map(opt => (
                <Option key={opt} value={opt}>{opt}</Option>
              ))}
            </Select>
          </Form.Item>
        );
      default:
        const isSensitive = config.sensitive;
        return (
          <Form.Item
            key={config.configKey}
            name={config.configKey}
            label={config.configName}
            extra={config.description}
            tooltip={config.sensitive ? '敏感配置' : undefined}
          >
            {isSensitive ? (
              <Input.Password
                value={value}
                onChange={(e) => handleConfigChange(config.configKey, e.target.value)}
                placeholder={config.defaultValue || '请输入'}
              />
            ) : (
              <Input.TextArea
                value={value}
                onChange={(e) => handleConfigChange(config.configKey, e.target.value)}
                placeholder={config.defaultValue || '请输入'}
                autoSize={{ minRows: 1, maxRows: 3 }}
              />
            )}
          </Form.Item>
        );
    }
  };

  // 渲染Tab内容
  const renderTabContent = (tab) => {
    const tabConfigs = getConfigsByTab(tab);
    const configKeys = tabConfigs.map(c => c.configKey);
    
    return (
      <Card 
        title={CONFIG_CATEGORIES[tab]?.label || tab}
        extra={
          <Space>
            <Button 
              icon={<HistoryOutlined />} 
              onClick={() => configKeys.length > 0 && fetchConfigHistory(configKeys[0])}
            >
              历史
            </Button>
            <Button 
              type="primary" 
              icon={<SaveOutlined />} 
              loading={saving}
              onClick={() => handleSaveConfigs(configKeys)}
            >
              保存
            </Button>
          </Space>
        }
      >
        <Row gutter={[16, 8]}>
          {tabConfigs.map(config => (
            <Col key={config.configKey} xs={24} sm={12} md={8}>
              {renderConfigItem(config)}
            </Col>
          ))}
        </Row>
        {tabConfigs.length === 0 && (
          <Alert message="该分类暂无配置项" type="info" showIcon />
        )}
      </Card>
    );
  };

  // 历史记录表格列
  const historyColumns = [
    {
      title: '操作时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (text) => text ? new Date(text).toLocaleString() : '-',
    },
    {
      title: '操作类型',
      dataIndex: 'operationType',
      key: 'operationType',
      width: 100,
      render: (text) => {
        const colors = {
          'CREATE': 'green',
          'UPDATE': 'blue',
          'DELETE': 'red',
          'RESET': 'orange',
          'ROLLBACK': 'purple',
        };
        return <Tag color={colors[text] || 'default'}>{text}</Tag>;
      },
    },
    {
      title: '旧值',
      dataIndex: 'oldValue',
      key: 'oldValue',
      ellipsis: true,
      render: (text) => text || '-',
    },
    {
      title: '新值',
      dataIndex: 'newValue',
      key: 'newValue',
      ellipsis: true,
      render: (text) => text || '-',
    },
    {
      title: '操作者',
      dataIndex: 'operatorName',
      key: 'operatorName',
      width: 120,
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_, record) => (
        record.rollbackable && !record.rolledBack && record.operationType !== 'CREATE' && (
          <Popconfirm
            title="确定要回滚到该版本吗？"
            onConfirm={() => handleRollback(selectedConfigKey)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" size="small" icon={<UndoOutlined />}>
              回滚
            </Button>
          </Popconfirm>
        )
      ),
    },
  ];

  const tabItems = [
    {
      key: 'video',
      label: (
        <span>
          <VideoCameraOutlined />
          视频参数
        </span>
      ),
      children: renderTabContent('video'),
    },
    {
      key: 'recording',
      label: (
        <span>
          <FileTextOutlined />
          录像设置
        </span>
      ),
      children: renderTabContent('recording'),
    },
    {
      key: 'loadbalance',
      label: (
        <span>
          <CloudOutlined />
          负载均衡
        </span>
      ),
      children: renderTabContent('loadbalance'),
    },
    {
      key: 'security',
      label: (
        <span>
          <SafetyOutlined />
          安全策略
        </span>
      ),
      children: renderTabContent('security'),
    },
    {
      key: 'notification',
      label: (
        <span>
          <BellOutlined />
          通知设置
        </span>
      ),
      children: renderTabContent('notification'),
    },
    {
      key: 'edge',
      label: (
        <span>
          <RobotOutlined />
          边缘节点
        </span>
      ),
      children: renderTabContent('edge'),
    },
    {
      key: 'history',
      label: (
        <span>
          <HistoryOutlined />
          全部历史
        </span>
      ),
      children: (
        <Card title="配置变更历史">
          <Table
            columns={historyColumns}
            dataSource={historyData}
            rowKey="id"
            loading={loading}
            pagination={{ pageSize: 10 }}
          />
        </Card>
      ),
    },
  ];

  return (
    <PageContainer>
      <div style={{ marginBottom: 16 }}>
        <Title level={4} style={{ marginBottom: 8 }}>
          <SettingOutlined /> 系统设置
        </Title>
        <Text type="secondary">
          管理系统各项配置，配置变更会自动记录历史，支持回滚操作。
        </Text>
      </div>

      <Form form={form} component={false}>
        <Tabs 
          activeKey={activeTab} 
          onChange={setActiveTab}
          items={tabItems}
          tabPosition="top"
        />
      </Form>
    </PageContainer>
  );
};

export default SystemSettings;
