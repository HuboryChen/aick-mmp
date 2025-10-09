import React, { useState, useEffect, useRef } from 'react';
import { Layout, Card, List, Button, message, Spin, Input, Row, Col, Checkbox, Slider, Select, 
         Modal, Dropdown, Menu, Badge } from 'antd';
import { VideoCameraOutlined, PlayCircleOutlined, PauseCircleOutlined, 
         ZoomInOutlined, SettingOutlined, EyeOutlined, LayoutOutlined, MoreOutlined,
         ReloadOutlined, FullscreenOutlined, FullscreenExitOutlined } from '@ant-design/icons';
import classNames from 'classnames';
import { cameraApi } from '../utils/api';
import Cookies from 'js-cookie';
import CameraStream from '../components/CameraStream';
import SplitScreenController from '../components/SplitScreenController';
import VideoQualityController from '../components/VideoQualityController';
import './VideoWall.css';

const { Option } = Select;
const { Content } = Layout;

const VideoWall = () => {
  // State management
  const [layout, setLayout] = useState('4'); // 1, 4, 9, 16
  const [cameras, setCameras] = useState([]);
  const [selectedCameras, setSelectedCameras] = useState([]);
  const [loading, setLoading] = useState(true);
  const [fullscreen, setFullscreen] = useState(false);
  const [quality, setQuality] = useState('720p');
  const [bitrate, setBitrate] = useState(2048);
  const [showCameraSelector, setShowCameraSelector] = useState(false);
  const [editingCameraIndex, setEditingCameraIndex] = useState(-1);
  const [cameraStats, setCameraStats] = useState({});
  const [isRefreshing, setIsRefreshing] = useState(false);

  const videoWallRef = useRef(null);

  // Layout configurations
  const layoutConfigs = {
    '1': { rows: 1, cols: 1 },
    '4': { rows: 2, cols: 2 },
    '9': { rows: 3, cols: 3 },
    '16': { rows: 4, cols: 4 }
  };

  // Fetch available cameras
  useEffect(() => {
    fetchCameras();
  }, []);

  // Update layout when layout changes or cameras are loaded
  useEffect(() => {
    if (cameras.length > 0 && selectedCameras.length === 0) {
      // Initialize with first N cameras
      const initialCameras = cameras.slice(0, parseInt(layout));
      setSelectedCameras(initialCameras);
    } else if (selectedCameras.length > parseInt(layout)) {
      // If we have more cameras than layout allows, trim the list
      setSelectedCameras(selectedCameras.slice(0, parseInt(layout)));
    } else if (selectedCameras.length < parseInt(layout) && cameras.length > 0) {
      // If we have fewer cameras than layout allows, fill with available cameras
      const needed = parseInt(layout) - selectedCameras.length;
      const availableCameras = cameras.filter(cam => 
        !selectedCameras.some(selected => selected && selected.id === cam.id)
      );
      const additional = availableCameras.slice(0, needed);
      setSelectedCameras([...selectedCameras, ...additional]);
    }
  }, [layout, cameras, selectedCameras]);

  // Handle fullscreen toggle
  useEffect(() => {
    const handleFullscreenChange = () => {
      setFullscreen(!!document.fullscreenElement);
    };

    document.addEventListener('fullscreenchange', handleFullscreenChange);
    return () => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
    };
  }, []);

  // Fetch cameras from API
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

  // Refresh camera streams
  const refreshStreams = async () => {
    setIsRefreshing(true);
    try {
      // Stop all current streams
      setSelectedCameras([]);
      // Wait a moment before restarting
      await new Promise(resolve => setTimeout(resolve, 1000));
      // Reload cameras
      await fetchCameras();
      message.success('Streams refreshed successfully');
    } catch (error) {
      message.error('Failed to refresh streams: ' + error.message);
    } finally {
      setIsRefreshing(false);
    }
  };

  // Change layout handler
  const handleLayoutChange = (value) => {
    setLayout(value);
  };

  // Change camera in a position
  const handleCameraChange = (index, camera) => {
    const newCameras = [...selectedCameras];
    newCameras[index] = camera;
    setSelectedCameras(newCameras);
    setShowCameraSelector(false);
    setEditingCameraIndex(-1);
  };

  // Open camera selector for a specific position
  const openCameraSelector = (index) => {
    setEditingCameraIndex(index);
    setShowCameraSelector(true);
  };

  // Toggle fullscreen
  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      videoWallRef.current.requestFullscreen().catch(err => {
        message.error(`Error attempting to enable fullscreen: ${err.message}`);
      });
    } else {
      document.exitFullscreen();
    }
  };

  // Update stream quality
  const handleQualityChange = (value) => {
    setQuality(value);
    // Map quality to bitrate
    const qualityToBitrate = {
      '480p': 1024,
      '720p': 2048,
      '1080p': 4096
    };
    setBitrate(qualityToBitrate[value]);
    message.info(`Video quality set to ${value}`);
  };

  // Update bitrate
  const handleBitrateChange = (value) => {
    setBitrate(value);
    message.info(`Video bitrate set to ${value} kbps`);
  };

  // Get stream URL for a camera
  const getStreamUrl = (cameraId) => {
    return `/api/stream/${cameraId}?quality=${quality}&bitrate=${bitrate}`;
  };

  // Handle camera status update
  const handleStatusUpdate = (cameraId, status, stats) => {
    setCameraStats(prev => ({
      ...prev,
      [cameraId]: stats
    }));
  };

  // Generate grid layout based on selected layout
  const generateGrid = () => {
    const { rows, cols } = layoutConfigs[layout];
    const grid = [];
    const totalCells = parseInt(layout);

    for (let i = 0; i < rows; i++) {
      const row = [];
      for (let j = 0; j < cols; j++) {
        const index = i * cols + j;
        row.push(
          <Col key={`${i}-${j}`} span={24 / cols}>
            <div className="video-cell">
              {selectedCameras[index] ? (
                <Card
                  className="video-card"
                  bordered={false}
                  bodyStyle={{ padding: 0, height: '100%', display: 'flex', flexDirection: 'column' }}
                >
                  <div className="video-header">
                    <div className="camera-name">
                      <VideoCameraOutlined className="camera-icon" />
                      {selectedCameras[index]?.name}
                    </div>
                    <div className="camera-controls">
                      <Badge status={cameraStats[selectedCameras[index]?.id]?.status === 'active' ? 'success' : 'error'} />
                      <Dropdown
                        overlay={(
                          <Menu>
                            <Menu.Item key="replace"
                              onClick={() => openCameraSelector(index)}>
                              Replace Camera
                            </Menu.Item>
                            <Menu.Item key="fullscreen">
                              Fullscreen
                            </Menu.Item>
                            <Menu.Item key="record">
                              Start Recording
                            </Menu.Item>
                            <Menu.Item key="settings">
                              Camera Settings
                            </Menu.Item>
                          </Menu>
                        )}
                      >
                        <Button type="text" size="small" icon={<MoreOutlined />} />
                      </Dropdown>
                    </div>
                  </div>
                  <div className="video-container">
                    <CameraStream
                      camera={selectedCameras[index]}
                      streamUrl={getStreamUrl(selectedCameras[index]?.id)}
                      onStatusUpdate={(status, stats) => handleStatusUpdate(selectedCameras[index]?.id, status, stats)}
                      quality={quality}
                      bitrate={bitrate}
                    />
                  </div>
                  <div className="video-footer">
                    <div className="camera-location">{selectedCameras[index]?.location}</div>
                    <div className="camera-stats">
                      {cameraStats[selectedCameras[index]?.id]?.resolution || quality} | 
                      {cameraStats[selectedCameras[index]?.id]?.fps || '0'} FPS | 
                      {Math.round((cameraStats[selectedCameras[index]?.id]?.bitrate || bitrate) / 1024)} Mbps
                    </div>
                  </div>
                </Card>
              ) : (
                <Card
                  className="empty-video-card"
                  bordered={false}
                  bodyStyle={{ padding: '16px', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                >
                  <div className="empty-video-placeholder">
                    <Select
                      showSearch
                      placeholder="Select a camera"
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
                      style={{ width: '100%' }}
                    >
                      {cameras
                        .filter(cam => !selectedCameras.some(selected => selected && selected.id === cam.id))
                        .map(camera => (
                          <Option key={camera.id} value={camera.id}>
                            {camera.name} ({camera.location || 'No location'})
                          </Option>
                        ))}
                    </Select>
                  </div>
                </Card>
              )}
            </div>
          </Col>
        );
      }
      grid.push(<Row key={i} gutter={[8, 8]} style={{ height: `${100 / rows}%` }}>{row}</Row>);
    }
    return grid;
  };

  return (
    <div ref={videoWallRef} className={classNames('video-wall-container', { 'fullscreen': fullscreen })}>
      <Layout style={{ height: '100%' }}>
        <Content style={{ padding: '16px', height: 'calc(100% - 64px)' }}>
          <div className="video-wall-header">
            <div className="title">
              <VideoCameraOutlined /> Video Wall
            </div>
            <div className="controls">
              <div className="layout-control">
                <span className="control-label">Layout:</span>
                <Select
                  value={layout}
                  onChange={handleLayoutChange}
                  size="small"
                  style={{ width: 100, marginLeft: 8 }}
                >
                  <Option value="1">1 Screen</Option>
                  <Option value="4">4 Screens</Option>
                  <Option value="9">9 Screens</Option>
                  <Option value="16">16 Screens</Option>
                </Select>
              </div>

              <div className="quality-control">
                <span className="control-label">Quality:</span>
                <Select
                  value={quality}
                  onChange={handleQualityChange}
                  size="small"
                  style={{ width: 100, marginLeft: 8 }}
                >
                  <Option value="480p">480p</Option>
                  <Option value="720p">720p</Option>
                  <Option value="1080p">1080p</Option>
                </Select>
              </div>

              <Button
                icon={<ReloadOutlined spin={isRefreshing} />}
                size="small"
                onClick={refreshStreams}
                disabled={isRefreshing}
                className="control-button"
              >
                Refresh
              </Button>

              <Button
                icon={fullscreen ? <FullscreenExitOutlined /> : <FullscreenOutlined />}
                size="small"
                onClick={toggleFullscreen}
                className="control-button"
              >
                {fullscreen ? 'Exit Fullscreen' : 'Fullscreen'}
              </Button>
            </div>
          </div>

          <div className="video-wall-content" style={{ height: 'calc(100% - 60px)' }}>
            {loading ? (
              <div className="loading-container">
                <Spin size="large" tip="Loading cameras..." />
              </div>
            ) : (
              <Layout style={{ height: '100%' }}>
                <Content style={{ height: '100%', padding: 0, overflow: 'hidden' }}>
                  <div className="video-grid" style={{ height: '100%' }}>
                    {generateGrid()}
                  </div>
                </Content>
              </Layout>
            )}
          </div>
        </Content>
      </Layout>

      {/* Camera Selector Modal - Modified to use dropdown instead of CameraSelector component */}
      <Modal
        title="Select Camera"
        open={showCameraSelector}
        onCancel={() => {
          setShowCameraSelector(false);
          setEditingCameraIndex(-1);
        }}
        footer={null}
        width={400}
        destroyOnClose
      >
        <div style={{ padding: '20px' }}>
          <Select
            showSearch
            placeholder="Select a camera"
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
            {cameras
              .filter(cam => 
                !selectedCameras.some(selected => selected && selected.id === cam.id) || 
                (selectedCameras[editingCameraIndex] && selectedCameras[editingCameraIndex].id === cam.id)
              )
              .map(camera => (
                <Option key={camera.id} value={camera.id}>
                  {camera.name} ({camera.location || 'No location'})
                </Option>
              ))}
          </Select>
        </div>
      </Modal>
    </div>
  );
};

export default VideoWall;