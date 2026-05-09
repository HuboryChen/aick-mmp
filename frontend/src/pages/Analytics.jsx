import React, { useState, useEffect, useCallback } from 'react';
import {
  Row, Col, Card, Tabs, DatePicker, Select, Button, Statistic,
  Progress, Table, Tag, Space, message, Spin, Empty, Modal, Form, Input, Tooltip
} from 'antd';
import {
  LineChartOutlined, BarChartOutlined, PieChartOutlined,
  DesktopOutlined, CloudOutlined, SaveOutlined, DownloadOutlined,
  ReloadOutlined, DeleteOutlined, EditOutlined, EyeOutlined,
  SafetyCertificateOutlined, ThunderboltOutlined, DatabaseOutlined,
  WarningOutlined, CheckCircleOutlined, ClockCircleOutlined,
  PlusOutlined, VideoCameraOutlined
} from '@ant-design/icons';
import { analyticsApi } from '../utils/api';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;
const { Option } = Select;
const { TabPane } = Tabs;
const { TextArea } = Input;

// 格式化数字
const formatNumber = (num) => {
  if (num == null) return '0';
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
};

// 格式化流量单位
const formatBandwidth = (mbps) => {
  if (mbps == null) return '0 Mbps';
  if (mbps >= 1000) return `${(mbps / 1000).toFixed(2)} Gbps`;
  return `${mbps.toFixed(2)} Mbps`;
};

// 格式化存储大小
const formatStorage = (bytes) => {
  if (bytes == null) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let unitIndex = 0;
  let value = bytes;
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex++;
  }
  return `${value.toFixed(2)} ${units[unitIndex]}`;
};

// 状态指示器组件
const StatusBadge = ({ status, text }) => {
  const statusConfig = {
    online: { color: '#00ff88', bg: 'rgba(0, 255, 136, 0.1)' },
    offline: { color: '#ff4757', bg: 'rgba(255, 71, 87, 0.1)' },
    warning: { color: '#fbbf24', bg: 'rgba(251, 191, 36, 0.1)' },
    error: { color: '#ff4757', bg: 'rgba(255, 71, 87, 0.1)' },
  };
  const config = statusConfig[status] || statusConfig.warning;
  
  return (
    <Tag
      style={{
        background: config.bg,
        borderColor: `${config.color}40`,
        color: config.color,
      }}
    >
      {text || status}
    </Tag>
  );
};

// 统计卡片组件
const StatsCard = ({ title, value, suffix, icon, trend, trendValue, loading }) => {
  return (
    <Card
      loading={loading}
      className="rounded-xl border overflow-hidden"
      style={{
        background: 'var(--color-bg-card)',
        borderColor: 'var(--color-border)',
      }}
      styles={{ body: { padding: 20 } }}
    >
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <div
            className="text-[13px] tracking-wide mb-2"
            style={{ color: 'var(--color-text-secondary)' }}
          >
            {title}
          </div>
          <div
            className="text-2xl font-bold leading-none"
            style={{ color: 'var(--color-text-primary)', fontFamily: "'JetBrains Mono', monospace" }}
          >
            {formatNumber(value)}
            <span className="ml-1 text-sm font-normal" style={{ color: 'var(--color-text-secondary)' }}>
              {suffix}
            </span>
          </div>
          {trend && (
            <div className="mt-2 flex items-center gap-1 text-xs">
              {trend === 'up' ? (
                <span style={{ color: '#00ff88' }}>↑ {trendValue}</span>
              ) : (
                <span style={{ color: '#ff4757' }}>↓ {trendValue}</span>
              )}
            </div>
          )}
        </div>
        <div
          className="flex items-center justify-center rounded-xl border"
          style={{
            background: 'var(--color-accent-muted)',
            borderColor: 'rgba(0, 212, 255, 0.2)',
            width: 48,
            height: 48,
          }}
        >
          <span style={{ fontSize: 22, color: 'var(--color-accent)' }}>{icon}</span>
        </div>
      </div>
    </Card>
  );
};

// 设备使用统计面板
const DeviceUsagePanel = ({ data, loading }) => {
  const stats = data?.stats || {};
  const trend = data?.usageTrend || [];
  
  return (
    <div>
      <Row gutter={[{ xs: 8, sm: 12, lg: 16 }, { xs: 8, sm: 12, lg: 16 }]} className="mb-4">
        <Col xs={12} sm={6}>
          <StatsCard
            title="设备总数"
            value={stats.total || 0}
            icon={<DesktopOutlined />}
            loading={loading}
          />
        </Col>
        <Col xs={12} sm={6}>
          <StatsCard
            title="在线设备"
            value={stats.online || 0}
            icon={<CheckCircleOutlined />}
            loading={loading}
          />
        </Col>
        <Col xs={12} sm={6}>
          <StatsCard
            title="故障设备"
            value={stats.abnormal || 0}
            icon={<WarningOutlined />}
            loading={loading}
          />
        </Col>
        <Col xs={12} sm={6}>
          <StatsCard
            title="维护中"
            value={stats.maintenance || 0}
            icon={<ClockCircleOutlined />}
            loading={loading}
          />
        </Col>
      </Row>
      
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card
            title="设备状态分布"
            className="rounded-xl border"
            style={{
              background: 'var(--color-bg-card)',
              borderColor: 'var(--color-border)',
            }}
            styles={{ body: { padding: 16 } }}
          >
            <div className="space-y-4">
              <div>
                <div className="flex justify-between mb-1">
                  <span style={{ color: 'var(--color-text-secondary)' }}>在线率</span>
                  <span style={{ color: '#00ff88' }}>{((stats.online || 0) / (stats.total || 1) * 100).toFixed(1)}%</span>
                </div>
                <Progress
                  percent={((stats.online || 0) / (stats.total || 1) * 100)}
                  strokeColor="#00ff88"
                  trailColor="var(--color-bg-secondary)"
                  showInfo={false}
                />
              </div>
              <div>
                <div className="flex justify-between mb-1">
                  <span style={{ color: 'var(--color-text-secondary)' }}>故障率</span>
                  <span style={{ color: '#ff4757' }}>{((stats.abnormal || 0) / (stats.total || 1) * 100).toFixed(1)}%</span>
                </div>
                <Progress
                  percent={((stats.abnormal || 0) / (stats.total || 1) * 100)}
                  strokeColor="#ff4757"
                  trailColor="var(--color-bg-secondary)"
                  showInfo={false}
                />
              </div>
              <div>
                <div className="flex justify-between mb-1">
                  <span style={{ color: 'var(--color-text-secondary)' }}>维护率</span>
                  <span style={{ color: '#fbbf24' }}>{((stats.maintenance || 0) / (stats.total || 1) * 100).toFixed(1)}%</span>
                </div>
                <Progress
                  percent={((stats.maintenance || 0) / (stats.total || 1) * 100)}
                  strokeColor="#fbbf24"
                  trailColor="var(--color-bg-secondary)"
                  showInfo={false}
                />
              </div>
            </div>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card
            title="可靠性指标"
            className="rounded-xl border"
            style={{
              background: 'var(--color-bg-card)',
              borderColor: 'var(--color-border)',
            }}
            styles={{ body: { padding: 16 } }}
          >
            <Row gutter={16}>
              <Col span={12}>
                <Statistic
                  title={<span style={{ color: 'var(--color-text-secondary)' }}>平均无故障时间 (MTBF)</span>}
                  value={stats.mtbfHours || 0}
                  suffix="小时"
                  valueStyle={{ color: '#00ff88', fontFamily: "'JetBrains Mono', monospace" }}
                />
              </Col>
              <Col span={12}>
                <Statistic
                  title={<span style={{ color: 'var(--color-text-secondary)' }}>平均修复时间 (MTTR)</span>}
                  value={stats.mttrMinutes || 0}
                  suffix="分钟"
                  valueStyle={{ color: '#fbbf24', fontFamily: "'JetBrains Mono', monospace" }}
                />
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

// 带宽统计面板
const BandwidthPanel = ({ data, loading }) => {
  const stats = data?.stats || {};
  
  return (
    <div>
      <Row gutter={[16, 16]} className="mb-4">
        <Col xs={12} sm={6}>
          <StatsCard
            title="当前带宽"
            value={stats.currentBandwidth || 0}
            suffix="Mbps"
            icon={<CloudOutlined />}
            loading={loading}
          />
        </Col>
        <Col xs={12} sm={6}>
          <StatsCard
            title="平均带宽"
            value={stats.averageBandwidth || 0}
            suffix="Mbps"
            icon={<LineChartOutlined />}
            loading={loading}
          />
        </Col>
        <Col xs={12} sm={6}>
          <StatsCard
            title="峰值带宽"
            value={stats.peakBandwidth || 0}
            suffix="Mbps"
            icon={<ThunderboltOutlined />}
            loading={loading}
          />
        </Col>
        <Col xs={12} sm={6}>
          <StatsCard
            title="总流量"
            value={stats.totalTrafficMB || 0}
            suffix="MB"
            icon={<DatabaseOutlined />}
            loading={loading}
          />
        </Col>
      </Row>
      
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card
            title="带宽使用趋势"
            className="rounded-xl border"
            style={{
              background: 'var(--color-bg-card)',
              borderColor: 'var(--color-border)',
            }}
            styles={{ body: { padding: 16 } }}
          >
            <div className="text-center py-8" style={{ color: 'var(--color-text-secondary)' }}>
              <LineChartOutlined style={{ fontSize: 48, marginBottom: 8 }} />
              <div>趋势图表</div>
              <div className="text-xs mt-2">数据点: {data?.bandwidthTrend?.length || 0}</div>
            </div>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card
            title="带宽分布"
            className="rounded-xl border"
            style={{
              background: 'var(--color-bg-card)',
              borderColor: 'var(--color-border)',
            }}
            styles={{ body: { padding: 16 } }}
          >
            <div className="space-y-4">
              <div>
                <div className="flex justify-between mb-1">
                  <span style={{ color: 'var(--color-text-secondary)' }}>使用率</span>
                  <span style={{ color: 'var(--color-accent)' }}>{((stats.currentBandwidth || 0) / (stats.peakBandwidth || 1) * 100).toFixed(1)}%</span>
                </div>
                <Progress
                  percent={((stats.currentBandwidth || 0) / (stats.peakBandwidth || 1) * 100)}
                  strokeColor="var(--color-accent)"
                  trailColor="var(--color-bg-secondary)"
                  showInfo={false}
                />
              </div>
              <div className="flex justify-between">
                <span style={{ color: 'var(--color-text-secondary)' }}>网络延迟</span>
                <span style={{ color: '#00ff88' }}>{stats.averageLatency || 0} ms</span>
              </div>
              <div className="flex justify-between">
                <span style={{ color: 'var(--color-text-secondary)' }}>丢包率</span>
                <span style={{ color: '#fbbf24' }}>{(stats.packetLossRate || 0).toFixed(2)}%</span>
              </div>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

// 存储统计面板
const StoragePanel = ({ data, loading }) => {
  const stats = data?.stats || {};
  
  return (
    <div>
      <Row gutter={[16, 16]} className="mb-4">
        <Col xs={12} sm={6}>
          <StatsCard
            title="总存储容量"
            value={formatStorage(stats.totalCapacityBytes || 0)}
            suffix=""
            icon={<DatabaseOutlined />}
            loading={loading}
          />
        </Col>
        <Col xs={12} sm={6}>
          <StatsCard
            title="已使用"
            value={formatStorage(stats.usedBytes || 0)}
            suffix=""
            icon={<SaveOutlined />}
            loading={loading}
          />
        </Col>
        <Col xs={12} sm={6}>
          <StatsCard
            title="录像总数"
            value={stats.totalRecordings || 0}
            suffix="个"
            icon={<VideoCameraOutlined />}
            loading={loading}
          />
        </Col>
        <Col xs={12} sm={6}>
          <StatsCard
            title="平均录像时长"
            value={stats.averageRecordingDuration || 0}
            suffix="秒"
            icon={<ClockCircleOutlined />}
            loading={loading}
          />
        </Col>
      </Row>
      
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card
            title="存储使用情况"
            className="rounded-xl border"
            style={{
              background: 'var(--color-bg-card)',
              borderColor: 'var(--color-border)',
            }}
            styles={{ body: { padding: 16 } }}
          >
            <Progress
              type="circle"
              percent={((stats.usedBytes || 0) / (stats.totalCapacityBytes || 1) * 100)}
              strokeColor={{
                '0%': '#00d4ff',
                '100%': '#00ff88',
              }}
              format={(percent) => (
                <span style={{ color: 'var(--color-text-primary)' }}>
                  {percent.toFixed(1)}%
                </span>
              )}
            />
            <div className="mt-4 flex justify-between">
              <span style={{ color: 'var(--color-text-secondary)' }}>可用空间</span>
              <span style={{ color: '#00ff88' }}>
                {formatStorage((stats.totalCapacityBytes || 0) - (stats.usedBytes || 0))}
              </span>
            </div>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card
            title="录像统计"
            className="rounded-xl border"
            style={{
              background: 'var(--color-bg-card)',
              borderColor: 'var(--color-border)',
            }}
            styles={{ body: { padding: 16 } }}
          >
            <div className="space-y-4">
              <div className="flex justify-between">
                <span style={{ color: 'var(--color-text-secondary)' }}>录像总时长</span>
                <span>{stats.totalRecordingDuration || 0} 秒</span>
              </div>
              <div className="flex justify-between">
                <span style={{ color: 'var(--color-text-secondary)' }}>存储增长率</span>
                <span style={{ color: '#fbbf24' }}>+{stats.storageGrowthRate || 0}%</span>
              </div>
              <div className="flex justify-between">
                <span style={{ color: 'var(--color-text-secondary)' }}>预计可用天数</span>
                <span>{stats.estimatedDaysRemaining || 0} 天</span>
              </div>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

// 告警统计面板
const AlertPanel = ({ data, loading }) => {
  const stats = data?.stats || {};
  const byLevel = data?.byLevel || {};
  const byType = data?.byType || [];
  
  const levelConfig = {
    CRITICAL: { color: '#ff4757', label: '严重' },
    HIGH: { color: '#ff6b35', label: '高' },
    MEDIUM: { color: '#fbbf24', label: '中' },
    LOW: { color: '#00d4ff', label: '低' },
  };
  
  return (
    <div>
      <Row gutter={[16, 16]} className="mb-4">
        <Col xs={12} sm={6}>
          <StatsCard
            title="告警总数"
            value={stats.total || 0}
            icon={<WarningOutlined />}
            loading={loading}
          />
        </Col>
        <Col xs={12} sm={6}>
          <StatsCard
            title="未处理"
            value={stats.unresolved || 0}
            icon={<SafetyCertificateOutlined />}
            loading={loading}
          />
        </Col>
        <Col xs={12} sm={6}>
          <StatsCard
            title="已处理"
            value={stats.resolved || 0}
            icon={<CheckCircleOutlined />}
            loading={loading}
          />
        </Col>
        <Col xs={12} sm={6}>
          <StatsCard
            title="处理率"
            value={((stats.resolved || 0) / (stats.total || 1) * 100).toFixed(1)}
            suffix="%"
            icon={<PieChartOutlined />}
            loading={loading}
          />
        </Col>
      </Row>
      
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card
            title="告警级别分布"
            className="rounded-xl border"
            style={{
              background: 'var(--color-bg-card)',
              borderColor: 'var(--color-border)',
            }}
            styles={{ body: { padding: 16 } }}
          >
            <div className="space-y-3">
              {Object.entries(levelConfig).map(([level, config]) => (
                <div key={level}>
                  <div className="flex justify-between mb-1">
                    <span style={{ color: config.color }}>{config.label}</span>
                    <span>{byLevel[level] || 0}</span>
                  </div>
                  <Progress
                    percent={(byLevel[level] || 0) / (stats.total || 1) * 100}
                    strokeColor={config.color}
                    trailColor="var(--color-bg-secondary)"
                    showInfo={false}
                    size="small"
                  />
                </div>
              ))}
            </div>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card
            title="告警类型分布"
            className="rounded-xl border"
            style={{
              background: 'var(--color-bg-card)',
              borderColor: 'var(--color-border)',
            }}
            styles={{ body: { padding: 16 } }}
          >
            {byType.length > 0 ? (
              <div className="space-y-2">
                {byType.slice(0, 5).map((item, index) => (
                  <div key={index} className="flex justify-between items-center">
                    <span style={{ color: 'var(--color-text-secondary)' }}>{item.type}</span>
                    <Tag color="blue">{item.count}</Tag>
                  </div>
                ))}
              </div>
            ) : (
              <Empty description="暂无数据" />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
};

// 报表订阅管理组件
const SubscriptionManager = ({ subscriptions, loading, onRefresh }) => {
  const [modalVisible, setModalVisible] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [form] = Form.useForm();
  
  const handleCreate = () => {
    setEditingItem(null);
    form.resetFields();
    setModalVisible(true);
  };
  
  const handleEdit = (record) => {
    setEditingItem(record);
    form.setFieldsValue(record);
    setModalVisible(true);
  };
  
  const handleDelete = async (id) => {
    try {
      await analyticsApi.deleteSubscription(id);
      message.success('删除成功');
      onRefresh();
    } catch (error) {
      message.error('删除失败');
    }
  };
  
  const handleToggle = async (id, enabled) => {
    try {
      await analyticsApi.toggleSubscription(id, !enabled);
      message.success(enabled ? '已禁用' : '已启用');
      onRefresh();
    } catch (error) {
      message.error('操作失败');
    }
  };
  
  const handleExport = async (id) => {
    try {
      const response = await analyticsApi.triggerReport(id);
      const blob = new Blob([response.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `report_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`;
      link.click();
      window.URL.revokeObjectURL(url);
      message.success('导出成功');
    } catch (error) {
      message.error('导出失败');
    }
  };
  
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (editingItem) {
        await analyticsApi.updateSubscription(editingItem.id, values);
        message.success('更新成功');
      } else {
        await analyticsApi.createSubscription(values);
        message.success('创建成功');
      }
      setModalVisible(false);
      onRefresh();
    } catch (error) {
      message.error('操作失败');
    }
  };
  
  const columns = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    {
      title: '类型',
      dataIndex: 'reportType',
      key: 'reportType',
      render: (type) => <Tag color="blue">{type}</Tag>,
    },
    {
      title: '周期',
      dataIndex: 'schedule',
      key: 'schedule',
      render: (schedule) => {
        const config = {
          DAILY: { color: 'green', text: '每日' },
          WEEKLY: { color: 'orange', text: '每周' },
          MONTHLY: { color: 'purple', text: '每月' },
        };
        const c = config[schedule] || { color: 'default', text: schedule };
        return <Tag color={c.color}>{c.text}</Tag>;
      },
    },
    { title: '通知邮箱', dataIndex: 'email', key: 'email' },
    {
      title: '状态',
      dataIndex: 'isActive',
      key: 'isActive',
      render: (active, record) => (
        <StatusBadge
          status={active ? 'online' : 'offline'}
          text={active ? '启用' : '禁用'}
        />
      ),
    },
    {
      title: '下次发送',
      dataIndex: 'nextSendTime',
      key: 'nextSendTime',
      render: (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Tooltip title="导出">
            <Button
              type="text"
              size="small"
              icon={<DownloadOutlined />}
              onClick={() => handleExport(record.id)}
            />
          </Tooltip>
          <Tooltip title={record.isActive ? '禁用' : '启用'}>
            <Button
              type="text"
              size="small"
              icon={<SafetyCertificateOutlined />}
              onClick={() => handleToggle(record.id, record.isActive)}
            />
          </Tooltip>
          <Tooltip title="编辑">
            <Button
              type="text"
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleEdit(record)}
            />
          </Tooltip>
          <Tooltip title="删除">
            <Button
              type="text"
              size="small"
              danger
              icon={<DeleteOutlined />}
              onClick={() => handleDelete(record.id)}
            />
          </Tooltip>
        </Space>
      ),
    },
  ];
  
  return (
    <Card
      title="报表订阅管理"
      className="rounded-xl border"
      style={{
        background: 'var(--color-bg-card)',
        borderColor: 'var(--color-border)',
      }}
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新建订阅
        </Button>
      }
    >
      <Table
        columns={columns}
        dataSource={subscriptions}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 10 }}
      />
      
      <Modal
        title={editingItem ? '编辑订阅' : '新建订阅'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="订阅名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="reportType" label="报表类型" rules={[{ required: true }]}>
            <Select>
              <Option value="DEVICE_STATUS">设备状态</Option>
              <Option value="BANDWIDTH">带宽</Option>
              <Option value="STORAGE">存储</Option>
              <Option value="ALERT">告警</Option>
              <Option value="COMPREHENSIVE">综合</Option>
            </Select>
          </Form.Item>
          <Form.Item name="schedule" label="发送周期" rules={[{ required: true }]}>
            <Select>
              <Option value="DAILY">每日</Option>
              <Option value="WEEKLY">每周</Option>
              <Option value="MONTHLY">每月</Option>
            </Select>
          </Form.Item>
          <Form.Item name="email" label="通知邮箱" rules={[{ required: true, type: 'email' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="format" label="报表格式" initialValue="EXCEL">
            <Select>
              <Option value="EXCEL">Excel</Option>
              <Option value="PDF">PDF</Option>
              <Option value="CSV">CSV</Option>
            </Select>
          </Form.Item>
          <Form.Item name="config" label="额外配置">
            <TextArea rows={3} placeholder='{"key": "value"}' />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

// 主页面组件
const Analytics = () => {
  const [activeTab, setActiveTab] = useState('device-usage');
  const [dateRange, setDateRange] = useState([
    dayjs().subtract(7, 'day'),
    dayjs(),
  ]);
  const [aggregationLevel, setAggregationLevel] = useState('HOUR');
  const [loading, setLoading] = useState(false);
  
  // 数据状态
  const [deviceUsageData, setDeviceUsageData] = useState(null);
  const [bandwidthData, setBandwidthData] = useState(null);
  const [storageData, setStorageData] = useState(null);
  const [alertData, setAlertData] = useState(null);
  const [subscriptions, setSubscriptions] = useState([]);
  const [subscriptionsLoading, setSubscriptionsLoading] = useState(false);
  
  // 加载数据的通用方法
  const fetchData = useCallback(async (apiFunc, setter) => {
    setLoading(true);
    try {
      const [startTime, endTime] = dateRange;
      const params = {
        startTime: startTime?.toISOString(),
        endTime: endTime?.toISOString(),
        level: aggregationLevel,
      };
      const response = await apiFunc(params);
      setter(response.data);
    } catch (error) {
      console.error('获取数据失败:', error);
      message.error('获取数据失败');
    } finally {
      setLoading(false);
    }
  }, [dateRange, aggregationLevel]);
  
  // 加载各类数据
  useEffect(() => {
    switch (activeTab) {
      case 'device-usage':
        fetchData(analyticsApi.getDeviceUsageStats, setDeviceUsageData);
        break;
      case 'bandwidth':
        fetchData(analyticsApi.getBandwidthStats, setBandwidthData);
        break;
      case 'storage':
        fetchData(analyticsApi.getStorageStats, setStorageData);
        break;
      case 'alerts':
        fetchData(analyticsApi.getAlertStats, setAlertData);
        break;
      default:
        break;
    }
  }, [activeTab, fetchData]);
  
  // 加载订阅列表
  const fetchSubscriptions = useCallback(async () => {
    setSubscriptionsLoading(true);
    try {
      const response = await analyticsApi.getSubscriptions();
      setSubscriptions(response.data);
    } catch (error) {
      console.error('获取订阅列表失败:', error);
    } finally {
      setSubscriptionsLoading(false);
    }
  }, []);
  
  useEffect(() => {
    fetchSubscriptions();
  }, [fetchSubscriptions]);
  
  // 导出报表
  const handleExport = async () => {
    try {
      const [startTime, endTime] = dateRange;
      const response = await analyticsApi.exportReport({
        reportType: activeTab.toUpperCase(),
        format: 'EXCEL',
        startTime: startTime?.toISOString(),
        endTime: endTime?.toISOString(),
        aggregationLevel,
      });
      
      const blob = new Blob([response.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `analytics_${activeTab}_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`;
      link.click();
      window.URL.revokeObjectURL(url);
      message.success('导出成功');
    } catch (error) {
      message.error('导出失败');
    }
  };
  
  const renderPanel = () => {
    switch (activeTab) {
      case 'device-usage':
        return <DeviceUsagePanel data={deviceUsageData} loading={loading} />;
      case 'bandwidth':
        return <BandwidthPanel data={bandwidthData} loading={loading} />;
      case 'storage':
        return <StoragePanel data={storageData} loading={loading} />;
      case 'alerts':
        return <AlertPanel data={alertData} loading={loading} />;
      case 'subscriptions':
        return (
          <SubscriptionManager
            subscriptions={subscriptions}
            loading={subscriptionsLoading}
            onRefresh={fetchSubscriptions}
          />
        );
      default:
        return null;
    }
  };
  
  const aggregationOptions = {
    'device-usage': [
      { value: 'MINUTE', label: '分钟' },
      { value: 'HOUR', label: '小时' },
      { value: 'DAY', label: '日' },
    ],
    'bandwidth': [
      { value: 'MINUTE', label: '分钟' },
      { value: 'HOUR', label: '小时' },
      { value: 'DAY', label: '日' },
    ],
    'storage': [
      { value: 'HOUR', label: '小时' },
      { value: 'DAY', label: '日' },
      { value: 'WEEK', label: '周' },
      { value: 'MONTH', label: '月' },
    ],
    'alerts': [
      { value: 'HOUR', label: '小时' },
      { value: 'DAY', label: '日' },
      { value: 'WEEK', label: '周' },
      { value: 'MONTH', label: '月' },
    ],
  };
  
  return (
    <div className="animate-fade-in">
      {/* 页面标题 */}
      <div className="mb-6 flex items-center gap-4">
        <div
          className="h-8 w-1 rounded-sm"
          style={{ background: 'var(--gradient-accent)', boxShadow: 'var(--shadow-glow)' }}
        />
        <h1
          className="m-0 tracking-[1px]"
          style={{ color: 'var(--color-text-primary)', fontSize: 24, fontWeight: 600 }}
        >
          数据分析与报表
        </h1>
      </div>
      
      {/* 筛选条件栏 */}
      <Card
        className="mb-4 rounded-xl border"
        style={{
          background: 'var(--color-bg-card)',
          borderColor: 'var(--color-border)',
        }}
        styles={{ body: { padding: '12px 16px' } }}
      >
        <div className="flex flex-wrap items-center gap-4">
          <div className="flex items-center gap-2">
            <span style={{ color: 'var(--color-text-secondary)' }}>时间范围:</span>
            <RangePicker
              value={dateRange}
              onChange={setDateRange}
              showTime
              format="YYYY-MM-DD HH:mm"
              style={{ width: 280 }}
            />
          </div>
          <div className="flex items-center gap-2">
            <span style={{ color: 'var(--color-text-secondary)' }}>聚合级别:</span>
            <Select
              value={aggregationLevel}
              onChange={setAggregationLevel}
              style={{ width: 100 }}
            >
              {(aggregationOptions[activeTab] || aggregationOptions['device-usage']).map((opt) => (
                <Option key={opt.value} value={opt.value}>
                  {opt.label}
                </Option>
              ))}
            </Select>
          </div>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => {
              switch (activeTab) {
                case 'device-usage':
                  fetchData(analyticsApi.getDeviceUsageStats, setDeviceUsageData);
                  break;
                case 'bandwidth':
                  fetchData(analyticsApi.getBandwidthStats, setBandwidthData);
                  break;
                case 'storage':
                  fetchData(analyticsApi.getStorageStats, setStorageData);
                  break;
                case 'alerts':
                  fetchData(analyticsApi.getAlertStats, setAlertData);
                  break;
                default:
                  break;
              }
            }}
          >
            刷新
          </Button>
          <Button
            type="primary"
            icon={<DownloadOutlined />}
            onClick={handleExport}
            disabled={activeTab === 'subscriptions'}
          >
            导出报表
          </Button>
        </div>
      </Card>
      
      {/* 标签页 */}
      <Card
        className="rounded-xl border"
        style={{
          background: 'var(--color-bg-card)',
          borderColor: 'var(--color-border)',
        }}
        styles={{ body: { padding: 0 } }}
      >
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          tabBarStyle={{
            padding: '0 16px',
            marginBottom: 0,
            borderBottom: '1px solid var(--color-border)',
          }}
          className="analytics-tabs"
        >
          <TabPane
            tab={
              <span className="flex items-center gap-2">
                <DesktopOutlined />
                设备使用
              </span>
            }
            key="device-usage"
          />
          <TabPane
            tab={
              <span className="flex items-center gap-2">
                <CloudOutlined />
                带宽统计
              </span>
            }
            key="bandwidth"
          />
          <TabPane
            tab={
              <span className="flex items-center gap-2">
                <DatabaseOutlined />
                存储统计
              </span>
            }
            key="storage"
          />
          <TabPane
            tab={
              <span className="flex items-center gap-2">
                <WarningOutlined />
                告警统计
              </span>
            }
            key="alerts"
          />
          <TabPane
            tab={
              <span className="flex items-center gap-2">
                <PieChartOutlined />
                报表订阅
              </span>
            }
            key="subscriptions"
          />
        </Tabs>
        
        <div style={{ padding: 16 }}>
          {loading && (
            <div className="flex justify-center py-20">
              <Spin size="large" />
            </div>
          )}
          {!loading && renderPanel()}
        </div>
      </Card>
    </div>
  );
};

export default Analytics;
