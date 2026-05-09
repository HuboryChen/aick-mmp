import React, { useEffect, useRef, useState, useCallback } from 'react';
import { Card, Spin, Alert, Button, Typography, Tooltip, Select, Space, Dropdown } from 'antd';
import { 
  ReloadOutlined, 
  WarningOutlined, 
  VideoCameraOutlined,
  PauseOutlined,
  PlayCircleOutlined,
  SettingOutlined,
  FullscreenOutlined,
  FullscreenExitOutlined,
  SoundOutlined,
  MutedOutlined,
  FileSearchOutlined,
  HistoryOutlined,
  WifiOutlined
} from '@ant-design/icons';
import { streamingApi, cameraApi } from '../utils/api';
import RecordingManagement from './RecordingManagement';
import CameraStatisticsOverview from './CameraStatisticsOverview';
import ConnectionStatusBadge from './ConnectionStatusBadge';
import { useStreamHealth, ConnectionState } from '../contexts/StreamHealthContext';
import { calculateDelay, calculateAdaptiveQuality } from '../hooks/useReconnect';

const { Text } = Typography;

const { Option } = Select;

// 全局 WebRTC 连接计数器，避免浏览器限制
let activeConnections = 0;
const MAX_PEER_CONNECTIONS = 10;

/**
 * WebRTC视频流播放器组件
 * 支持摄像头视频流的播放、控制和质量切换
 */
const CameraStream = ({ 
  camera, 
  quality = 'auto',
  autoPlay = true,
  showControls = true,
  showStats = true,
  onStatusUpdate,
  onError
}) => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [stats, setStats] = useState({});
  const [streamSession, setStreamSession] = useState(null);
  const [streamStatus, setStreamStatus] = useState('idle');
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [isMuted, setIsMuted] = useState(false);
  const [currentQuality, setCurrentQuality] = useState(quality);
  const [isRecording, setIsRecording] = useState(false);
  const [showRecordingModal, setShowRecordingModal] = useState(false);
  const [showStatsOverview, setShowStatsOverview] = useState(false);
  const [cameraStatus, setCameraStatus] = useState(null); // 摄像头状态: ONLINE, OFFLINE, etc.

  // Stream health context for connection state management
  const {
    updateConnectionState,
    incrementRetry,
    resetRetry,
    setError: setHealthError,
  } = useStreamHealth();

  const videoRef = useRef(null);
  const peerConnectionRef = useRef(null);
  const remoteStreamRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const isReconnectingRef = useRef(false);

  // 清理函数
  const cleanup = useCallback(() => {
    if (peerConnectionRef.current) {
      peerConnectionRef.current.close();
      peerConnectionRef.current = null;
      activeConnections = Math.max(0, activeConnections - 1); // 减少连接计数
    }
    if (remoteStreamRef.current) {
      remoteStreamRef.current.getTracks().forEach(track => track.stop());
      remoteStreamRef.current = null;
    }
  }, []);

  // 初始化WebRTC连接
  const initWebRTC = useCallback(async (cameraId) => {
    try {
      // 检查全局连接数限制
      if (activeConnections >= MAX_PEER_CONNECTIONS) {
        throw new Error(`连接数已达上限(${MAX_PEER_CONNECTIONS})，请关闭其他视频窗口`);
      }
      
      cleanup();
      activeConnections++; // 增加连接计数
      
      // 创建RTCPeerConnection
      const configuration = {
        iceServers: [
          { urls: 'stun:stun.l.google.com:19302' },
          { urls: 'stun:stun1.l.google.com:19302' }
        ]
      };
      
      const pc = new RTCPeerConnection(configuration);
      peerConnectionRef.current = pc;

      // 处理ICE候选
      pc.onicecandidate = async (event) => {
        if (event.candidate) {
          // 可以发送ICE候选到服务器
          console.log('ICE candidate:', event.candidate);
        }
      };

      // 处理远程流
      pc.ontrack = (event) => {
        console.log('Received remote stream');
        remoteStreamRef.current = event.streams[0];
        if (videoRef.current) {
          videoRef.current.srcObject = event.streams[0];
        }
      };

      // 处理连接状态变化
      pc.onconnectionstatechange = () => {
        console.log('Connection state:', pc.connectionState);
        setStreamStatus(pc.connectionState);

        if (pc.connectionState === 'connected') {
          setLoading(false);
          setError(null);
          updateConnectionState(ConnectionState.CONNECTED);
          onStatusUpdate && onStatusUpdate(ConnectionState.CONNECTED, stats);
        } else if (pc.connectionState === 'failed') {
          updateConnectionState(ConnectionState.FAILED);
          setError('视频流连接中断');
          onStatusUpdate && onStatusUpdate(ConnectionState.FAILED, null);
          // 触发自动重连
          triggerReconnect();
        } else if (pc.connectionState === 'disconnected') {
          updateConnectionState(ConnectionState.DISCONNECTED);
          setError('视频流连接断开');
          onStatusUpdate && onStatusUpdate(ConnectionState.DISCONNECTED, null);
          // 触发自动重连
          triggerReconnect();
        }
      };

      return pc;
    } catch (err) {
      console.error('Failed to initialize WebRTC:', err);
      throw err;
    }
  }, [cleanup, onStatusUpdate, stats]);

  // 检查摄像头状态
  const checkCameraStatus = useCallback(async (cameraId) => {
    try {
      const response = await cameraApi.getCamera(cameraId);
      return response.data?.status;
    } catch (e) {
      console.warn('Failed to get camera status:', e);
      return null;
    }
  }, []);

  // 启动视频流
  const startStream = useCallback(async () => {
    if (!camera || !camera.id) {
      setError('未选择摄像头');
      setLoading(false);
      return;
    }
    
    setLoading(true);
    setError(null);
    setStreamStatus('connecting');
    setCameraStatus(null);
    
    // 1. 先检查摄像头状态
    const status = await checkCameraStatus(camera.id);
    setCameraStatus(status);
    
    // 如果摄像头未启动（OFFLINE 或非 ONLINE 状态），不尝试连接
    if (status && status !== 'ONLINE') {
      const statusMessages = {
        'OFFLINE': '摄像头未启动',
        'STOPPED': '摄像头已停止',
        'ERROR': '摄像头故障',
        'MAINTENANCE': '摄像头维护中',
      };
      setError(`摄像头未就绪: ${statusMessages[status] || status}`);
      setStreamStatus('camera_offline');
      setLoading(false);
      updateConnectionState(ConnectionState.FAILED);
      onStatusUpdate && onStatusUpdate(ConnectionState.FAILED, null);
      return; // 不再重试
    }
    
    try {
      // 初始化WebRTC
      const pc = await initWebRTC(camera.id);
      
      // 获取WebRTC Offer
      const offerResponse = await streamingApi.generateWebRtcOffer(camera.id);
      const offer = offerResponse.data?.offer;
      
      if (!offer) {
        throw new Error('Failed to get WebRTC offer');
      }

      // 设置本地描述（Offer）
      await pc.setLocalDescription(JSON.parse(offer));

      // 发送Offer到服务器，获取Answer
      const answerResponse = await streamingApi.processWebRtcAnswer(camera.id, {
        sdp: pc.localDescription.sdp,
        type: pc.localDescription.type
      });

      // 设置远程描述（Answer）
      if (answerResponse.data?.answer) {
        await pc.setRemoteDescription(JSON.parse(answerResponse.data.answer));
      }

      // 启动视频流会话
      const streamResponse = await streamingApi.startStream(camera.id);
      setStreamSession(streamResponse.data);
      
      // 获取流状态
      const statusResponse = await streamingApi.getStreamStatus(camera.id);
      setStats(statusResponse.data);
      
      // 开始定时获取流指标
      startStatsPolling(camera.id);
      
      setStreamStatus('connected');
      updateConnectionState(ConnectionState.CONNECTED);
      onStatusUpdate && onStatusUpdate(ConnectionState.CONNECTED, statusResponse.data);
      
    } catch (err) {
      console.error('视频流加载失败:', err);
      let errorMessage = err.response?.data?.message || err.message || '未知错误';
      
      // 针对特定错误给出友好提示
      if (errorMessage.includes('Cannot create so many PeerConnections') || 
          errorMessage.includes('PeerConnection')) {
        errorMessage = `连接数已达上限(${MAX_PEER_CONNECTIONS})，请关闭其他视频窗口后再试`;
        // 摄像头未启动时不需要重试
        if (cameraStatus && cameraStatus !== 'ONLINE') {
          errorMessage = `摄像头未就绪，无法建立连接`;
        }
      }
      
      setError('视频流加载失败: ' + errorMessage);
      setStreamStatus('failed');
      updateConnectionState(ConnectionState.FAILED);
      setLoading(false);
      onStatusUpdate && onStatusUpdate(ConnectionState.FAILED, null);
      onError && onError(err);
    }
  }, [camera, initWebRTC, checkCameraStatus, cameraStatus, onStatusUpdate, onError]);

  // 停止视频流
  const stopStream = useCallback(async () => {
    cleanup();
    
    try {
      if (streamSession?.sessionId) {
        await streamingApi.stopStream(camera?.id);
      }
    } catch (err) {
      console.error('停止视频流失败:', err);
    }
    
    setStreamSession(null);
    setStreamStatus('idle');
    setStats({});
  }, [cleanup, streamSession, camera]);

  // 自动重连逻辑（指数退避）- 优化间隔减少闪烁
  const RECONNECT_CONFIG = {
    enabled: true,
    maxRetries: 5,
    initialDelayMs: 3000,  // 延长初始等待时间，减少频繁闪烁
    maxDelayMs: 30000,
  };

  const triggerReconnect = useCallback(async () => {
    if (isReconnectingRef.current || !RECONNECT_CONFIG.enabled) {
      return;
    }

    isReconnectingRef.current = true;
    updateConnectionState(ConnectionState.RECONNECTING);
    setStreamStatus('reconnecting');

    try {
      incrementRetry();
      
      // 计算延迟（指数退避）- 每次重试增加等待时间
      const delay = calculateDelay(
        peerConnectionRef.current?.retryCount || 0, 
        RECONNECT_CONFIG
      );
      console.log(`Reconnecting in ${delay}ms...`);

      // 等待退避时间
      await new Promise((resolve) => {
        reconnectTimeoutRef.current = setTimeout(resolve, delay);
      });

      // 收集 WebRTC 统计信息以决定自适应画质
      let adaptiveQuality = currentQuality;
      try {
        const stats = await peerConnectionRef.current?.getStats();
        stats?.forEach((report) => {
          if (report.type === 'candidate-pair' && report.state === 'succeeded') {
            const rtt = report.currentRoundTripTime ? report.currentRoundTripTime * 1000 : 0;
            adaptiveQuality = calculateAdaptiveQuality({ packetLoss: 0, roundTripTime: rtt }, currentQuality);
          }
        });
      } catch (e) {
        console.warn('Failed to collect stats:', e);
      }

      // 先尝试 ICE 重连（不销毁连接）
      const pc = peerConnectionRef.current;
      if (pc) {
        try {
          // 创建新的 ICE 重启 offer
          const offer = await pc.createOffer({ iceRestart: true });
          await pc.setLocalDescription(offer);
          
          // 发送 offer 获取 answer
          const answerResponse = await streamingApi.processWebRtcAnswer(camera.id, {
            sdp: pc.localDescription.sdp,
            type: pc.localDescription.type
          });
          
          if (answerResponse.data?.answer) {
            await pc.setRemoteDescription(JSON.parse(answerResponse.data.answer));
          }
          
          // 重连成功
          resetRetry();
          updateConnectionState(ConnectionState.CONNECTED);
          setStreamStatus('connected');
          isReconnectingRef.current = false;
          return;
        } catch (e) {
          console.warn('ICE restart failed, full reconnect needed:', e.message);
          // 如果 ICE 重启失败，则完全重建连接
          await stopStream();
        }
      }

      // 完全重建连接
      await startStream();

      // 重连成功
      resetRetry();
      updateConnectionState(ConnectionState.CONNECTED);
      isReconnectingRef.current = false;
    } catch (e) {
      console.error('Reconnect failed:', e);
      setHealthError(`重连失败: ${e.message}`);
      setStreamStatus('failed');
      isReconnectingRef.current = false;
    }
  }, [camera, currentQuality, updateConnectionState, incrementRetry, resetRetry, setHealthError, stopStream, startStream]);

  // 暂停视频流
  const pauseStream = useCallback(async () => {
    try {
      await streamingApi.pauseStream(camera?.id);
      setStreamStatus('paused');
    } catch (err) {
      console.error('暂停视频流失败:', err);
    }
  }, [camera]);

  // 恢复视频流
  const resumeStream = useCallback(async () => {
    try {
      await streamingApi.resumeStream(camera?.id);
      setStreamStatus('connected');
    } catch (err) {
      console.error('恢复视频流失败:', err);
    }
  }, [camera]);

  // 定时获取流指标
  const startStatsPolling = useCallback((cameraId) => {
    const interval = setInterval(async () => {
      try {
        const response = await streamingApi.getStreamStatus(cameraId);
        setStats(response.data);
      } catch (err) {
        console.error('获取流状态失败:', err);
      }
    }, 5000); // 每5秒获取一次
    
    return () => clearInterval(interval);
  }, []);

  // 切换视频质量 - 重建 WebRTC 连接以使画质立即生效
  const handleQualityChange = useCallback(async (newQuality) => {
    const previousQuality = currentQuality;
    setCurrentQuality(newQuality);

    const qualityMap = {
      'low': { resolution: '640x480', bitrate: 500, frameRate: 15 },
      'medium': { resolution: '1280x720', bitrate: 2000, frameRate: 25 },
      'high': { resolution: '1920x1080', bitrate: 4000, frameRate: 30 },
      'auto': { resolution: 'auto', bitrate: 'auto', frameRate: 'auto' },
      '480p': { resolution: '640x480', bitrate: 1024, frameRate: 15 },
      '720p': { resolution: '1280x720', bitrate: 2048, frameRate: 25 },
      '1080p': { resolution: '1920x1080', bitrate: 4096, frameRate: 30 },
    };

    const qualitySettings = qualityMap[newQuality] || qualityMap['auto'];

    try {
      // 1. 断开当前连接
      await stopStream();

      // 2. 更新配置到后端
      await streamingApi.updateQuality(camera?.id, qualitySettings);

      // 3. 重新建立连接（使用新画质）
      await startStream();

      // 4. 持久化到 localStorage
      localStorage.setItem(`videoWallQuality_${camera?.id}`, newQuality);
    } catch (err) {
      console.error('画质切换失败:', err);
      // 恢复旧画质
      setCurrentQuality(previousQuality);
      throw err;
    }
  }, [camera, currentQuality, stopStream, startStream]);

  // 切换全屏
  const toggleFullscreen = useCallback(() => {
    if (!document.fullscreenElement) {
      videoRef.current?.parentElement?.requestFullscreen();
      setIsFullscreen(true);
    } else {
      document.exitFullscreen();
      setIsFullscreen(false);
    }
  }, []);

  // 切换静音
  const toggleMute = useCallback(() => {
    if (videoRef.current) {
      videoRef.current.muted = !videoRef.current.muted;
      setIsMuted(videoRef.current.muted);
    }
  }, []);

  // 开始/停止录像
  const toggleRecording = useCallback(async () => {
    try {
      if (isRecording) {
        await streamingApi.stopRecording(camera?.id);
      } else {
        await streamingApi.startRecording(camera?.id);
      }
      setIsRecording(!isRecording);
    } catch (err) {
      console.error('录像控制失败:', err);
    }
  }, [camera, isRecording]);

  // 重新加载（手动重试）
  const handleRetry = useCallback(() => {
    isReconnectingRef.current = false;
    resetRetry();
    updateConnectionState(ConnectionState.CONNECTING);
    startStream();
  }, [startStream, resetRetry, updateConnectionState]);

  // 组件挂载时启动流
  useEffect(() => {
    if (camera && camera.id && autoPlay) {
      startStream();
    }
    
    return () => {
      cleanup();
    };
  }, [camera, autoPlay, startStream, cleanup]);

  // 质量变化时重新加载
  useEffect(() => {
    if (camera && camera.id && currentQuality !== quality) {
      handleQualityChange(quality);
    }
  }, [quality]);

  // 渲染视频播放器
  const renderVideoPlayer = () => {
    const isConnecting = loading || streamStatus === 'connecting' || streamStatus === 'reconnecting';
    const isFailed = error || streamStatus === 'failed';
    const isDisconnected = streamStatus === 'disconnected';
    const isReconnecting = isReconnectingRef.current;
    
    return (
      <div 
        style={{ 
          position: 'relative',
          width: '100%',
          height: '100%',
          minHeight: '200px',
          backgroundColor: '#0a0a0a',
          borderRadius: '6px',
          overflow: 'hidden'
        }}
      >
        {/* 视频元素 - 始终渲染，但控制可见性 */}
        <video
          ref={videoRef}
          autoPlay
          playsInline
          muted={isMuted}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'contain',
            opacity: (isConnecting || isFailed) ? 0.3 : 1,
            transition: 'opacity 0.3s ease'
          }}
        />

        {/* 加载中状态 - 叠加层，不闪烁 */}
        {isConnecting && (
          <div style={{
            position: 'absolute',
            inset: 0,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            background: 'rgba(10, 10, 10, 0.85)',
            gap: '12px'
          }}>
            <Spin size="large" tip={<span style={{ color: '#fff' }}>连接中...</span>} />
            {isReconnecting && (
              <span style={{ color: '#888', fontSize: '12px' }}>
                正在重连...
              </span>
            )}
          </div>
        )}

        {/* 断开连接状态 - 温和提示 */}
        {isDisconnected && !isFailed && (
          <div style={{
            position: 'absolute',
            inset: 0,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            background: 'rgba(10, 10, 10, 0.7)',
            gap: '12px'
          }}>
            <WifiOutlined style={{ fontSize: '32px', color: '#faad14' }} />
            <span style={{ color: '#faad14', fontSize: '14px' }}>连接中断，正在重连...</span>
          </div>
        )}

        {/* 失败状态 - 优雅的错误展示 */}
        {isFailed && (
          <div style={{
            position: 'absolute',
            inset: 0,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            background: 'rgba(10, 10, 10, 0.85)',
            gap: '16px'
          }}>
            <div style={{
              width: '64px',
              height: '64px',
              borderRadius: '50%',
              background: 'rgba(255, 77, 79, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <WarningOutlined style={{ fontSize: '28px', color: '#ff4d4f' }} />
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{ color: '#fff', fontSize: '14px', marginBottom: '4px' }}>连接失败</div>
              <div style={{ color: '#888', fontSize: '12px', maxWidth: '200px' }}>
                {error?.split(':').pop()?.trim() || '请检查网络连接'}
              </div>
            </div>
            <Button 
              type="primary" 
              icon={<ReloadOutlined />} 
              onClick={handleRetry}
              loading={loading}
            >
              重试连接
            </Button>
          </div>
        )}

        {/* 连接状态徽章 - 仅在正常播放时显示 */}
        {!isFailed && !isConnecting && (
          <ConnectionStatusBadge cameraId={camera?.id} onRetry={handleRetry} />
        )}

        {/* LIVE 标识 - 仅在正常播放时显示 */}
        {!isFailed && !isConnecting && streamStatus === 'connected' && (
          <div style={{
            position: 'absolute',
            top: '8px',
            left: '8px',
            background: 'rgba(255,0,0,0.7)',
            color: 'white',
            padding: '2px 8px',
            borderRadius: '4px',
            fontSize: '12px',
            fontWeight: 'bold'
          }}>
            LIVE
          </div>
        )}
        
        {/* 录像标识 */}
        {isRecording && (
          <div style={{
            position: 'absolute',
            top: '8px',
            right: '8px',
            background: 'rgba(255,0,0,0.7)',
            color: 'white',
            padding: '2px 8px',
            borderRadius: '4px',
            fontSize: '12px',
            display: 'flex',
            alignItems: 'center',
            gap: '4px'
          }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#fff' }} />
            REC
          </div>
        )}
        
        {/* 帧率信息 */}
        {stats.framerate && !isFailed && (
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
        )}
      </div>
    );
  };

  // 渲染控制栏
  const renderControls = () => {
    const controlItems = [
      {
        key: 'play',
        icon: streamStatus === 'paused' ? <PlayCircleOutlined /> : <PauseOutlined />,
        label: streamStatus === 'paused' ? '恢复' : '暂停',
        onClick: streamStatus === 'paused' ? resumeStream : pauseStream
      },
      {
        key: 'mute',
        icon: isMuted ? <MutedOutlined /> : <SoundOutlined />,
        label: isMuted ? '取消静音' : '静音',
        onClick: toggleMute
      },
      {
        key: 'fullscreen',
        icon: isFullscreen ? <FullscreenExitOutlined /> : <FullscreenOutlined />,
        label: isFullscreen ? '退出全屏' : '全屏',
        onClick: toggleFullscreen
      },
      {
        key: 'record',
        label: isRecording ? '停止录像' : '开始录像',
        onClick: toggleRecording
      },
      {
        key: 'quality',
        label: (
          <Select 
            value={currentQuality} 
            onChange={handleQualityChange}
            style={{ width: 100 }}
            onClick={(e) => e.stopPropagation()}
          >
            <Option value="low">流畅 (480p)</Option>
            <Option value="medium">清晰 (720p)</Option>
            <Option value="high">高清 (1080p)</Option>
            <Option value="auto">自动</Option>
          </Select>
        )
      }
    ];

    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center',
        alignItems: 'center',
        gap: '16px',
        padding: '8px 0'
      }}>
        {controlItems.map(item => (
          <Tooltip key={item.key} title={item.label}>
            <Button 
              type="text" 
              icon={item.icon} 
              onClick={item.onClick}
              style={{ color: '#fff' }}
            />
          </Tooltip>
        ))}
      </div>
    );
  };

  // 未选择摄像头
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

  // 正常播放状态 - 使用叠加层处理所有状态，不再条件渲染
  return (
    <>
      <Card 
        className="video-stream-card"
        style={{ height: '100%' }}
        title={
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>{camera.name}</span>
            {showStats && (
              <div style={{ fontSize: '12px', color: '#666' }}>
                {stats.resolution || 'N/A'} | {stats.bitrate ? `${stats.bitrate}kbps` : 'N/A'} | {stats.latency ? `${stats.latency}ms` : 'N/A'}
              </div>
            )}
          </div>
        }
        extra={
          <Space>
            <Tooltip title="录像管理">
              <Button 
                type="text" 
                icon={<FileSearchOutlined />} 
                size="small"
                onClick={() => setShowRecordingModal(true)}
              />
            </Tooltip>
            <Tooltip title="统计概览">
              <Button 
                type="text" 
                icon={<HistoryOutlined />} 
                size="small"
                onClick={() => setShowStatsOverview(true)}
              />
            </Tooltip>
            <Dropdown 
              menu={{ 
                items: [
                  { key: 'retry', label: '刷新', icon: <ReloadOutlined />, onClick: handleRetry },
                  { key: 'stop', label: '停止', onClick: stopStream }
                ]
              }}
            >
              <Button type="text" icon={<SettingOutlined />} size="small" />
            </Dropdown>
          </Space>
        }
        styles={{ body: { padding: 0 } }}
      >
        {renderVideoPlayer()}
        {showControls && renderControls()}
      </Card>

      {/* 录像管理模态框 */}
      <RecordingManagement
        cameraId={camera?.id}
        cameraName={camera?.name}
        visible={showRecordingModal}
        onClose={() => setShowRecordingModal(false)}
      />

      {/* 统计概览模态框 */}
      <CameraStatisticsOverview
        visible={showStatsOverview}
        onClose={() => setShowStatsOverview(false)}
      />
    </>
  );
};

export default CameraStream;
