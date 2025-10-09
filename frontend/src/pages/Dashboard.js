import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Statistic, Progress, List, Typography, Tag, Space, message } from 'antd';
import {
  VideoCameraOutlined,
  ClusterOutlined,
  WifiOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  PlayCircleOutlined
} from '@ant-design/icons';
import { dashboardApi, cameraApi, edgeNodeApi } from '../utils/api';

const { Title, Text } = Typography;

const Dashboard = () => {
  const [stats, setStats] = useState({
    totalCameras: 0,
    onlineCameras: 0,
    totalEdgeNodes: 0,
    onlineEdgeNodes: 0,
    totalStreams: 0,
    activeStreams: 0,
    onlineUsers: 0
  });
  
  const [alerts, setAlerts] = useState([]);
  const [activities, setActivities] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchDashboardData();
    const interval = setInterval(fetchDashboardData, 30000); // 每30秒更新一次
    return () => clearInterval(interval);
  }, []);

  const fetchDashboardData = async () => {
    setLoading(true);
    try {
      // 获取仪表盘统计数据
      const statsResponse = await dashboardApi.getStats();
      setStats(statsResponse.data);
      
      // 获取边缘节点统计数据（用于告警）
      const edgeNodeResponse = await edgeNodeApi.getEdgeNodes({ size: 1000 });
      const edgeNodes = edgeNodeResponse.data.content;
      
      // 设置告警数据（这里模拟一些告警）
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
      
      // 设置活动数据（这里模拟一些活动）
      const newActivities = [];
      
      // 添加节点相关活动
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

  const getAlertIcon = (type) => {
    switch (type) {
      case 'error': return <ExclamationCircleOutlined style={{ color: '#ff4d4f' }} />;
      case 'warning': return <ExclamationCircleOutlined style={{ color: '#faad14' }} />;
      default: return <CheckCircleOutlined style={{ color: '#52c41a' }} />;
    }
  };

  const getAlertColor = (type) => {
    switch (type) {
      case 'error': return 'error';
      case 'warning': return 'warning';
      default: return 'success';
    }
  };

  return (
    <div>
      <Title level={2} style={{ marginBottom: '24px' }}>系统监控仪表盘</Title>
      
      {/* 统计卡片 */}
      <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="摄像头状态"
              value={stats.onlineCameras}
              suffix={`/ ${stats.totalCameras}`}
              prefix={<VideoCameraOutlined />}
            />
            <Progress
              percent={stats.totalCameras ? Math.round((stats.onlineCameras / stats.totalCameras) * 100) : 0}
              size="small"
              status={stats.onlineCameras === stats.totalCameras ? 'success' : 'active'}
            />
          </Card>
        </Col>
        
        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="边缘节点"
              value={stats.onlineEdgeNodes}
              suffix={`/ ${stats.totalEdgeNodes}`}
              prefix={<ClusterOutlined />}
            />
            <Progress
              percent={stats.totalEdgeNodes ? Math.round((stats.onlineEdgeNodes / stats.totalEdgeNodes) * 100) : 0}
              size="small"
              status={stats.onlineEdgeNodes === stats.totalEdgeNodes ? 'success' : 'active'}
            />
          </Card>
        </Col>
        
        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="视频流"
              value={stats.activeStreams}
              suffix={`/ ${stats.totalStreams}`}
              prefix={<WifiOutlined />}
            />
            <Progress
              percent={stats.totalStreams ? Math.round((stats.activeStreams / stats.totalStreams) * 100) : 0}
              size="small"
              status={stats.activeStreams === stats.totalStreams ? 'success' : 'active'}
            />
          </Card>
        </Col>
        
        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="在线用户"
              value={stats.onlineUsers}
              prefix={<PlayCircleOutlined />}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        {/* 系统告警 */}
        <Col xs={24} lg={12}>
          <Card title="系统告警" style={{ height: '400px' }} loading={loading}>
            <List
              dataSource={alerts}
              renderItem={item => (
                <List.Item>
                  <List.Item.Meta
                    avatar={getAlertIcon(item.type)}
                    title={
                      <Space>
                        <span>{item.message}</span>
                        <Tag color={getAlertColor(item.type)}>
                          {item.type === 'error' ? '错误' : item.type === 'warning' ? '警告' : '信息'}
                        </Tag>
                      </Space>
                    }
                    description={item.time}
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
        
        {/* 系统活动 */}
        <Col xs={24} lg={12}>
          <Card title="系统活动" style={{ height: '400px' }} loading={loading}>
            <List
              dataSource={activities}
              renderItem={item => (
                <List.Item>
                  <List.Item.Meta
                    title={item.action}
                    description={
                      <div>
                        <Text>{item.details}</Text>
                        <br />
                        <Text type="secondary" style={{ fontSize: '12px' }}>{item.time}</Text>
                      </div>
                    }
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;