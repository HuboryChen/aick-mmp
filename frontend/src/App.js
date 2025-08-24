import React, { useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Layout, theme, Spin, message } from 'antd';
import {
  MenuUnfoldOutlined,
  MenuFoldOutlined,
  VideoCameraOutlined,
  HistoryOutlined,
  SettingOutlined,
  UserOutlined,
  BellOutlined,
  LogoutOutlined
} from '@ant-design/icons';
import axios from 'axios';
import Cookies from 'js-cookie';
import Dashboard from './pages/Dashboard';
import VideoWall from './pages/VideoWall';
import CameraManagement from './pages/CameraManagement';
import EdgeNodeManagement from './pages/EdgeNodeManagement';
import Playback from './pages/Playback';
import SystemSettings from './pages/SystemSettings';
import UserProfile from './pages/UserProfile';
import Login from './pages/Login';
import PrivateRoute from './components/PrivateRoute';
import Sidebar from './components/Sidebar';
import Header from './components/Header';
import './App.css';

const { Content } = Layout;

const App = () => {
  const [collapsed, setCollapsed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [userInfo, setUserInfo] = useState(null);
  const [notifications, setNotifications] = useState([]);

  const { token: { colorBgContainer, borderRadiusLG } } = theme.useToken();

  // Check if user is logged in
  React.useEffect(() => {
    const checkAuth = async () => {
      const token = Cookies.get('token');
      if (token) {
        try {
          setLoading(true);
          const response = await axios.get('/api/auth/me', {
            headers: { Authorization: `Bearer ${token}` }
          });
          setUserInfo(response.data);
        } catch (error) {
          message.error('Session expired, please login again');
          Cookies.remove('token');
        } finally {
          setLoading(false);
        }
      }
    };

    checkAuth();
  }, []);

  const toggle = () => {
    setCollapsed(!collapsed);
  };

  const handleLogout = () => {
    Cookies.remove('token');
    setUserInfo(null);
    message.success('Logged out successfully');
  };

  if (loading) {
    return (
      <div className="loading-container">
        <Spin size="large" />
      </div>
    );
  }

  return (
    <Router>
      {userInfo ? (
        <Layout style={{ minHeight: '100vh' }}>
          <Sidebar collapsed={collapsed} />
          <Layout>
            <Header
              collapsed={collapsed}
              toggle={toggle}
              userInfo={userInfo}
              notifications={notifications}
              onLogout={handleLogout}
            />
            <Content
              style={{
                margin: '24px 16px',
                padding: 24,
                minHeight: 280,
                background: colorBgContainer,
                borderRadius: borderRadiusLG,
              }}
            >
              <Routes>
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/video-wall" element={<VideoWall />} />
                <Route path="/cameras" element={<CameraManagement />} />
                <Route path="/edge-nodes" element={<EdgeNodeManagement />} />
                <Route path="/playback" element={<Playback />} />
                <Route path="/settings" element={<SystemSettings />} />
                <Route path="/profile" element={<UserProfile />} />
                <Route path="/" element={<Navigate to="/dashboard" />} />
              </Routes>
            </Content>
          </Layout>
        </Layout>
      ) : (
        <Routes>
          <Route path="/login" element={<Login setUserInfo={setUserInfo} />} />
          <Route path="/*" element={<Navigate to="/login" />} />
        </Routes>
      )}
    </Router>
  );
};

export default App;