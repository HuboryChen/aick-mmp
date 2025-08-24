import React from 'react';
import { Layout, Button, Space, Avatar, Dropdown, Badge, Typography } from 'antd';
import {
  MenuUnfoldOutlined,
  MenuFoldOutlined,
  BellOutlined,
  LogoutOutlined,
  UserOutlined
} from '@ant-design/icons';

const { Header: AntHeader } = Layout;
const { Text } = Typography;

const Header = ({ collapsed, toggle, userInfo, notifications, onLogout }) => {
  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人中心'
    },
    {
      type: 'divider'
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: onLogout
    }
  ];

  const notificationMenuItems = notifications.map((notification, index) => ({
    key: index,
    label: (
      <div style={{ maxWidth: '200px' }}>
        <Text strong>{notification.title}</Text>
        <br />
        <Text type="secondary" style={{ fontSize: '12px' }}>
          {notification.time}
        </Text>
      </div>
    )
  }));

  return (
    <AntHeader
      style={{
        padding: '0 16px',
        background: '#fff',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginLeft: collapsed ? 80 : 200,
        transition: 'margin-left 0.2s'
      }}
    >
      <Button
        type="text"
        icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
        onClick={toggle}
        style={{
          fontSize: '16px',
          width: 64,
          height: 64,
        }}
      />
      
      <Space>
        <Dropdown
          menu={{ items: notificationMenuItems }}
          trigger={['click']}
          placement="bottomRight"
        >
          <Badge count={notifications.length} offset={[-3, 3]}>
            <Button
              type="text"
              icon={<BellOutlined />}
              style={{
                fontSize: '16px',
                width: 40,
                height: 40,
              }}
            />
          </Badge>
        </Dropdown>
        
        <Dropdown
          menu={{ items: userMenuItems }}
          trigger={['click']}
          placement="bottomRight"
        >
          <Space style={{ cursor: 'pointer', padding: '0 8px' }}>
            <Avatar icon={<UserOutlined />} />
            <Text>{userInfo?.username || 'Admin'}</Text>
          </Space>
        </Dropdown>
      </Space>
    </AntHeader>
  );
};

export default Header;