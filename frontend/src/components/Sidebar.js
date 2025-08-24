import React from 'react';
import { Layout, Menu } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  DashboardOutlined,
  VideoCameraOutlined,
  PlaySquareOutlined,
  ClusterOutlined,
  HistoryOutlined,
  SettingOutlined,
  UserOutlined
} from '@ant-design/icons';

const { Sider } = Layout;

const Sidebar = ({ collapsed }) => {
  const navigate = useNavigate();
  const location = useLocation();

  const menuItems = [
    {
      key: '/dashboard',
      icon: <DashboardOutlined />,
      label: '仪表盘'
    },
    {
      key: '/video-wall',
      icon: <VideoCameraOutlined />,
      label: '视频墙'
    },
    {
      key: '/cameras',
      icon: <PlaySquareOutlined />,
      label: '摄像头管理'
    },
    {
      key: '/edge-nodes',
      icon: <ClusterOutlined />,
      label: '边缘节点'
    },
    {
      key: '/playback',
      icon: <HistoryOutlined />,
      label: '视频回放'
    },
    {
      key: '/settings',
      icon: <SettingOutlined />,
      label: '系统设置'
    },
    {
      key: '/profile',
      icon: <UserOutlined />,
      label: '个人中心'
    }
  ];

  const handleMenuClick = ({ key }) => {
    navigate(key);
  };

  return (
    <Sider 
      trigger={null} 
      collapsible 
      collapsed={collapsed}
      style={{
        overflow: 'auto',
        height: '100vh',
        position: 'fixed',
        left: 0,
        top: 0,
        bottom: 0,
      }}
    >
      <div className="logo" style={{
        height: '32px',
        margin: '16px',
        background: 'rgba(255, 255, 255, 0.3)',
        borderRadius: '6px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: 'white',
        fontSize: '16px',
        fontWeight: 'bold'
      }}>
        {collapsed ? 'MMP' : '多地区监控'}
      </div>
      <Menu
        theme="dark"
        mode="inline"
        selectedKeys={[location.pathname]}
        items={menuItems}
        onClick={handleMenuClick}
      />
    </Sider>
  );
};

export default Sidebar;