import React, { useState, useEffect } from 'react';
import { Button, Space, Avatar, Dropdown, Badge, Typography, Tooltip } from 'antd';
import { useNavigate } from 'react-router-dom';
import {
  MenuUnfoldOutlined,
  MenuFoldOutlined,
  BellOutlined,
  LogoutOutlined,
  UserOutlined,
  KeyOutlined,
  SunOutlined,
  MoonOutlined
} from '@ant-design/icons';
import { useTheme } from '../theme';
import './Header.css';

const { Text } = Typography;

const Header = ({ 
  collapsed, 
  toggle, 
  userInfo, 
  notifications = [], 
  onLogout, 
  isMobile = false,
  systemName = 'AICK视频监控平台',
}) => {
  const { theme, isDark, toggleTheme } = useTheme();
  const [entered, setEntered] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (!prefersReducedMotion) {
      setEntered(true);
    }
  }, []);

  const handleUserMenuClick = ({ key }) => {
    switch (key) {
      case 'profile':
        navigate('/profile');
        break;
      case 'resetPassword':
        navigate('/profile?tab=password');
        break;
      case 'logout':
        onLogout?.();
        break;
      default:
        break;
    }
  };

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined style={{ fontSize: 14 }} />,
      label: '个人中心',
    },
    {
      key: 'resetPassword',
      icon: <KeyOutlined style={{ fontSize: 14 }} />,
      label: '重置密码',
    },
    { type: 'divider' },
    {
      key: 'logout',
      icon: <LogoutOutlined style={{ fontSize: 14 }} />,
      label: '退出登录',
    },
  ];

  const notificationMenuItems = (notifications || []).map((notification, index) => ({
    key: index,
    label: (
      <div style={{ maxWidth: '240px' }}>
        <Text strong style={{ color: 'var(--color-text-primary)', fontSize: 13 }}>{notification.title}</Text>
        <br />
        <Text type="secondary" style={{ fontSize: 12 }}>
          {notification.time}
        </Text>
      </div>
    )
  }));

  const displayName = systemName && systemName.trim() !== '' 
    ? systemName 
    : 'AICK视频监控平台';

  return (
    <div className={`app-header${entered ? ' header-enter' : ''}`}>
      {/* Left Section - Menu Toggle + System Name */}
      <div className="header-left">
        {!isMobile && (
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={toggle}
            className="header-toggle-btn"
          />
        )}
        
        {/* System Name / Page Title */}
        <div className="header-title-group">
          <div className="title-accent-bar" />
          <Text strong className="header-system-name">
            {isMobile ? (displayName.length > 8 ? displayName.slice(0, 7) + '…' : displayName) : displayName}
          </Text>
        </div>
      </div>

      {/* Right Section - Actions */}
      <div className="header-right">
        <Space size={4}>
          {/* Theme Toggle */}
          <Tooltip title={isDark ? '切换亮色模式' : '切换暗色模式'}>
            <Button
              type="text"
              icon={isDark ? <SunOutlined /> : <MoonOutlined />}
              onClick={toggleTheme}
              className="header-action-btn"
            />
          </Tooltip>

          {/* Notifications */}
          <Dropdown
            menu={{ items: notificationMenuItems }}
            trigger={['click']}
            placement="bottomRight"
          >
            <Badge count={(notifications || []).length} offset={[-2, 4]} size="small">
              <Button
                type="text"
                icon={<BellOutlined />}
                className="header-action-btn"
              />
            </Badge>
          </Dropdown>

          {/* Divider */}
          {!isMobile && <div className="header-divider-vertical" />}

          {/* User Area - Click avatar to enter profile */}
          <Dropdown
            menu={{ items: userMenuItems, onClick: handleUserMenuClick }}
            trigger={['click']}
            placement="bottomRight"
          >
            <div className="user-menu-trigger">
              <Avatar 
                size={34} 
                icon={<UserOutlined />}
                src={userInfo?.avatar}
                className="user-avatar"
              />
              {!isMobile && (
                <div className="user-info">
                  <span className="user-username">
                    {userInfo?.username || 'Admin'}
                  </span>
                  <span className="user-role">
                    {userInfo?.role || '管理员'}
                  </span>
                </div>
              )}
            </div>
          </Dropdown>
        </Space>
      </div>
    </div>
  );
};

export default Header;
