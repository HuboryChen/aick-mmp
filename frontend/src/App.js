import React, { useState } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { Layout, Spin } from 'antd';
import { useAuth } from './hooks/useAuth';
import Sidebar from './components/Sidebar';
import Header from './components/Header';
import PrivateRoute from './components/PrivateRoute';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import VideoWall from './pages/VideoWall';
import CameraManagement from './pages/CameraManagement';
import EdgeNodeManagement from './pages/EdgeNodeManagement';
import RegionManagement from './pages/RegionManagement';
import Playback from './pages/Playback';
import Settings from './pages/SystemSettings';
import Profile from './pages/UserProfile';
import './App.css';

const { Content } = Layout;

function App() {
  const [collapsed, setCollapsed] = useState(false);
  const { userInfo, loading, login, logout } = useAuth();
  const isAuthenticated = !!userInfo;

  const toggle = () => {
    setCollapsed(!collapsed);
  };

  if (loading) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh' 
      }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <Router>
      <Layout style={{ minHeight: '100vh' }}>
        {isAuthenticated && <Sidebar collapsed={collapsed} />}
        <Layout className={isAuthenticated ? (collapsed ? 'content-collapsed' : 'content-expanded') : ''}>
          {isAuthenticated && (
            <Header 
              collapsed={collapsed} 
              toggle={toggle}
              userInfo={userInfo}
              notifications={[]}
              onLogout={logout}
            />
          )}
          <Content style={{ 
            margin: isAuthenticated ? '64px 16px 0' : 0,
            marginTop: isAuthenticated ? 64 : 0
          }}>
            <div className="content-container">
              <Routes>
                <Route path="/login" element={<Login />} />
                <Route path="/" element={<PrivateRoute />}>
                  <Route index element={<Dashboard />} />
                  <Route path="/dashboard" element={<Dashboard />} />
                  <Route path="/video-wall" element={<VideoWall />} />
                  <Route path="/cameras" element={<CameraManagement />} />
                  <Route path="/edge-nodes" element={<EdgeNodeManagement />} />
                  <Route path="/regions" element={<RegionManagement />} />
                  <Route path="/playback" element={<Playback />} />
                  <Route path="/settings" element={<Settings />} />
                  <Route path="/profile" element={<Profile />} />
                </Route>
              </Routes>
            </div>
          </Content>
        </Layout>
      </Layout>
    </Router>
  );
}

export default App;