import React, { useEffect, useState, useCallback } from 'react';
import { notification, Badge, Button, Space, List, Typography, Empty } from 'antd';
import { 
  BellOutlined, 
  WarningOutlined, 
  CloseCircleOutlined,
  InfoCircleOutlined,
  CheckCircleOutlined
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

const { Text } = Typography;

/**
 * WebSocket告警通知组件
 * 实时接收并显示告警通知
 */
const AlertNotification = () => {
  const [alerts, setAlerts] = useState([]);
  const [connected, setConnected] = useState(false);
  const navigate = useNavigate();

  // 告警级别图标和颜色映射
  const levelConfig = {
    'INFO': { icon: <InfoCircleOutlined />, color: '#1890ff' },
    'WARNING': { icon: <WarningOutlined />, color: '#faad14' },
    'ERROR': { icon: <CloseCircleOutlined />, color: '#f5222d' },
    'CRITICAL': { icon: <WarningOutlined />, color: '#722ed1' }
  };

  // 告警通知点击处理
  const handleAlertClick = useCallback((alert) => {
    notification.close(alert.id);
    
    // 跳转到告警列表并显示详情
    navigate('/alerts/records', { 
      state: { selectedAlert: alert } 
    });
  }, [navigate]);

  // 添加新告警
  const addAlert = useCallback((alert) => {
    setAlerts(prev => {
      // 检查是否已存在
      if (prev.some(a => a.id === alert.id)) {
        return prev;
      }
      
      // 添加到列表头部，保留最近10条
      const newAlerts = [alert, ...prev].slice(0, 10);
      return newAlerts;
    });

    // 显示桌面通知
    const config = levelConfig[alert.level] || levelConfig['INFO'];
    
    notification.open({
      key: alert.id,
      message: (
        <Space>
          <span style={{ color: config.color }}>{config.icon}</span>
          <span>{alert.title}</span>
        </Space>
      ),
      description: alert.message || alert.ruleName,
      duration: alert.level === 'CRITICAL' ? 0 : 5, // 严重告警不自动关闭
      placement: 'topRight',
      style: {
        borderLeft: `4px solid ${config.color}`
      },
      onClick: () => handleAlertClick(alert)
    });
  }, [handleAlertClick]);

  // WebSocket连接
  useEffect(() => {
    let ws = null;
    let reconnectTimer = null;

    const connect = () => {
      try {
        // 获取WebSocket URL
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${protocol}//${window.location.host}/ws/alerts`;
        
        ws = new WebSocket(wsUrl);

        ws.onopen = () => {
          console.log('Alert WebSocket connected');
          setConnected(true);
        };

        ws.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            if (data && data.id) {
              addAlert(data);
            }
          } catch (err) {
            console.error('Failed to parse alert message:', err);
          }
        };

        ws.onerror = (error) => {
          console.error('Alert WebSocket error:', error);
          setConnected(false);
        };

        ws.onclose = () => {
          console.log('Alert WebSocket disconnected');
          setConnected(false);
          
          // 尝试重新连接
          reconnectTimer = setTimeout(() => {
            console.log('Attempting to reconnect...');
            connect();
          }, 5000);
        };

      } catch (err) {
        console.error('Failed to connect to Alert WebSocket:', err);
      }
    };

    connect();

    return () => {
      if (ws) {
        ws.close();
      }
      if (reconnectTimer) {
        clearTimeout(reconnectTimer);
      }
    };
  }, [addAlert]);

  // 标记已读
  const markAsRead = (alertId) => {
    setAlerts(prev => prev.filter(a => a.id !== alertId));
    notification.close(alertId);
  };

  // 全部标记已读
  const markAllAsRead = () => {
    alerts.forEach(alert => {
      notification.close(alert.id);
    });
    setAlerts([]);
  };

  return null; // 此组件不需要渲染任何内容，只是用于后台处理通知
};

/**
 * 告警徽章组件
 * 显示未处理告警数量
 */
export const AlertBadge = ({ count, children }) => {
  return (
    <Badge count={count} overflowCount={99} size="small">
      {children}
    </Badge>
  );
};

/**
 * 告警列表组件
 * 显示最近告警列表
 */
export const AlertList = ({ alerts, onAlertClick, onMarkAsRead, maxCount = 5 }) => {
  const levelConfig = {
    'INFO': { icon: <InfoCircleOutlined />, color: '#1890ff' },
    'WARNING': { icon: <WarningOutlined />, color: '#faad14' },
    'ERROR': { icon: <CloseCircleOutlined />, color: '#f5222d' },
    'CRITICAL': { icon: <WarningOutlined />, color: '#722ed1' }
  };

  if (!alerts || alerts.length === 0) {
    return <Empty description="暂无告警" image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  }

  return (
    <List
      size="small"
      dataSource={alerts.slice(0, maxCount)}
      renderItem={(alert) => {
        const config = levelConfig[alert.level] || levelConfig['INFO'];
        return (
          <List.Item
            style={{ 
              cursor: 'pointer',
              background: alert.status === 'UNRESOLVED' ? '#fff2f0' : 'transparent'
            }}
            onClick={() => onAlertClick && onAlertClick(alert)}
          >
            <List.Item.Meta
              avatar={
                <span style={{ color: config.color, fontSize: 16 }}>
                  {config.icon}
                </span>
              }
              title={
                <Text 
                  ellipsis 
                  style={{ 
                    fontWeight: alert.status === 'UNRESOLVED' ? 'bold' : 'normal'
                  }}
                >
                  {alert.title}
                </Text>
              }
              description={
                <Text type="secondary" ellipsis>
                  {alert.ruleName} · {alert.alertTime ? new Date(alert.alertTime).toLocaleString() : ''}
                </Text>
              }
            />
          </List.Item>
        );
      }}
    />
  );
};

export default AlertNotification;
