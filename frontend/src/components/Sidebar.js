import React, { useState, useEffect } from 'react';
import { Layout, Menu } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  DashboardOutlined,
  VideoCameraOutlined,
  PlaySquareOutlined,
  ClusterOutlined,
  CloudOutlined,
  HistoryOutlined,
  SettingOutlined,
  GlobalOutlined,
  AppstoreOutlined,
  KeyOutlined,
  UserOutlined,
  AlertOutlined,
  AlertFilled,
  BarChartOutlined,
  SearchOutlined,
  DownloadOutlined
} from '@ant-design/icons';
import { useTheme } from '../theme';
import './Sidebar.css';

const { Sider } = Layout;

const Sidebar = ({ collapsed, onClose, isMobile = false, systemName = 'AICK视频监控平台' }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { isDark } = useTheme();
  const [entered, setEntered] = useState(false);
  const [openKeys, setOpenKeys] = useState([]);

  useEffect(() => {
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (!prefersReducedMotion) {
      setEntered(true);
    }
  }, []);

  useEffect(() => {
    const path = location.pathname;
    if (path.startsWith('/cameras')) {
      setOpenKeys(prev => prev.includes('camera-menu') ? prev : [...prev, 'camera-menu']);
    }
  }, [location.pathname]);

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
      key: 'camera-menu',
      icon: <PlaySquareOutlined />,
      label: '摄像头管理',
      children: [
        {
          key: '/cameras',
          icon: <PlaySquareOutlined />,
          label: '摄像头列表'
        },
        {
          key: '/cameras/templates',
          icon: <SettingOutlined />,
          label: '配置模板'
        },
        {
          key: '/cameras/discovery',
          icon: <SearchOutlined />,
          label: '网络发现'
        },
        {
          key: '/cameras/batch-import',
          icon: <DownloadOutlined />,
          label: '批量导入'
        }
      ]
    },
    {
      key: '/edge-nodes',
      icon: <ClusterOutlined />,
      label: '边缘节点'
    },
    {
      key: '/cdn-nodes',
      icon: <CloudOutlined />,
      label: 'CDN节点'
    },
    {
      key: '/regions',
      icon: <GlobalOutlined />,
      label: '地区管理'
    },
    {
      key: '/playback',
      icon: <HistoryOutlined />,
      label: '视频回放'
    },
    {
      key: '/analytics',
      icon: <BarChartOutlined />,
      label: '数据分析'
    },
    {
      key: 'alert-menu',
      icon: <AlertOutlined />,
      label: '告警管理',
      children: [
        {
          key: '/alerts/rules',
          icon: <AlertFilled />,
          label: '告警规则'
        },
        {
          key: '/alerts/records',
          icon: <AlertOutlined />,
          label: '告警记录'
        }
      ]
    },
    {
      key: '/system-apps',
      icon: <AppstoreOutlined />,
      label: '系统应用'
    },
    {
      key: '/api-keys',
      icon: <KeyOutlined />,
      label: 'API Key管理'
    },
    {
      key: '/settings',
      icon: <SettingOutlined />,
      label: '系统设置'
    }
  ];

  const handleMenuClick = ({ key }) => {
    navigate(key);
    if (onClose) {
      onClose();
    }
  };

  return (
    <Sider 
      trigger={null} 
      collapsible 
      collapsed={collapsed}
      width={200}
      collapsedWidth={isMobile ? 0 : 80}
      className={`industrial-sidebar${entered ? ' sidebar-enter' : ''}`}
      style={{
        overflow: 'auto',
        height: '100vh',
        borderRight: '1px solid var(--color-border)',
        transition: 'all 0.3s ease',
        background: 'var(--color-bg-secondary)',
        ...(isMobile ? {
          position: 'fixed',
          left: 0,
          top: 0,
          bottom: 0,
          zIndex: 1000,
        } : {}),
      }}
    >
      {/* Logo Area */}
      <div
        className="sidebar-logo-area"
        style={{
          padding: collapsed ? '0 8px' : '0 16px',
          borderBottom: '1px solid var(--color-border)',
        }}
      >
        {collapsed ? (
          <div className="logo-icon-collapsed">
            {systemName.charAt(0) || 'A'}
          </div>
        ) : (
          <div className="logo-expanded">
            <div className="logo-mark">
              {systemName.slice(0, 2).toUpperCase() || 'AI'}
            </div>
            <div className="logo-text-group">
              <div className="logo-title">
                {systemName || 'AICK视频监控平台'}
              </div>
              <div className="logo-subtitle">Video Monitor</div>
            </div>
          </div>
        )}
      </div>

      {/* Menu */}
      <Menu
        theme={isDark ? 'dark' : 'light'}
        mode="inline"
        selectedKeys={[location.pathname]}
        openKeys={openKeys}
        onOpenChange={setOpenKeys}
        items={menuItems}
        onClick={handleMenuClick}
        className="industrial-menu mt-2 bg-transparent !border-r-0"
      />

      {/* Version Info */}
      {!collapsed && (
        <div
          className="absolute bottom-4 left-0 right-0 border-t px-4 py-2 text-center"
          style={{ borderTopColor: 'var(--color-border)' }}
        >
          <div className="text-[10px] tracking-wide" style={{ color: 'var(--color-text-muted)' }}>
            V1.0.0
          </div>
          <div className="mt-0.5 text-[9px]" style={{ color: 'var(--color-text-muted)', opacity: 0.6 }}>
            Industrial Command Center
          </div>
        </div>
      )}
    </Sider>
  );
};

export default Sidebar;
