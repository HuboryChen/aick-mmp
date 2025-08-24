import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Statistic, Progress, List, Typography, Tag, Space } from 'antd';
import {
  VideoCameraOutlined,
  ClusterOutlined,
  WifiOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  PlayCircleOutlined
} from '@ant-design/icons';
import axios from 'axios';

const { Title, Text } = Typography;

const Dashboard = () => {
  const [stats, setStats] = useState({
    totalCameras: 0,
    onlineCameras: 0,
    totalEdgeNodes: 0,
    onlineEdgeNodes: 0,
    totalStreams: 0,
    activeStreams: 0
  });
  
  const [alerts, setAlerts] = useState([]);
  const [activities, setActivities] = useState([]);

  useEffect(() => {
    fetchDashboardData();
    const interval = setInterval(fetchDashboardData, 30000); // 每30秒更新一次
    return () => clearInterval(interval);
  }, []);

  const fetchDashboardData = async () => {
    try {
      // 模拟数据，实际应该从API获取
      setStats({
        totalCameras: 24,
        onlineCameras: 22,
        totalEdgeNodes: 6,
        onlineEdgeNodes: 5,
        totalStreams: 18,
        activeStreams: 16
      });
      
      setAlerts([
        { id: 1, type: 'warning', message: '边缘节点 Node-A-02 连接不稳定', time: '10分钟前' },
        { id: 2, type: 'error', message: '摄像头 Camera-B-05 离线', time: '25分钟前' },
        { id: 3, type: 'info', message: '系统自动备份完成', time: '1小时前' }
      ]);
      
      setActivities([
        { id: 1, action: '新增摄像头', details: 'Camera-C-08 已成功添加', time: '2分钟前' },
        { id: 2, action: '流媒体启动', details: '地区A的视频流已开始传输', time: '15分钟前' },
        { id: 3, action: '节点重启', details: '边缘节点 Node-B-01 重启完成', time: '30分钟前' },
        { id: 4, action: '用户登录', details: '管理员 admin 登录系统', time: '45分钟前' }
      ]);
    } catch (error) {
      console.error('获取仪表盘数据失败:', error);
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
          <Card>
            <Statistic
              title="摄像头状态"
              value={stats.onlineCameras}
              suffix={`/ ${stats.totalCameras}`}
              prefix={<VideoCameraOutlined />}
            />
            <Progress
              percent={Math.round((stats.onlineCameras / stats.totalCameras) * 100)}
              size="small"
              status={stats.onlineCameras === stats.totalCameras ? 'success' : 'active'}
            />
          </Card>
        </Col>
        
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="边缘节点"
              value={stats.onlineEdgeNodes}
              suffix={`/ ${stats.totalEdgeNodes}`}
              prefix={<ClusterOutlined />}
            />
            <Progress
              percent={Math.round((stats.onlineEdgeNodes / stats.totalEdgeNodes) * 100)}
              size="small"
              status={stats.onlineEdgeNodes === stats.totalEdgeNodes ? 'success' : 'active'}
            />
          </Card>
        </Col>
        
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="视频流"
              value={stats.activeStreams}
              suffix={`/ ${stats.totalStreams}`}
              prefix={<WifiOutlined />}
            />
            <Progress
              percent={Math.round((stats.activeStreams / stats.totalStreams) * 100)}
              size="small"
              status={stats.activeStreams === stats.totalStreams ? 'success' : 'active'}
            />
          </Card>
        </Col>
        
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="在线用户"
              value={3}
              prefix={<PlayCircleOutlined />}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        {/* 系统告警 */}
        <Col xs={24} lg={12}>
          <Card title="系统告警" style={{ height: '400px' }}>
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
          <Card title="系统活动" style={{ height: '400px' }}>
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