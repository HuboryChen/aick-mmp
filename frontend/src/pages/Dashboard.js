import React, { useState, useEffect, useRef } from 'react';
import { Row, Col, Card, Statistic, Progress, List, Typography, Tag, Space, message, Table, Tooltip } from 'antd';
import {
  VideoCameraOutlined,
  ClusterOutlined,
  WifiOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  PlayCircleOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  GlobalOutlined,
  DashboardOutlined,
  AlertOutlined,
  CloudServerOutlined
} from '@ant-design/icons';
import { dashboardApi, cameraApi, edgeNodeApi } from '../utils/api';

const { Title, Text } = Typography;

// Format number with comma separators
const formatNumber = (num) => {
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
};

// Animated counter component (enhanced: Phase 3 Task 14.4)
const AnimatedNumber = ({ value, duration = 1000 }) => {
  const [displayValue, setDisplayValue] = useState(0);
  const previousValue = useRef(0);

  useEffect(() => {
    const startTime = Date.now();
    const startValue = previousValue.current;
    const endValue = typeof value === 'number' ? value : parseInt(value) || 0;

    const animate = () => {
      const elapsed = Date.now() - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const easeOut = 1 - Math.pow(1 - progress, 3);
      const current = Math.round(startValue + (endValue - startValue) * easeOut);

      setDisplayValue(current);

      if (progress < 1) {
        requestAnimationFrame(animate);
      } else {
        previousValue.current = endValue;
      }
    };

    requestAnimationFrame(animate);
  }, [value, duration]);

  return <span>{formatNumber(displayValue)}</span>;
};

// Status indicator with pulse animation
const StatusIndicator = ({ status, showPulse = false }) => {
  const statusColors = {
    online: '#00ff88',
    offline: '#ff4757',
    warning: '#fbbf24',
  };
  
  const color = statusColors[status] || statusColors.offline;
  
  return (
    <span
      className="mr-1.5 inline-block h-2 w-2 rounded-full"
      style={{
        backgroundColor: color,
        boxShadow: showPulse ? `0 0 6px ${color}, 0 0 12px ${color}` : `0 0 4px ${color}`,
        animation: showPulse ? 'pulse-glow 2s ease-in-out infinite' : 'none',
      }}
    />
  );
};

// Region statistics card
const RegionStatsCard = ({ regionStats, totalRegions, loading }) => {
  return (
    <Card
      loading={loading}
      className="rounded-xl border"
      style={{
        background: 'var(--color-bg-card)',
        borderColor: 'var(--color-border)',
      }}
      styles={{ body: { padding: 16 } }}
      title={
        <div className="flex items-center gap-2">
          <GlobalOutlined style={{ color: 'var(--color-accent)' }} />
          <span style={{ color: 'var(--color-text-primary)' }}>区域统计</span>
          <Tag color="blue">{totalRegions}</Tag>
        </div>
      }
    >
      <Table
        dataSource={regionStats?.map((r, i) => ({ ...r, key: i })) || []}
        pagination={false}
        size="small"
        columns={[
          {
            title: '区域',
            dataIndex: 'regionName',
            render: (text, record) => (
              <Space>
                <StatusIndicator status={record.onlineCameras > 0 ? 'online' : 'offline'} />
                <span>{text}</span>
              </Space>
            ),
          },
          {
            title: '摄像头',
            dataIndex: 'onlineCameras',
            render: (online, record) => `${online} / ${record.totalCameras}`,
            align: 'center',
          },
          {
            title: '边缘节点',
            dataIndex: 'onlineEdges',
            render: (online, record) => `${online} / ${record.totalEdges}`,
            align: 'center',
          },
        ]}
      />
    </Card>
  );
};

// CDN Node statistics card
const CdnStatsCard = ({ cdnStats, loading }) => {
  if (!cdnStats) return null;
  
  const loadPercent = cdnStats.avgLoad;
  const loadStatus = loadPercent > 80 ? 'error' : loadPercent > 50 ? 'warning' : 'success';
  
  return (
    <Card
      loading={loading}
      className="rounded-xl border"
      style={{
        background: 'var(--color-bg-card)',
        borderColor: 'var(--color-border)',
      }}
      styles={{ body: { padding: 16 } }}
      title={
        <div className="flex items-center gap-2">
          <CloudServerOutlined style={{ color: 'var(--color-accent)' }} />
          <span style={{ color: 'var(--color-text-primary)' }}>CDN节点状态</span>
        </div>
      }
    >
      <Row gutter={[16, 16]}>
        <Col span={12}>
          <Statistic
            title={<span style={{ color: 'var(--color-text-secondary)' }}>在线节点</span>}
            value={cdnStats.onlineNodes}
            suffix={`/ ${cdnStats.totalNodes}`}
            valueStyle={{ color: '#00ff88', fontSize: 24 }}
            prefix={<StatusIndicator status="online" />}
          />
        </Col>
        <Col span={12}>
          <Statistic
            title={<span style={{ color: 'var(--color-text-secondary)' }}>离线节点</span>}
            value={cdnStats.offlineNodes}
            valueStyle={{ color: cdnStats.offlineNodes > 0 ? '#ff4757' : 'var(--color-text-primary)', fontSize: 24 }}
            prefix={<StatusIndicator status="offline" />}
          />
        </Col>
      </Row>
      <div className="mt-4">
        <div className="flex justify-between mb-1">
          <span style={{ color: 'var(--color-text-secondary)' }}>平均负载</span>
          <span style={{ color: loadStatus === 'success' ? '#00ff88' : loadStatus === 'warning' ? '#fbbf24' : '#ff4757' }}>
            {cdnStats.avgLoad}%
          </span>
        </div>
        <Progress
          percent={loadPercent}
          showInfo={false}
          strokeColor={loadStatus === 'success' ? '#00ff88' : loadStatus === 'warning' ? '#fbbf24' : '#ff4757'}
          trailColor="var(--color-bg-secondary)"
        />
      </div>
      <Row gutter={[16, 16]} className="mt-3">
        <Col span={12}>
          <Text type="secondary" className="text-xs">高负载节点</Text>
          <div style={{ color: cdnStats.highLoadNodes > 0 ? '#ff4757' : '#00ff88' }}>{cdnStats.highLoadNodes}</div>
        </Col>
        <Col span={12}>
          <Text type="secondary" className="text-xs">低负载节点</Text>
          <div style={{ color: '#00ff88' }}>{cdnStats.lowLoadNodes}</div>
        </Col>
      </Row>
    </Card>
  );
};

// Alert statistics card
const AlertStatsCard = ({ alertStats, loading }) => {
  if (!alertStats) return null;
  
  return (
    <Card
      loading={loading}
      className="rounded-xl border"
      style={{
        background: 'var(--color-bg-card)',
        borderColor: 'var(--color-border)',
      }}
      styles={{ body: { padding: 16 } }}
      title={
        <div className="flex items-center gap-2">
          <AlertOutlined style={{ color: alertStats.totalActive > 0 ? '#ff4757' : 'var(--color-accent)' }} />
          <span style={{ color: 'var(--color-text-primary)' }}>告警统计</span>
          <Tag color={alertStats.totalActive > 0 ? 'red' : 'green'}>{alertStats.totalActive}</Tag>
        </div>
      }
    >
      <Row gutter={[16, 16]}>
        <Col span={12}>
          <Statistic
            title={<span style={{ color: 'var(--color-text-secondary)', fontSize: 12 }}>待处理</span>}
            value={alertStats.pending}
            valueStyle={{ color: alertStats.pending > 0 ? '#ff4757' : '#00ff88', fontSize: 20 }}
          />
        </Col>
        <Col span={12}>
          <Statistic
            title={<span style={{ color: 'var(--color-text-secondary)', fontSize: 12 }}>已确认</span>}
            value={alertStats.acknowledged}
            valueStyle={{ color: '#fbbf24', fontSize: 20 }}
          />
        </Col>
      </Row>
      <div className="mt-3 pt-3 border-t" style={{ borderColor: 'var(--color-border)' }}>
        <Text type="secondary" className="text-xs block mb-2">今日告警级别分布</Text>
        <Space size={[4, 4]} wrap>
          {alertStats.critical > 0 && <Tag color="red">严重 {alertStats.critical}</Tag>}
          {alertStats.error > 0 && <Tag color="orange">错误 {alertStats.error}</Tag>}
          {alertStats.warning > 0 && <Tag color="gold">警告 {alertStats.warning}</Tag>}
          {alertStats.info > 0 && <Tag color="blue">信息 {alertStats.info}</Tag>}
          {alertStats.triggeredToday === 0 && <Tag color="green">无告警</Tag>}
        </Space>
      </div>
      <div className="mt-3 pt-3 border-t" style={{ borderColor: 'var(--color-border)' }}>
        <Row gutter={[16, 16]}>
          <Col span={12}>
            <Text type="secondary" className="text-xs">今日触发</Text>
            <div style={{ color: 'var(--color-text-primary)', fontWeight: 500 }}>{alertStats.triggeredToday}</div>
          </Col>
          <Col span={12}>
            <Text type="secondary" className="text-xs">今日解决</Text>
            <div style={{ color: '#00ff88', fontWeight: 500 }}>{alertStats.resolvedToday}</div>
          </Col>
        </Row>
      </div>
    </Card>
  );
};

// Industrial styled statistic card
const IndustrialStatCard = ({ title, value, suffix, icon, onlineCount, totalCount, loading, index }) => {
  const percentage = totalCount > 0 ? Math.round((onlineCount / totalCount) * 100) : 0;
  const isAllOnline = onlineCount === totalCount && totalCount > 0;
  
  return (
    <Card
      loading={loading}
      className="industrial-stat-card overflow-hidden rounded-xl border"
      style={{
        background: 'var(--color-bg-card)',
        borderColor: 'var(--color-border)',
        animation: `fade-in 0.5s ease-out ${index * 0.1}s both`,
      }}
      styles={{ body: { padding: 20 } }}
    >
      {/* Top accent line */}
      <div
        className="absolute left-0 right-0 top-0 h-[3px]"
        style={{ background: 'var(--gradient-accent)', boxShadow: 'var(--shadow-glow)' }}
      />

      <div className="flex items-start justify-between">
        <div>
          <Text
            type="secondary"
            className="mb-2 block text-[13px] tracking-[0.5px]"
            style={{ color: 'var(--color-text-secondary)' }}
          >
            {title}
          </Text>

          <div className="mb-1 font-bold leading-none" style={{ fontSize: 32, color: 'var(--color-text-primary)', fontFamily: "'JetBrains Mono', monospace" }}>
            <AnimatedNumber value={value} duration={800} />
            <span className="ml-1 text-lg font-normal" style={{ color: 'var(--color-text-secondary)' }}>
              {suffix}
            </span>
          </div>

          {/* Online status indicator */}
          <div className="mt-2 flex items-center gap-2">
            <StatusIndicator
              status={isAllOnline ? 'online' : percentage > 50 ? 'online' : 'warning'}
              showPulse={isAllOnline}
            />
            <Text className="text-xs" style={{ color: 'var(--color-text-secondary)' }}>
              {percentage}% 在线
            </Text>
          </div>
        </div>

        {/* Icon */}
        <div
          className="flex h-14 w-14 items-center justify-center rounded-xl border"
          style={{
            background: 'var(--color-accent-muted)',
            borderColor: 'rgba(0, 212, 255, 0.2)',
          }}
        >
          <span style={{ fontSize: 24, color: 'var(--color-accent)' }}>{icon}</span>
        </div>
      </div>

      {/* Progress bar */}
      <Progress
        percent={percentage}
        showInfo={false}
        strokeColor={{ '0%': 'var(--color-accent)', '100%': isAllOnline ? '#00ff88' : 'var(--color-accent)' }}
        trailColor="var(--color-bg-secondary)"
        size="small"
        className="industrial-progress mt-4"
      />
    </Card>
  );
};

// Alert item component
const AlertItem = ({ type, message, time, index }) => {
  const typeConfig = {
    error: {
      color: '#ff4757',
      bg: 'rgba(255, 71, 87, 0.1)',
      icon: <ExclamationCircleOutlined />,
      label: '错误',
    },
    warning: {
      color: '#fbbf24',
      bg: 'rgba(251, 191, 36, 0.1)',
      icon: <ExclamationCircleOutlined />,
      label: '警告',
    },
    success: {
      color: '#00ff88',
      bg: 'rgba(0, 255, 136, 0.1)',
      icon: <CheckCircleOutlined />,
      label: '正常',
    },
  };
  
  const config = typeConfig[type] || typeConfig.success;
  
  return (
    <List.Item
      className="mb-2 rounded-lg border"
      style={{
        padding: '12px 16px',
        background: config.bg,
        borderColor: `${config.color}20`,
        animation: `slide-in 0.3s ease-out ${index * 0.05}s both`,
      }}
    >
      <Space size={12} style={{ width: '100%' }}>
        <span className="flex items-center text-lg" style={{ color: config.color }}>
          {config.icon}
        </span>
        <div style={{ flex: 1 }}>
          <div className="mb-0.5 font-medium" style={{ color: 'var(--color-text-primary)' }}>
            {message}
          </div>
          <Text type="secondary" className="text-xs">{time}</Text>
        </div>
        <Tag
          color={type === 'error' ? 'red' : type === 'warning' ? 'orange' : 'green'}
          className="!m-0 !rounded"
        >
          {config.label}
        </Tag>
      </Space>
    </List.Item>
  );
};

// Activity item component
const ActivityItem = ({ action, details, time, index }) => {
  return (
    <List.Item
      className="mb-2 rounded-lg border"
      style={{
        padding: '12px 16px',
        background: 'var(--color-bg-elevated)',
        borderColor: 'var(--color-border)',
        animation: `slide-in 0.3s ease-out ${index * 0.05}s both`,
      }}
    >
      <Space size={12} style={{ width: '100%' }}>
        <div
          className="flex h-8 w-8 items-center justify-center rounded-lg"
          style={{ background: 'var(--color-accent-muted)' }}
        >
          <span style={{ color: 'var(--color-accent)' }}>↻</span>
        </div>
        <div style={{ flex: 1 }}>
          <div className="mb-0.5 font-medium" style={{ color: 'var(--color-text-primary)' }}>
            {action}
          </div>
          <Text type="secondary" className="text-xs">{details}</Text>
        </div>
        <Text type="secondary" className="text-[11px]">{time}</Text>
      </Space>
    </List.Item>
  );
};

const Dashboard = () => {
  const [stats, setStats] = useState({
    totalCameras: 0,
    onlineCameras: 0,
    totalEdgeNodes: 0,
    onlineEdgeNodes: 0,
    totalStreams: 0,
    activeStreams: 0,
    onlineUsers: 0,
    regionStats: [],
    totalRegions: 0,
    cdnNodeStats: null,
    alertStats: null
  });
  
  const [alerts, setAlerts] = useState([]);
  const [activities, setActivities] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchDashboardData();
    const interval = setInterval(fetchDashboardData, 30000);
    return () => clearInterval(interval);
  }, []);

  const fetchDashboardData = async () => {
    setLoading(true);
    try {
      const statsResponse = await dashboardApi.getStats();
      setStats(statsResponse.data);
      
      const edgeNodeResponse = await edgeNodeApi.getEdgeNodes({ size: 1000 });
      const edgeNodes = edgeNodeResponse.data.content;
      
      const newAlerts = [];
      const offlineNodes = edgeNodes.filter(node => node.status === 'OFFLINE');
      
      offlineNodes.slice(0, 3).forEach((node, index) => {
        newAlerts.push({
          id: `node-${index}`,
          type: 'error',
          message: `边缘节点 ${node.name} 离线`,
          time: node.lastHeartbeatTime || '最近'
        });
      });
      
      setAlerts(newAlerts);
      
      const newActivities = [];
      edgeNodes.slice(0, 4).forEach((node, index) => {
        newActivities.push({
          id: `activity-node-${index}`,
          action: '节点心跳',
          details: `边缘节点 ${node.name} 发送心跳`,
          time: node.lastHeartbeatTime || '最近'
        });
      });
      
      setActivities(newActivities);
    } catch (error) {
      console.error('获取仪表盘数据失败:', error);
      message.error('获取仪表盘数据失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="animate-fade-in">
      {/* Page Header */}
      <div className="mb-6 flex items-center gap-4">
        <div
          className="h-8 w-1 rounded-sm"
          style={{ background: 'var(--gradient-accent)', boxShadow: 'var(--shadow-glow)' }}
        />
        <Title level={2} className="!m-0 tracking-[1px]" style={{ color: 'var(--color-text-primary)' }}>
          系统监控仪表盘
        </Title>
      </div>

      {/* Stats Grid */}
      <Row gutter={[{ xs: 8, sm: 12, lg: 16 }, { xs: 8, sm: 12, lg: 16 }]} className="mb-6">
        <Col xs={24} sm={12} lg={6}>
          <IndustrialStatCard
            title="摄像头状态"
            value={stats.onlineCameras}
            suffix={`/ ${stats.totalCameras}`}
            icon={<VideoCameraOutlined />}
            onlineCount={stats.onlineCameras}
            totalCount={stats.totalCameras}
            loading={loading}
            index={0}
          />
        </Col>
        
        <Col xs={24} sm={12} lg={6}>
          <IndustrialStatCard
            title="边缘节点"
            value={stats.onlineEdgeNodes}
            suffix={`/ ${stats.totalEdgeNodes}`}
            icon={<ClusterOutlined />}
            onlineCount={stats.onlineEdgeNodes}
            totalCount={stats.totalEdgeNodes}
            loading={loading}
            index={1}
          />
        </Col>
        
        <Col xs={24} sm={12} lg={6}>
          <IndustrialStatCard
            title="视频流"
            value={stats.activeStreams}
            suffix={`/ ${stats.totalStreams}`}
            icon={<WifiOutlined />}
            onlineCount={stats.activeStreams}
            totalCount={stats.totalStreams}
            loading={loading}
            index={2}
          />
        </Col>
        
        <Col xs={24} sm={12} lg={6}>
          <IndustrialStatCard
            title="在线用户"
            value={stats.onlineUsers}
            suffix=""
            icon={<PlayCircleOutlined />}
            onlineCount={stats.onlineUsers}
            totalCount={stats.onlineUsers}
            loading={loading}
            index={3}
          />
        </Col>
      </Row>

      {/* Extended Stats Row - Region, CDN, Alert */}
      <Row gutter={[{ xs: 8, sm: 12, lg: 16 }, { xs: 8, sm: 12, lg: 16 }]} className="mb-6">
        <Col xs={24} lg={8}>
          <RegionStatsCard 
            regionStats={stats.regionStats}
            totalRegions={stats.totalRegions}
            loading={loading}
          />
        </Col>
        <Col xs={24} lg={8}>
          <CdnStatsCard 
            cdnStats={stats.cdnNodeStats}
            loading={loading}
          />
        </Col>
        <Col xs={24} lg={8}>
          <AlertStatsCard 
            alertStats={stats.alertStats}
            loading={loading}
          />
        </Col>
      </Row>

      {/* Lists Row */}
      <Row gutter={[{ xs: 8, sm: 12, lg: 16 }, { xs: 8, sm: 12, lg: 16 }]}>
        {/* System Alerts */}
        <Col xs={24} lg={12}>
          <Card 
            title={
              <div className="flex items-center gap-2.5">
                <div
                  className="h-2 w-2 rounded-full"
                  style={{
                    background: alerts.length > 0 ? '#ff4757' : '#00ff88',
                    boxShadow: alerts.length > 0 ? '0 0 6px #ff4757' : '0 0 6px #00ff88',
                  }}
                />
                <span style={{ color: 'var(--color-text-primary)' }}>系统告警</span>
                <Tag color={alerts.length > 0 ? 'red' : 'green'}>{alerts.length}</Tag>
              </div>
            }
            className="h-full rounded-xl border"
            style={{
              background: 'var(--color-bg-card)',
              borderColor: 'var(--color-border)',
            }}
            styles={{
              header: { borderBottom: '1px solid var(--color-border)', padding: '12px 20px' },
              body: { padding: 12, maxHeight: 380, overflowY: 'auto' },
            }}
            loading={loading}
          >
            {alerts.length > 0 ? (
              alerts.map((item, index) => (
                <AlertItem key={item.id} {...item} index={index} />
              ))
            ) : (
              <div className="text-center py-10" style={{ color: 'var(--color-text-secondary)' }}>
                <CheckCircleOutlined style={{ fontSize: 32, color: '#00ff88', marginBottom: 12 }} />
                <div>系统运行正常，无告警</div>
              </div>
            )}
          </Card>
        </Col>
        
        {/* System Activities */}
        <Col xs={24} lg={12}>
          <Card 
            title={
              <div className="flex items-center gap-2.5">
                <div
                  className="h-2 w-2 rounded-full"
                  style={{ background: 'var(--color-accent)', animation: 'pulse-glow 2s ease-in-out infinite' }}
                />
                <span style={{ color: 'var(--color-text-primary)' }}>系统活动</span>
              </div>
            }
            className="h-full rounded-xl border"
            style={{
              background: 'var(--color-bg-card)',
              borderColor: 'var(--color-border)',
            }}
            styles={{
              header: { borderBottom: '1px solid var(--color-border)', padding: '12px 20px' },
              body: { padding: 12, maxHeight: 380, overflowY: 'auto' },
            }}
            loading={loading}
          >
            {activities.map((item, index) => (
              <ActivityItem key={item.id} {...item} index={index} />
            ))}
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;
