import { useRef, useState, useCallback, useEffect } from 'react';
import { useStreamHealth, ConnectionState } from '../contexts/StreamHealthContext';

// Default reconnect configuration
const DEFAULT_CONFIG = {
  enabled: true,
  maxRetries: 5,
  initialDelayMs: 1000,
  maxDelayMs: 30000,
  jitterFactor: 0.3,
};

/**
 * 指数退避算法计算下次重连延迟
 * @param {number} retryCount - 当前重试次数
 * @param {object} config - 配置对象
 * @returns {number} 延迟毫秒数
 */
export function calculateDelay(retryCount, config = DEFAULT_CONFIG) {
  const { initialDelayMs, maxDelayMs, jitterFactor } = config;
  const base = initialDelayMs * Math.pow(2, retryCount);
  const jitter = base * jitterFactor * (Math.random() * 2 - 1);
  return Math.min(base + jitter, maxDelayMs);
}

/**
 * 根据 WebRTC 统计数据决定恢复画质
 * @param {object} stats - { packetLoss, roundTripTime }
 * @param {string} currentQuality - 当前画质
 * @returns {string} 推荐画质
 */
export function calculateAdaptiveQuality(stats, currentQuality) {
  const qualityMap = {
    '1080p': { level: 4, bitrate: 8192 },
    '720p': { level: 3, bitrate: 4096 },
    '480p': { level: 2, bitrate: 2048 },
    '360p': { level: 1, bitrate: 1024 },
  };

  const { packetLoss = 0, roundTripTime = 0 } = stats;

  // 网络质量优秀：恢复原画质
  if (roundTripTime < 100 && packetLoss < 1) {
    return currentQuality;
  }

  // 网络质量一般：降一级
  if ((roundTripTime >= 100 && roundTripTime < 300) ||
      (packetLoss >= 1 && packetLoss < 3)) {
    const current = qualityMap[currentQuality];
    if (current && current.level > 1) {
      const levels = Object.entries(qualityMap).find(([, v]) => v.level === current.level - 1);
      return levels ? levels[0] : currentQuality;
    }
    return currentQuality;
  }

  // 网络质量差：降两级
  if (roundTripTime >= 300 || packetLoss >= 3) {
    const current = qualityMap[currentQuality];
    if (current && current.level > 2) {
      const levels = Object.entries(qualityMap).find(([, v]) => v.level === current.level - 2);
      return levels ? levels[0] : '360p';
    }
    return '360p';
  }

  return currentQuality;
}

export function useReconnect(cameraId, startStreamFn, stopStreamFn, currentQuality) {
  const {
    connectionState,
    retryCount,
    updateConnectionState,
    incrementRetry,
    resetRetry,
    setError,
    updateHealthMetrics,
  } = useStreamHealth();

  // Try to merge with videoConfig if available
  let config = DEFAULT_CONFIG;
  try {
    // eslint-disable-next-line no-unused-vars
    const videoConfig = require('../config/videoConfig').default;
    if (videoConfig?.stream?.reconnect) {
      config = { ...DEFAULT_CONFIG, ...videoConfig.stream.reconnect };
    }
  } catch (e) {
    // videoConfig not available, use defaults
  }

  const timeoutRef = useRef(null);
  const isReconnectingRef = useRef(false);
  const [isReconnecting, setIsReconnecting] = useState(false);
  const [nextRetryIn, setNextRetryIn] = useState(0);

  const clearReconnectTimeout = useCallback(() => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }
  }, []);

  const collectStats = useCallback(async (pc) => {
    if (!pc) return { packetLoss: 0, roundTripTime: 0, jitter: 0 };
    try {
      const stats = await pc.getStats();
      let packetLoss = 0;
      let roundTripTime = 0;
      let jitter = 0;

      stats.forEach((report) => {
        if (report.type === 'inbound-rtp' && report.kind === 'video') {
          const packetsLost = report.packetsLost || 0;
          const packetsReceived = report.packetsReceived || 0;
          const total = packetsLost + packetsReceived;
          packetLoss = total > 0 ? (packetsLost / total) * 100 : 0;
          jitter = report.jitter || 0;
        }
        if (report.type === 'candidate-pair' && report.state === 'succeeded') {
          roundTripTime = report.currentRoundTripTime ? report.currentRoundTripTime * 1000 : 0;
        }
      });

      updateHealthMetrics({ packetLoss, roundTripTime, jitter });
      return { packetLoss, roundTripTime, jitter };
    } catch (e) {
      console.warn('Failed to collect stats:', e);
      return { packetLoss: 0, roundTripTime: 0, jitter: 0 };
    }
  }, [updateHealthMetrics]);

  const reconnect = useCallback(async (pc) => {
    if (isReconnectingRef.current || !config.enabled) {
      return;
    }

    isReconnectingRef.current = true;
    setIsReconnecting(true);
    updateConnectionState(ConnectionState.RECONNECTING);

    try {
      // 收集 WebRTC 统计信息
      const stats = await collectStats(pc);

      // 计算自适应画质
      const adaptiveQuality = calculateAdaptiveQuality(stats, currentQuality);

      // 递增重试计数
      incrementRetry();

      // 检查是否超过最大重试次数
      if (retryCount >= config.maxRetries) {
        updateConnectionState(ConnectionState.FAILED);
        setError('连接中断，已停止重连，请检查网络或刷新页面');
        isReconnectingRef.current = false;
        setIsReconnecting(false);
        return;
      }

      // 计算延迟并设置倒计时
      const delay = calculateDelay(retryCount, config);
      let remaining = delay;

      const countdownInterval = setInterval(() => {
        remaining -= 1000;
        setNextRetryIn(Math.max(0, remaining));
      }, 1000);

      // 等待退避时间
      await new Promise((resolve) => {
        timeoutRef.current = setTimeout(() => {
          clearInterval(countdownInterval);
          resolve();
        }, delay);
      });

      clearReconnectTimeout();

      // 断开旧连接
      if (stopStreamFn) {
        await stopStreamFn();
      }

      // 使用自适应画质重连
      if (startStreamFn) {
        await startStreamFn(adaptiveQuality);
      }

      // 重连成功
      resetRetry();
      updateConnectionState(ConnectionState.CONNECTED);
      setIsReconnecting(false);
      setNextRetryIn(0);
      isReconnectingRef.current = false;
    } catch (e) {
      console.error('Reconnect failed:', e);
      setError(`重连失败: ${e.message}`);
      isReconnectingRef.current = false;
      setIsReconnecting(false);
    }
  }, [
    config, retryCount, currentQuality, updateConnectionState, incrementRetry,
    resetRetry, setError, collectStats, startStreamFn, stopStreamFn,
    clearReconnectTimeout,
  ]);

  const stopReconnect = useCallback(() => {
    clearReconnectTimeout();
    isReconnectingRef.current = false;
    setIsReconnecting(false);
    setNextRetryIn(0);
    updateConnectionState(ConnectionState.DISCONNECTED);
  }, [clearReconnectTimeout, updateConnectionState]);

  // 组件卸载时清理
  useEffect(() => {
    return () => {
      clearReconnectTimeout();
    };
  }, [clearReconnectTimeout]);

  return {
    reconnect,
    stopReconnect,
    retryCount,
    maxRetries: config.maxRetries,
    isReconnecting,
    nextRetryIn,
  };
}

export default useReconnect;
