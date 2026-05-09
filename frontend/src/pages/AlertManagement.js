import React, { useState, useEffect, useCallback } from 'react';
import { 
  Table, Button, Space, Modal, Form, Input, Select, InputNumber, 
  Tag, Popconfirm, message, Card, Row, Col, Statistic, Tooltip, Divider, Checkbox, Tabs
} from 'antd';
import { 
  PlusOutlined, EditOutlined, DeleteOutlined, PlayCircleOutlined, 
  PauseCircleOutlined, SettingOutlined, AlertOutlined, BellOutlined, ExperimentOutlined 
} from '@ant-design/icons';
import { alertRuleApi } from '../utils/api';
import AlertConditionEditor from '../components/AlertConditionEditor';
import NotificationChannelConfig from '../components/NotificationChannelConfig';
import AlertRuleTemplateSelector from '../components/AlertRuleTemplateSelector';
import AlertRuleTestResult from '../components/AlertRuleTestResult';

const { Option } = Select;
const { TextArea } = Input;

/**
 * 告警规则管理页面
 */
const AlertManagement = () => {
  const [rules, setRules] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0
  });
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRule, setEditingRule] = useState(null);
  const [form] = Form.useForm();
  const [activeTab, setActiveTab] = useState('basic');
  const [testResult, setTestResult] = useState(null);
  const [testLoading, setTestLoading] = useState(false);

  // 告警类型选项
  const alertTypeOptions = [
    { value: 'CPU_USAGE', label: 'CPU使用率' },
    { value: 'MEMORY_USAGE', label: '内存使用率' },
    { value: 'DISK_USAGE', label: '磁盘使用率' },
    { value: 'NETWORK_LATENCY', label: '网络延迟' },
    { value: 'CAMERA_OFFLINE', label: '摄像头离线' },
    { value: 'CAMERA_ERROR', label: '摄像头错误' },
    { value: 'EDGE_NODE_OFFLINE', label: '边缘节点离线' },
    { value: 'STREAM_INTERRUPTED', label: '视频流中断' },
    { value: 'MOTION_DETECTED', label: '移动侦测' },
    { value: 'RECORDING_FAILED', label: '录像失败' },
    { value: 'SYSTEM_ERROR', label: '系统错误' },
    { value: 'CUSTOM', label: '自定义' }
  ];

  // 告警级别选项
  const alertLevelOptions = [
    { value: 'INFO', label: '信息', color: 'blue' },
    { value: 'WARNING', label: '警告', color: 'orange' },
    { value: 'ERROR', label: '错误', color: 'red' },
    { value: 'CRITICAL', label: '严重', color: 'purple' }
  ];

  // 目标类型选项
  const targetTypeOptions = [
    { value: 'SYSTEM', label: '系统级别' },
    { value: 'EDGE_NODE', label: '边缘节点' },
    { value: 'CAMERA', label: '摄像头' },
    { value: 'STREAM', label: '视频流' },
    { value: 'REGION', label: '区域' }
  ];

  // 通知方式选项
  const notificationMethodOptions = [
    { value: 'IN_APP', label: '应用内通知' },
    { value: 'EMAIL', label: '邮件通知' },
    { value: 'SMS', label: '短信通知' },
    { value: 'WEBHOOK', label: 'Webhook通知' },
    { value: 'DINGTALK', label: '钉钉通知' }
  ];

  // 加载规则列表
  const loadRules = useCallback(async () => {
    setLoading(true);
    try {
      const response = await alertRuleApi.list({
        page: pagination.current - 1,
        size: pagination.pageSize
      });
      const data = response.data;
      setRules(data.content || []);
      setPagination({
        ...pagination,
        total: data.totalElements || 0
      });
    } catch (error) {
      message.error('加载告警规则失败: ' + (error.message || '未知错误'));
    } finally {
      setLoading(false);
    }
  }, [pagination.current, pagination.pageSize]);

  useEffect(() => {
    loadRules();
  }, [loadRules]);

  // 表格分页变化
  const handleTableChange = (newPagination) => {
    setPagination(newPagination);
  };

  // 打开创建/编辑弹窗
  const openModal = (rule = null) => {
    setEditingRule(rule);
    if (rule) {
      form.setFieldsValue({
        ...rule,
        targetId: rule.targetId || undefined
      });
    } else {
      form.resetFields();
      form.setFieldsValue({
        enabled: true,
        durationSeconds: 300,
        cooldownSeconds: 600
      });
    }
    setModalVisible(true);
  };

  // 关闭弹窗
  const closeModal = () => {
    setModalVisible(false);
    setEditingRule(null);
    form.resetFields();
  };

  // 提交表单
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      
      if (editingRule) {
        await alertRuleApi.update(editingRule.id, values);
        message.success('告警规则更新成功');
      } else {
        await alertRuleApi.create(values);
        message.success('告警规则创建成功');
      }
      
      closeModal();
      loadRules();
    } catch (error) {
      if (error.errorFields) {
        return; // 表单验证错误
      }
      message.error(editingRule ? '更新失败: ' : '创建失败: ' + (error.message || '未知错误'));
    }
  };

  // 删除规则
  const handleDelete = async (id) => {
    try {
      await alertRuleApi.delete(id);
      message.success('告警规则删除成功');
      loadRules();
    } catch (error) {
      message.error('删除失败: ' + (error.message || '未知错误'));
    }
  };

  // 启用/禁用规则
  const handleToggleEnable = async (id, enabled) => {
    try {
      if (enabled) {
        await alertRuleApi.enable(id);
        message.success('告警规则已启用');
      } else {
        await alertRuleApi.disable(id);
        message.success('告警规则已禁用');
      }
      loadRules();
    } catch (error) {
      message.error('操作失败: ' + (error.message || '未知错误'));
    }
  };

  // 表格列定义
  const columns = [
    {
      title: '规则名称',
      dataIndex: 'name',
      key: 'name',
      render: (text, record) => (
        <Space>
          <span style={{ fontWeight: 'bold' }}>{text}</span>
          {!record.enabled && <Tag color="default">已禁用</Tag>}
        </Space>
      )
    },
    {
      title: '告警类型',
      dataIndex: 'alertType',
      key: 'alertType',
      render: (type) => {
        const option = alertTypeOptions.find(o => o.value === type);
        return option?.label || type;
      }
    },
    {
      title: '级别',
      dataIndex: 'level',
      key: 'level',
      render: (level) => {
        const option = alertLevelOptions.find(o => o.value === level);
        return <Tag color={option?.color}>{option?.label || level}</Tag>;
      }
    },
    {
      title: '目标类型',
      dataIndex: 'targetType',
      key: 'targetType',
      render: (type) => {
        const option = targetTypeOptions.find(o => o.value === type);
        return option?.label || type;
      }
    },
    {
      title: '警告阈值',
      dataIndex: 'warningThreshold',
      key: 'warningThreshold',
      render: (value) => value ? `${value}%` : '-'
    },
    {
      title: '严重阈值',
      dataIndex: 'criticalThreshold',
      key: 'criticalThreshold',
      render: (value) => value ? `${value}%` : '-'
    },
    {
      title: '冷却时间',
      dataIndex: 'cooldownSeconds',
      key: 'cooldownSeconds',
      render: (seconds) => seconds ? `${Math.floor(seconds / 60)}分钟` : '-'
    },
    {
      title: '通知方式',
      dataIndex: 'notificationMethod',
      key: 'notificationMethod',
      render: (method) => {
        const option = notificationMethodOptions.find(o => o.value === method);
        return option?.label || method;
      }
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="编辑">
            <Button 
              type="text" 
              icon={<EditOutlined />} 
              onClick={() => openModal(record)}
            />
          </Tooltip>
          <Tooltip title={record.enabled ? '禁用' : '启用'}>
            <Button 
              type="text" 
              icon={record.enabled ? <PauseCircleOutlined /> : <PlayCircleOutlined />} 
              onClick={() => handleToggleEnable(record.id, !record.enabled)}
            />
          </Tooltip>
          <Popconfirm
            title="确认删除"
            description="删除后无法恢复，确定要删除吗？"
            onConfirm={() => handleDelete(record.id)}
            okText="确认"
            cancelText="取消"
          >
            <Tooltip title="删除">
              <Button type="text" danger icon={<DeleteOutlined />} />
            </Tooltip>
          </Popconfirm>
        </Space>
      )
    }
  ];

  return (
    <div className="alert-management">
      <Card 
        title={
          <Space>
            <AlertOutlined />
            <span>告警规则管理</span>
          </Space>
        }
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>
            创建规则
          </Button>
        }
      >
        <Table
          columns={columns}
          dataSource={rules}
          rowKey="id"
          loading={loading}
          pagination={pagination}
          onChange={handleTableChange}
          size="middle"
        />
      </Card>

      {/* 创建/编辑弹窗 */}
      <Modal
        title={editingRule ? '编辑告警规则' : '创建告警规则'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={closeModal}
        width={800}
        okText={editingRule ? '保存' : '创建'}
        cancelText="取消"
      >
        <Tabs 
          activeKey={activeTab} 
          onChange={setActiveTab}
          items={[
            {
              key: 'basic',
              label: '基础配置',
              children: (
                <Form
                  form={form}
                  layout="vertical"
                  initialValues={{
                    enabled: true,
                    durationSeconds: 300,
                    cooldownSeconds: 600,
                    level: 'WARNING'
                  }}
                >
                  <Row gutter={[16, 16]}>
                    <Col span={12}>
                      <Form.Item
                        name="name"
                        label="规则名称"
                        rules={[{ required: true, message: '请输入规则名称' }]}
                      >
                        <Input placeholder="请输入规则名称" />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item
                        name="alertType"
                        label="告警类型"
                        rules={[{ required: true, message: '请选择告警类型' }]}
                      >
                        <Select placeholder="请选择">
                          {alertTypeOptions.map(option => (
                            <Option key={option.value} value={option.value}>{option.label}</Option>
                          ))}
                        </Select>
                      </Form.Item>
                    </Col>
                  </Row>

                  <Row gutter={[16, 16]}>
                    <Col span={12}>
                      <Form.Item
                        name="level"
                        label="告警级别"
                        rules={[{ required: true, message: '请选择告警级别' }]}
                      >
                        <Select placeholder="请选择">
                          {alertLevelOptions.map(option => (
                            <Option key={option.value} value={option.value}>{option.label}</Option>
                          ))}
                        </Select>
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item
                        name="targetType"
                        label="目标类型"
                        rules={[{ required: true, message: '请选择目标类型' }]}
                      >
                        <Select placeholder="请选择">
                          {targetTypeOptions.map(option => (
                            <Option key={option.value} value={option.value}>{option.label}</Option>
                          ))}
                        </Select>
                      </Form.Item>
                    </Col>
                  </Row>

                  <Row gutter={[16, 16]}>
                    <Col span={12}>
                      <Form.Item
                        name="warningThreshold"
                        label="警告阈值"
                      >
                        <InputNumber 
                          min={0} 
                          max={100} 
                          style={{ width: '100%' }} 
                          addonAfter="%"
                          placeholder="请输入"
                        />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item
                        name="criticalThreshold"
                        label="严重阈值"
                      >
                        <InputNumber 
                          min={0} 
                          max={100} 
                          style={{ width: '100%' }} 
                          addonAfter="%"
                          placeholder="请输入"
                        />
                      </Form.Item>
                    </Col>
                  </Row>

                  <Row gutter={[16, 16]}>
                    <Col span={12}>
                      <Form.Item
                        name="durationSeconds"
                        label="持续时间（秒）"
                        tooltip="指标超过阈值持续多长时间才触发告警"
                      >
                        <InputNumber min={1} max={3600} style={{ width: '100%' }} />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item
                        name="cooldownSeconds"
                        label="冷却时间（秒）"
                        tooltip="告警触发后多久内不重复告警"
                      >
                        <InputNumber min={0} max={86400} style={{ width: '100%' }} />
                      </Form.Item>
                    </Col>
                  </Row>

                  <Form.Item
                    name="description"
                    label="规则描述"
                  >
                    <TextArea rows={2} placeholder="请输入规则描述" />
                  </Form.Item>

                  <Form.Item
                    name="enabled"
                    valuePropName="checked"
                  >
                    <Checkbox>启用此规则</Checkbox>
                  </Form.Item>
                </Form>
              )
            },
            {
              key: 'conditions',
              label: '告警条件',
              children: (
                <Card title="告警条件配置" size="small">
                  <AlertConditionEditor 
                    conditions={form.getFieldValue('conditions') || []} 
                    onChange={(conditions) => form.setFieldValue('conditions', conditions)}
                  />
                </Card>
              )
            },
            {
              key: 'notifications',
              label: '通知渠道',
              children: (
                <Card title="通知渠道配置" size="small">
                  <NotificationChannelConfig
                    notifications={form.getFieldValue('notifications') || []}
                    onChange={(notifications) => form.setFieldValue('notifications', notifications)}
                  />
                </Card>
              )
            },
            {
              key: 'template',
              label: '模板',
              children: (
                <Card title="从模板创建" size="small">
                  <AlertRuleTemplateSelector
                    value={form.getFieldValue('templateId')}
                    onChange={(template) => {
                      if (template) {
                        form.setFieldsValue({
                          name: template.name,
                          alertType: template.alertType,
                          level: template.level,
                          targetType: template.targetType,
                          description: template.description,
                        });
                        setActiveTab('basic');
                      }
                    }}
                    alertType={form.getFieldValue('alertType')}
                  />
                </Card>
              )
            },
            {
              key: 'test',
              label: '规则测试',
              children: (
                <AlertRuleTestResult
                  ruleId={editingRule?.id}
                  testResult={testResult}
                  onTest={async () => {
                    if (!editingRule?.id) {
                      message.warning('请先保存规则后再测试');
                      return;
                    }
                    setTestLoading(true);
                    try {
                      const response = await alertRuleApi.test(editingRule.id);
                      setTestResult(response.data);
                    } catch (error) {
                      setTestResult({ success: false, message: error.message || '测试失败' });
                    } finally {
                      setTestLoading(false);
                    }
                  }}
                  loading={testLoading}
                />
              )
            }
          ]}
        />
      </Modal>
    </div>
  );
};

export default AlertManagement;
