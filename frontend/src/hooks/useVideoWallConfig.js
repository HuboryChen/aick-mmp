/**
 * 视频墙配置 Hook
 *
 * 管理视频墙的布局、画质、码率、摄像头选择等配置状态，
 * 提供 localStorage 同步和数据库持久化功能。
 *
 * 注意：本 Hook 不处理预设相关的状态或逻辑。
 */

import { useState, useCallback, useRef, useEffect } from 'react';
import Cookies from 'js-cookie';
import { videoWallConfigApi } from '../api/videoWallConfig';

// ==================== 常量与辅助函数 ====================

const VIDEO_WALL_CONFIG_KEY = 'VIDEO_WALL_CONFIG';

const DEBOUNCE_DELAY = 500;

const DEFAULT_CONFIG = {
  layout: '4',
  quality: '720p',
  bitrate: 2048,
  cameraIds: [],
  savedAt: 0,
};

const isAuthenticated = () => {
  const token = Cookies.get('token');
  return !!token;
};

// ==================== Hook ====================

const useVideoWallConfig = () => {
  const [config, setConfig] = useState(DEFAULT_CONFIG);
  const [isLoaded, setIsLoaded] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const debounceTimerRef = useRef(null);

  // 清理定时器
  useEffect(() => {
    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, []);

  // ==================== localStorage 同步 ====================

  const saveToLocalStorage = useCallback((newConfig) => {
    try {
      localStorage.setItem(VIDEO_WALL_CONFIG_KEY, JSON.stringify({
        ...newConfig,
        savedAt: Date.now(),
      }));
    } catch (e) {
      console.error('localStorage save failed:', e);
    }
  }, []);

  const loadFromLocalStorage = useCallback(() => {
    try {
      const stored = localStorage.getItem(VIDEO_WALL_CONFIG_KEY);
      if (stored) {
        return JSON.parse(stored);
      }
    } catch (e) {
      console.error('localStorage load failed:', e);
    }
    return null;
  }, []);

  // ==================== 数据库同步 ====================

  const loadFromDatabase = useCallback(async () => {
    try {
      const response = await videoWallConfigApi.getPreferences();
      return response.data;
    } catch (error) {
      console.debug('Failed to load from database:', error);
      return null;
    }
  }, []);

  const debouncedSaveToDatabase = useCallback((newConfig) => {
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }
    debounceTimerRef.current = setTimeout(async () => {
      try {
        await videoWallConfigApi.updatePreferences({
          layout: newConfig.layout,
          quality: newConfig.quality,
          bitrate: newConfig.bitrate,
          cameraIds: newConfig.cameraIds,
        });
        console.debug('Preferences saved to database');
      } catch (error) {
        console.error('Failed to save preferences to database:', error);
        setError('保存到服务器失败');
      }
    }, DEBOUNCE_DELAY);
  }, []);

  const saveToDatabaseImmediately = useCallback(async (newConfig) => {
    try {
      await videoWallConfigApi.updatePreferences({
        layout: newConfig.layout,
        quality: newConfig.quality,
        bitrate: newConfig.bitrate,
        cameraIds: newConfig.cameraIds,
      });
      console.debug('Preferences saved to database immediately');
    } catch (error) {
      console.error('Failed to save preferences to database:', error);
      setError('保存到服务器失败');
    }
  }, []);

  // ==================== 配置保存操作 ====================

  const saveConfig = useCallback((newConfig) => {
    setConfig(newConfig);
    saveToLocalStorage(newConfig);
    if (isAuthenticated()) {
      debouncedSaveToDatabase(newConfig);
    }
  }, [saveToLocalStorage, debouncedSaveToDatabase]);

  const saveConfigImmediately = useCallback(async (newConfig) => {
    setConfig(newConfig);
    saveToLocalStorage(newConfig);
    if (isAuthenticated()) {
      await saveToDatabaseImmediately(newConfig);
    }
  }, [saveToLocalStorage, saveToDatabaseImmediately]);

  // ==================== 返回值 ====================

  return {
    // 状态
    config,
    isLoaded,
    isLoading,
    error,

    // 内部 setter（供组合 Hook 使用）
    setConfig,
    setIsLoaded,
    setIsLoading,
    setError,

    // 配置操作
    saveConfig,
    saveConfigImmediately,

    // localStorage 同步
    saveToLocalStorage,
    loadFromLocalStorage,

    // 数据库同步
    loadFromDatabase,
    saveToDatabaseImmediately,
    debouncedSaveToDatabase,

    // 常量
    DEFAULT_CONFIG,
  };
};

export default useVideoWallConfig;
export { useVideoWallConfig, isAuthenticated };
