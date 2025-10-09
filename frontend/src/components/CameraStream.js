import React, { useEffect, useRef, useState } from 'react';
import { Card, Spin, Alert, Button, Typography, Tooltip } from 'antd';
import { PlayCircleOutlined, ReloadOutlined, WarningOutlined, InfoCircleOutlined, VideoCameraOutlined } from '@ant-design/icons';
import { streamingApi } from '../utils/api';

const { Text } = Typography;

const CameraStream = ({ camera, streamUrl, quality, bitrate, onStatusUpdate }) => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [stats, setStats] = useState({});
  const [streamSession, setStreamSession] = useState(null);
  const videoRef = useRef(null);

  useEffect(() => {
    if (camera && camera.id) {
      loadStream();
    }
    
    return () => {
      if (streamSession) {
        stopStream();
      }
    };
  }, [camera, quality, bitrate]);

  const loadStream = async () => {
    if (!camera || !camera.id) {
      setError('未选择摄像头');
      setLoading(false);
      return;
    }
    
    setLoading(true);
    setError(null);
    
    try {
      // 启动视频流
      const response = await streamingApi.startStream(camera.id);
      
      // 获取流状态
      const statusResponse = await streamingApi.getStreamStatus(camera.id);
      const streamStats = statusResponse.data;
      
      setStreamSession(response.data);
      setStats(streamStats);
      
      onStatusUpdate && onStatusUpdate('connected', streamStats);
      setLoading(false);
    } catch (err) {
      console.error('视频流加载失败:', err);
      setError('视频流加载失败: ' + (err.response?.data?.message || err.message || '未知错误'));
      setLoading(false);
      onStatusUpdate && onStatusUpdate('error', null);
    }
  };

  const stopStream = async () => {
    try {
      if (streamSession && streamSession.sessionId) {
        await streamingApi.stopStream(streamSession.sessionId);
      }
    } catch (err) {
      console.error('停止视频流失败:', err);
    }
  };

  const handleRetry = () => {
    loadStream();
  };

  if (!camera) {
    return (
      <Card className="video-stream-card" style={{ height: '100%', minHeight: '200px' }}>
        <div style={{ 
          display: 'flex', 
          justifyContent: 'center', 
          alignItems: 'center', 
          height: '100%',
          flexDirection: 'column'
        }}>
          <VideoCameraOutlined style={{ fontSize: '48px', color: '#bfbfbf', marginBottom: '16px' }} />
          <Text type="secondary">未选择摄像头</Text>
        </div>
      </Card>
    );
  }

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
            {stats.resolution || 'N/A'} | {stats.bitrate ? `${stats.bitrate}kbps` : 'N/A'} | {stats.latency ? `${stats.latency}ms` : 'N/A'}
          </div>
        </div>
      }
      size="small"
      extra={
        <Tooltip title="刷新流状态">
          <Button 
            type="text" 
            icon={<ReloadOutlined />} 
            onClick={loadStream}
            size="small"
          />
        </Tooltip>
      }
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
        {/* 视频播放器占位 */}
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
            {stats.framerate ? `${stats.framerate}fps` : 'N/A'}
          </div>
          {stats.isRecording && (
            <div style={{
              position: 'absolute',
              top: '8px',
              right: '8px',
              background: 'rgba(255,0,0,0.7)',
              color: 'white',
              padding: '2px 8px',
              borderRadius: '4px',
              fontSize: '12px'
            }}>
              REC
            </div>
          )}
        </div>
        
        {/* 流状态信息 */}
        <div style={{
          position: 'absolute',
          bottom: '8px',
          left: '8px',
          background: 'rgba(0,0,0,0.7)',
          color: 'white',
          padding: '2px 8px',
          borderRadius: '4px',
          fontSize: '12px',
          display: 'flex',
          alignItems: 'center'
        }}>
          <InfoCircleOutlined style={{ marginRight: '4px' }} />
          {stats.status || 'Unknown'}
        </div>
      </div>
    </Card>
  );
};

export default CameraStream;