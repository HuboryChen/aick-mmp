import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { Layout, Spin } from 'antd';
import { ThemeProvider } from './theme';
import { AuthProvider, useAuth } from './hooks/useAuth';
import useSystemSettings from './hooks/useSystemSettings';
import Sidebar from './components/Sidebar';
import Header from './components/Header';
import PrivateRoute from './components/PrivateRoute';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import VideoWall from './pages/VideoWall';
import CameraManagement from './pages/CameraManagement';
import ConfigTemplateManagement from './pages/ConfigTemplateManagement';
import CameraDiscovery from './pages/CameraDiscovery';
import CameraBatchImport from './pages/CameraBatchImport';
import EdgeNodeManagement from './pages/EdgeNodeManagement';
import CdnNodeManagement from './pages/CdnNodeManagement';
import RegionManagement from './pages/RegionManagement';
import Playback from './pages/Playback';
import Settings from './pages/SystemSettings';
import Profile from './pages/UserProfile';
import SystemAppManagement from './pages/SystemAppManagement';
import ApiKeyManagement from './pages/ApiKeyManagement';
import AlertManagement from './pages/AlertManagement';
import AlertList from './pages/AlertList';
import Analytics from './pages/Analytics';
import AiPassengerDashboard from './pages/AiPassengerDashboard';
import AiBehaviorAlertCenter from './pages/AiBehaviorAlertCenter';
import AiLicensePlateManagement from './pages/AiLicensePlateManagement';
import AlertNotification from './components/AlertNotification';
import './styles/index.css';

const { Content } = Layout;

function AppContent() {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [isMobile, setIsMobile] = useState(false);
  const { userInfo, loading, login, logout } = useAuth();
  const isAuthenticated = !!userInfo;
  const { systemName } = useSystemSettings();

  // Detect mobile screen
  useEffect(() => {
    const checkMobile = () => {
      setIsMobile(window.innerWidth < 768);
      if (window.innerWidth < 768) {
        setCollapsed(true);
      }
    };
    
    checkMobile();
    window.addEventListener('resize', checkMobile);
    return () => window.removeEventListener('resize', checkMobile);
  }, []);

  // Skip loading screen once auth check is complete
  const renderContent = () => {
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
      <Layout style={{ minHeight: '100vh' }} className="app-layout">
        {isAuthenticated && (
          <>
            <Sidebar 
              collapsed={isMobile ? true : collapsed} 
              onClose={closeMobileMenu}
              isMobile={isMobile}
              systemName={systemName}
            />
            {/* Mobile overlay */}
            {isMobile && mobileMenuOpen && (
              <div className="sidebar-overlay" onClick={closeMobileMenu} />
            )}
          </>
        )}
        <Layout style={{ marginLeft: (!isAuthenticated || (isMobile && !mobileMenuOpen)) ? 0 : undefined }}>
          {isAuthenticated && (
            <Header 
              collapsed={isMobile ? true : collapsed} 
              toggle={toggle}
              userInfo={userInfo}
              notifications={[]}
              onLogout={logout}
              isMobile={isMobile}
              systemName={systemName}
            />
          )}
          <Content className="content-area">
            <div className="content-container">
              <Routes>
                {/* Alert notification component (no route) */}
                <Route path="*" element={<AlertNotification />} />
                <Route path="/login" element={<Login />} />
                <Route path="/" element={<PrivateRoute />}>
                  <Route index element={<Dashboard />} />
                  <Route path="/dashboard" element={<Dashboard />} />
                  <Route path="/video-wall" element={<VideoWall />} />
                  <Route path="/cameras" element={<CameraManagement />} />
                  <Route path="/cameras/templates" element={<ConfigTemplateManagement />} />
                  <Route path="/cameras/discovery" element={<CameraDiscovery />} />
                  <Route path="/cameras/batch-import" element={<CameraBatchImport />} />
                  <Route path="/edge-nodes" element={<EdgeNodeManagement />} />
                  <Route path="/cdn-nodes" element={<CdnNodeManagement />} />
                  <Route path="/regions" element={<RegionManagement />} />
                  <Route path="/playback" element={<Playback />} />
                  <Route path="/system-apps" element={<SystemAppManagement />} />
                  <Route path="/api-keys" element={<ApiKeyManagement />} />
                  <Route path="/settings" element={<Settings />} />
                  <Route path="/profile" element={<Profile />} />
                  <Route path="/alerts/rules" element={<AlertManagement />} />
                  <Route path="/alerts/records" element={<AlertList />} />
                  <Route path="/analytics" element={<Analytics />} />
                  <Route path="/ai/passenger" element={<AiPassengerDashboard />} />
                  <Route path="/ai/alerts" element={<AiBehaviorAlertCenter />} />
                  <Route path="/ai/vehicles" element={<AiLicensePlateManagement />} />
                </Route>
              </Routes>
            </div>
          </Content>
        </Layout>
      </Layout>
    );
  };

  const toggle = () => {
    if (isMobile) {
      setMobileMenuOpen(!mobileMenuOpen);
    } else {
      setCollapsed(!collapsed);
    }
  };

  const closeMobileMenu = () => {
    setMobileMenuOpen(false);
  };

  return renderContent();
}

function App() {
  return (
    <ThemeProvider>
      <Router>
        <AuthProvider>
          <AppContent />
        </AuthProvider>
      </Router>
    </ThemeProvider>
  );
}

export default App;