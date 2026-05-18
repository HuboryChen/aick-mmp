import React, { useState, useEffect, useRef, useMemo } from 'react';
import { Layout, Card, Button, message, Spin, Row, Col, Select, 
         Dropdown, Menu, Tooltip, Space } from 'antd';
import { VideoCameraOutlined, 
         ReloadOutlined, 
         FullscreenOutlined, 
         FullscreenExitOutlined,
         MoreOutlined,
         ExpandOutlined,
         SettingOutlined,
         AudioOutlined } from '@ant-design/icons';
import classNames from 'classnames';
import { cameraApi } from '../utils/api';
import CameraStream from '../components/CameraStream';
import GlobalReconnectBar from '../components/GlobalReconnectBar';
import VideoWallSettingsDrawer from '../components/VideoWall/VideoWallSettingsDrawer';
import useVideoWallSettings from '../hooks/useVideoWallSettings';
import { StreamHealthProvider, ConnectionState } from '../contexts/StreamHealthContext';
import './VideoWall.css';

const { Option } = Select;
const { Content } = Layout;

// Status indicator with pulse animation
const StatusIndicator = ({ status }) => {
  const isOnline = status === 'active';
  const color = isOnline ? '#00ff88' : '#ff4757';
  
  return (
    <span
      className="mr-1.5 inline-block h-2 w-2 rounded-full"
      style={{
        backgroundColor: color,
        boxShadow: isOnline
          ? '0 0 6px #00ff88, 0 0 12px #00ff88'
          : '0 0 4px #ff4757',
        animation: isOnline ? 'pulse-glow 2s ease-in-out infinite' : 'none',
      }}
    />
  );
};

// Video control button
const VideoControlButton = ({ icon, tooltip, onClick }) => (
  <Tooltip title={tooltip}>
    <Button
      type="text"
      icon={icon}
      onClick={onClick}
      className="video-control-btn flex h-8 w-8 items-center justify-center rounded-md backdrop-blur-[4px]"
      style={{ background: 'rgba(0, 0, 0, 0.5)' }}
    />
  </Tooltip>
);

const VideoWall = () => {
  const { config, isLoaded, setLayout, setQuality, setBitrate: setConfigBitrate, setSelectedCameras,
          presets, builtInPresets, activePresetId, isLoading, error, saveConfigImmediately,
          applyPreset, createPreset, updatePreset, deletePreset, setAsDefaultPreset, reorderPresets,
          resetToDefaults, canEditPreset, canDeletePreset, isBuiltInPreset, reload } = useVideoWallSettings();
  const [layout, setLayoutState] = useState(config.layout || '4');
  const [cameras, setCameras] = useState([]);
  const [selectedCamerasState, setSelectedCamerasState] = useState([]);
  const [loading, setLoading] = useState(true);
  const [fullscreen, setFullscreen] = useState(false);
  const [quality, setQualityState] = useState(config.quality || '720p');
  const [bitrate, setBitrate] = useState(config.bitrate || 2048);
  const [editingCameraIndex, setEditingCameraIndex] = useState(-1);
  const [cameraStats, setCameraStats] = useState({});
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [hoveredIndex, setHoveredIndex] = useState(-1);
  const [initialized, setInitialized] = useState(false);
  // 跟踪所有摄像头的连接健康状态（用于 GlobalReconnectBar）
  const [cameraHealthStates, setCameraHealthStates] = useState({});

  // 设置抽屉状态
  const [settingsDrawerOpen, setSettingsDrawerOpen] = useState(false);

  const videoWallRef = useRef(null);

  const layoutConfigs = {
    '1': { rows: 1, cols: 1 },
    '4': { rows: 2, cols: 2 },
    '9': { rows: 3, cols: 3 },
    '16': { rows: 4, cols: 4 }
  };

  useEffect(() => {
    if (isLoaded) {
      setLayoutState(config.layout);
      setQualityState(config.quality);
      fetchCameras();
    }
  }, [isLoaded]);

  // 配置加载后初始化摄像头选择
  useEffect(() => {
    if (!isLoaded || cameras.length === 0) return;
    
    // 如果有保存的摄像头配置，尝试恢复
    if (config.cameraIds && config.cameraIds.length > 0) {
      const savedCameraIds = config.cameraIds.map(c => c.id || c);
      const restoredCameras = cameras.filter(cam => savedCameraIds.includes(cam.id));
      if (restoredCameras.length > 0) {
        setSelectedCamerasState(restoredCameras.slice(0, parseInt(layout)));
        setInitialized(true);
        return;
      }
    }
    
    // 否则使用默认逻辑
    if (selectedCamerasState.length === 0) {
      const initialCameras = cameras.slice(0, parseInt(layout));
      setSelectedCamerasState(initialCameras);
      setSelectedCameras(initialCameras);
      setInitialized(true);
    } else if (selectedCamerasState.length > parseInt(layout)) {
      setSelectedCamerasState(selectedCamerasState.slice(0, parseInt(layout)));
    }
  }, [isLoaded, cameras.length]);

  useEffect(() => {
    if (!initialized) return;
    
    if (cameras.length > 0 && selectedCamerasState.length === 0) {
      const initialCameras = cameras.slice(0, parseInt(layout));
      setSelectedCamerasState(initialCameras);
      setSelectedCameras(initialCameras);
    } else if (selectedCamerasState.length > parseInt(layout)) {
      const trimmed = selectedCamerasState.slice(0, parseInt(layout));
      setSelectedCamerasState(trimmed);
      setSelectedCameras(trimmed);
    } else if (selectedCamerasState.length < parseInt(layout) && cameras.length > 0) {
      const needed = parseInt(layout) - selectedCamerasState.length;
      const availableCameras = cameras.filter(cam => 
        !selectedCamerasState.some(selected => selected && selected.id === cam.id)
      );
      const additional = availableCameras.slice(0, needed);
      const newSelected = [...selectedCamerasState, ...additional];
      setSelectedCamerasState(newSelected);
      setSelectedCameras(newSelected);
    }
  }, [layout, cameras, initialized]);

  useEffect(() => {
    const handleFullscreenChange = () => {
      setFullscreen(!!document.fullscreenElement);
    };

    document.addEventListener('fullscreenchange', handleFullscreenChange);
    return () => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
    };
  }, []);

  const fetchCameras = async () => {
    try {
      setLoading(true);
      const response = await cameraApi.getCameras({ status: 'ONLINE' });
      setCameras(response.data.content || []);
    } catch (error) {
      console.error('Failed to fetch cameras:', error);
      message.error('Failed to fetch cameras: ' + (error.response?.data?.message || error.message));
      setCameras([]);
    } finally {
      setLoading(false);
    }
  };

  const refreshStreams = async () => {
    setIsRefreshing(true);
    try {
      setSelectedCamerasState([]);
      await new Promise(resolve => setTimeout(resolve, 1000));
      await fetchCameras();
      message.success('Streams refreshed successfully');
    } catch (error) {
      message.error('Failed to refresh streams: ' + error.message);
    } finally {
      setIsRefreshing(false);
    }
  };

  const handleLayoutChange = (value) => {
    setLayoutState(value);
    setLayout(value);
  };

  const handleCameraChange = (index, camera) => {
    const newCameras = [...selectedCamerasState];
    newCameras[index] = camera;
    setSelectedCamerasState(newCameras);
    setSelectedCameras(newCameras);
    setEditingCameraIndex(-1);
  };

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      videoWallRef.current?.requestFullscreen().catch(err => {
        message.error(`Error attempting to enable fullscreen: ${err.message}`);
      });
    } else {
      document.exitFullscreen();
    }
  };

  const handleQualityChange = (value) => {
    setQualityState(value);
    setQuality(value);
    const qualityToBitrate = {
      '480p': 1024,
      '720p': 2048,
      '1080p': 4096
    };
    setBitrate(qualityToBitrate[value]);
    message.info(`Video quality set to ${value}`);
  };

  const getStreamUrl = (cameraId) => {
    return `/api/stream/${cameraId}?quality=${quality}&bitrate=${bitrate}`;
  };

  const handleStatusUpdate = (cameraId, status, stats) => {
    setCameraStats(prev => ({
      ...prev,
      [cameraId]: stats
    }));
    // 更新摄像头健康状态（用于 GlobalReconnectBar）
    if (status) {
      setCameraHealthStates(prev => ({
        ...prev,
        [cameraId]: {
          ...prev[cameraId],
          connectionState: status,
          lastUpdate: Date.now(),
        }
      }));
    }
  };

  // 全部重试回调（用于 GlobalReconnectBar）
  const handleRetryAll = () => {
    // 重置所有失败摄像头的健康状态，让它们重新连接
    const failedCameraIds = selectedCamerasState
      .filter(cam => cam && cameraHealthStates[cam.id]?.connectionState === ConnectionState.FAILED)
      .map(cam => cam.id);
    
    if (failedCameraIds.length > 0) {
      setCameraHealthStates(prev => {
        const updated = { ...prev };
        failedCameraIds.forEach(id => {
          updated[id] = {
            ...updated[id],
            connectionState: ConnectionState.RECONNECTING,
            retryCount: 0,
          };
        });
        return updated;
      });
      message.info(`正在重试 ${failedCameraIds.length} 个摄像头...`);
    }
  };

  // 构建 camerasInfo 数组供 GlobalReconnectBar 使用
  const camerasInfo = useMemo(() => {
    return selectedCamerasState
      .filter(cam => cam) // 过滤空值
      .map(cam => ({
        cameraId: cam.id,
        cameraName: cam.name,
        connectionState: cameraHealthStates[cam.id]?.connectionState || ConnectionState.IDLE,
        retryCount: cameraHealthStates[cam.id]?.retryCount || 0,
      }));
  }, [selectedCamerasState, cameraHealthStates]);

  const getAvailableCameras = (currentIndex) => {
    return cameras.filter(cam => 
      !selectedCamerasState.some((selected, idx) => 
        selected && selected.id === cam.id && idx !== currentIndex
      )
    );
  };

  const generateGrid = () => {
    const { cols } = layoutConfigs[layout];
    const totalCells = parseInt(layout);
    const rows = Math.ceil(totalCells / cols);

    const grid = [];
    for (let i = 0; i < rows; i++) {
      const row = [];
      for (let j = 0; j < cols; j++) {
        const index = i * cols + j;
        if (index >= totalCells) break;
        
        row.push(
          <Col key={`cell-${index}`} span={24 / cols}>
            <div 
              className={`video-cell ${hoveredIndex === index ? 'hovered' : ''}`}
              onMouseEnter={() => setHoveredIndex(index)}
              onMouseLeave={() => setHoveredIndex(-1)}
            >
              {selectedCamerasState[index] ? (
                <Card
                  className="video-card"
                  bordered={false}
                  bodyStyle={{ 
                    padding: 0, 
                    height: '100%', 
                    display: 'flex', 
                    flexDirection: 'column',
                    background: 'transparent',
                  }}
                >
                  {/* Video Header */}
                  <div className="video-header">
                    <div className="camera-info">
                      <StatusIndicator status={cameraStats[selectedCamerasState[index]?.id]?.status} />
                      <VideoCameraOutlined style={{ 
                        color: 'var(--color-accent)',
                        marginRight: 8,
                      }} />
                      <span className="camera-name">{selectedCamerasState[index]?.name}</span>
                    </div>
                    <Dropdown
                      overlay={
                        <Menu>
                          <Menu.Item key="fullscreen" onClick={() => {
                            const videoEl = document.querySelector(`[data-camera-id="${selectedCamerasState[index]?.id}"]`);
                            videoEl?.requestFullscreen?.();
                          }}>
                            全屏
                          </Menu.Item>
                          <Menu.Item key="replace" onClick={() => setEditingCameraIndex(index)}>
                            更换摄像头
                          </Menu.Item>
                        </Menu>
                      }
                    >
                      <Button type="text" size="small" icon={<MoreOutlined />} />
                    </Dropdown>
                  </div>
                  
                  {/* Video Container */}
                  <div className="video-container" data-camera-id={selectedCamerasState[index]?.id}>
                    <CameraStream
                      camera={selectedCamerasState[index]}
                      streamUrl={getStreamUrl(selectedCamerasState[index]?.id)}
                      onStatusUpdate={(status, stats) => handleStatusUpdate(selectedCamerasState[index]?.id, status, stats)}
                      quality={quality}
                      bitrate={bitrate}
                    />
                  </div>
                  
                  {/* Video Footer */}
                  <div className="video-footer">
                    <div className="camera-location">
                      <span>📍</span> {selectedCamerasState[index]?.location || '未知位置'}
                    </div>
                    <div className="camera-stats">
                      {cameraStats[selectedCamerasState[index]?.id]?.resolution || quality} | 
                      {cameraStats[selectedCamerasState[index]?.id]?.fps || '0'} FPS | 
                      {Math.round((cameraStats[selectedCamerasState[index]?.id]?.bitrate || bitrate) / 1024)} Mbps
                    </div>
                  </div>
                </Card>
              ) : (
                <Card
                  className="empty-video-card"
                  bordered={false}
                  bodyStyle={{ 
                    padding: 0, 
                    height: '100%', 
                    display: 'flex', 
                    alignItems: 'center', 
                    justifyContent: 'center',
                    background: 'transparent',
                  }}
                >
                  <Select
                    showSearch
                    placeholder="选择摄像头"
                    optionFilterProp="children"
                    onChange={(value) => {
                      const camera = cameras.find(cam => cam.id === value);
                      if (camera) {
                        handleCameraChange(index, camera);
                      }
                    }}
                    filterOption={(input, option) =>
                      option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                    }
                    style={{ width: '90%' }}
                    className="camera-select"
                  >
                    {getAvailableCameras(index).map(camera => (
                      <Option key={camera.id} value={camera.id}>
                        {camera.name} ({camera.location || '无位置'})
                      </Option>
                    ))}
                  </Select>
                </Card>
              )}
            </div>
          </Col>
        );
      }
      grid.push(
        <Row key={`row-${i}`} gutter={[{ xs: 12, sm: 16, lg: 20 }, { xs: 12, sm: 16, lg: 20 }]}>
          {row}
        </Row>
      );
    }
    return grid;
  };

  return (
    <StreamHealthProvider>
      <div
        ref={videoWallRef}
        className={classNames('video-wall-container', { 'fullscreen': fullscreen })}
      >
      <Layout style={{ height: '100%', background: 'transparent' }}>
        {/* Video Wall Header */}
        <div className="video-wall-header">
          <div className="header-left">
            <div className="title">
              <VideoCameraOutlined style={{ 
                color: 'var(--color-accent)',
                marginRight: 8,
              }} />
              <span>视频墙</span>
              <span className="camera-count">
                {selectedCamerasState.filter(c => c).length} / {layout} 路
              </span>
            </div>
          </div>
          
          <div className="header-controls">
            {/* Action Buttons */}
            <Space size={8}>
              <Tooltip title="刷新">
                <Button
                  icon={<ReloadOutlined spin={isRefreshing} />}
                  onClick={refreshStreams}
                  disabled={isRefreshing}
                  className="control-btn"
                />
              </Tooltip>

              <Tooltip title="视频墙设置">
                <Button
                  icon={<SettingOutlined />}
                  onClick={() => setSettingsDrawerOpen(true)}
                  className="control-btn"
                />
              </Tooltip>
              
              <Tooltip title={fullscreen ? '退出全屏' : '全屏'}>
                <Button
                  icon={fullscreen ? <FullscreenExitOutlined /> : <FullscreenOutlined />}
                  onClick={toggleFullscreen}
                  className="control-btn"
                />
              </Tooltip>
            </Space>
          </div>
        </div>

        {/* Global Reconnect Bar */}
        <GlobalReconnectBar camerasInfo={camerasInfo} onRetryAll={handleRetryAll} />

        {/* Video Content */}
        <Content style={{ 
          padding: fullscreen ? 0 : 16, 
          height: `calc(100% - ${fullscreen ? 0 : 64}px)`,
          background: fullscreen ? '#000' : 'var(--color-bg-primary)',
        }}>
          {loading ? (
            <div className="loading-container">
              <Spin size="large" tip="加载摄像头中..." />
            </div>
          ) : (
            <div className="video-grid-container">
              {generateGrid()}
            </div>
          )}
        </Content>
      </Layout>

      {/* Camera Selector Modal */}
      {editingCameraIndex >= 0 && (
        <div className="camera-selector-modal">
          <div className="modal-content">
            <div className="modal-header">
              <span>选择摄像头</span>
              <Button 
                type="text" 
                onClick={() => setEditingCameraIndex(-1)}
              >
                ✕
              </Button>
            </div>
            <Select
              showSearch
              autoFocus
              placeholder="搜索摄像头..."
              optionFilterProp="children"
              onChange={(value) => {
                const camera = cameras.find(cam => cam.id === value);
                if (camera) {
                  handleCameraChange(editingCameraIndex, camera);
                }
              }}
              filterOption={(input, option) =>
                option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
              }
              style={{ width: '100%' }}
              size="large"
            >
              {getAvailableCameras(editingCameraIndex).map(camera => (
                <Option key={camera.id} value={camera.id}>
                  {camera.name} ({camera.location || '无位置'})
                </Option>
              ))}
            </Select>
          </div>
        </div>
      )}

      {/* Video Wall Settings Drawer */}
      <VideoWallSettingsDrawer
        visible={settingsDrawerOpen}
        onClose={() => setSettingsDrawerOpen(false)}
        onOpenChange={setSettingsDrawerOpen}
        onLayoutChange={handleLayoutChange}
        onQualityChange={handleQualityChange}
        onBitrateChange={setBitrate}
        // Props from parent hook (eliminating duplicate hook instance in Drawer)
        config={config}
        presets={presets}
        builtInPresets={builtInPresets}
        activePresetId={activePresetId}
        isLoading={isLoading}
        isLoaded={isLoaded}
        error={error}
        reload={reload}
        saveConfigImmediately={saveConfigImmediately}
        applyPreset={applyPreset}
        createPreset={createPreset}
        updatePreset={updatePreset}
        deletePreset={deletePreset}
        setAsDefaultPreset={setAsDefaultPreset}
        reorderPresets={reorderPresets}
        resetToDefaults={resetToDefaults}
        canEditPreset={canEditPreset}
        canDeletePreset={canDeletePreset}
        isBuiltInPreset={isBuiltInPreset}
      />

    </div>
    </StreamHealthProvider>
  );
};

export default VideoWall;
