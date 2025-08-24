import React, { useEffect, useRef, useState } from 'react';
import { Card, Spin, Alert, Button, Typography } from 'antd';
import { PlayCircleOutlined, ReloadOutlined, WarningOutlined } from '@ant-design/icons';

const { Text } = Typography;

const CameraStream = ({ camera, streamUrl, quality, onStatusUpdate }) => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [stats, setStats] = useState({});
  const videoRef = useRef(null);

  useEffect(() => {
    if (camera && streamUrl) {
      loadStream();
    }
  }, [camera, streamUrl, quality]);

  const loadStream = async () => {
    setLoading(true);
    setError(null);
    
    try {
      // 模拟流加载
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // 模拟流统计
      const mockStats = {
        bitrate: Math.floor(Math.random() * 3000) + 1000,
        framerate: 25,
        resolution: quality === '1080p' ? '1920x1080' : quality === '720p' ? '1280x720' : '640x480',
        latency: Math.floor(Math.random() * 100) + 50
      };
      
      setStats(mockStats);
      onStatusUpdate && onStatusUpdate(camera.id, 'connected', mockStats);
      setLoading(false);
    } catch (err) {
      setError('Failed to load video stream');
      setLoading(false);
      onStatusUpdate && onStatusUpdate(camera.id, 'error', null);
    }
  };

  const handleRetry = () => {
    loadStream();
  };

  if (loading) {
    return (
      <Card className="video-stream-card" style={{ height: '100%', minHeight: '200px' }}>
        <div style={{ 
          display: 'flex', 
          justifyContent: 'center', 
          alignItems: 'center', 
          height: '100%',
          flexDirection: 'column'
        }}>
          <Spin size="large" />
          <Text style={{ marginTop: '16px' }}>加载视频流...</Text>
        </div>
      </Card>
    );
  }

  if (error) {
    return (
      <Card className="video-stream-card error" style={{ height: '100%', minHeight: '200px' }}>
        <div style={{ 
          display: 'flex', 
          justifyContent: 'center', 
          alignItems: 'center', 
          height: '100%',
          flexDirection: 'column'
        }}>
          <WarningOutlined style={{ fontSize: '48px', color: '#ff4d4f', marginBottom: '16px' }} />
          <Alert 
            message="视频流加载失败" 
            description={error}
            type="error" 
            showIcon={false}
            style={{ marginBottom: '16px' }}
          />
          <Button icon={<ReloadOutlined />} onClick={handleRetry}>
            重试
          </Button>
        </div>
      </Card>
    );
  }

  return (
    <Card 
      className="video-stream-card"
      style={{ height: '100%', minHeight: '200px' }}
      title={
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span>{camera.name}</span>
          <div style={{ fontSize: '12px', color: '#666' }}>
            {stats.resolution} | {stats.bitrate}kbps | {stats.latency}ms
          </div>
        </div>
      }
      size="small"
    >
      <div 
        className="video-container"
        style={{ 
          position: 'relative',
          width: '100%',
          height: '200px',
          backgroundColor: '#000',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          borderRadius: '6px',
          overflow: 'hidden'
        }}
      >
        {/* 模拟视频播放器 */}
        <div style={{
          width: '100%',
          height: '100%',
          background: 'linear-gradient(45deg, #1a1a1a 25%, transparent 25%), linear-gradient(-45deg, #1a1a1a 25%, transparent 25%), linear-gradient(45deg, transparent 75%, #1a1a1a 75%), linear-gradient(-45deg, transparent 75%, #1a1a1a 75%)',
          backgroundSize: '20px 20px',
          backgroundPosition: '0 0, 0 10px, 10px -10px, -10px 0px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          position: 'relative'
        }}>
          <PlayCircleOutlined style={{ fontSize: '48px', color: '#1890ff', opacity: 0.8 }} />
          <div style={{
            position: 'absolute',
            top: '8px',
            left: '8px',
            background: 'rgba(0,0,0,0.7)',
            color: 'white',
            padding: '2px 8px',
            borderRadius: '4px',
            fontSize: '12px'
          }}>
            LIVE
          </div>
          <div style={{
            position: 'absolute',
            bottom: '8px',
            right: '8px',
            background: 'rgba(0,0,0,0.7)',
            color: 'white',
            padding: '2px 8px',
            borderRadius: '4px',
            fontSize: '12px'
          }}>
            {stats.framerate}fps
          </div>
        </div>
      </div>
    </Card>
  );
};

export default CameraStream;