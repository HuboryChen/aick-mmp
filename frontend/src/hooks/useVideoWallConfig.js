import { useState, useEffect, useCallback } from 'react';
import { userConfigApi } from '../utils/api';
import { 
  BUILT_IN_PRESETS, 
  isBuiltInPreset, 
  canEditPreset, 
  canDeletePreset 
} from '../components/VideoWall/builtInPresets';

const VIDEO_WALL_CONFIG_KEY = 'VIDEO_WALL_CONFIG';

const defaultConfig = {
  layout: '4',
  quality: '720p',
  selectedCameras: [],
};

const useVideoWallConfig = () => {
  const [config, setConfig] = useState(defaultConfig);
  const [isLoaded, setIsLoaded] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  // 从后端API加载配置
  const loadConfigFromBackend = useCallback(async () => {
    try {
      setIsLoading(true);
      const response = await userConfigApi.getConfig(VIDEO_WALL_CONFIG_KEY);
      if (response.data && response.data.configValue) {
        const parsed = JSON.parse(response.data.configValue);
        return {
          layout: parsed.layout || defaultConfig.layout,
          quality: parsed.quality || defaultConfig.quality,
          selectedCameras: Array.isArray(parsed.selectedCameras) ? parsed.selectedCameras : [],
        };
      }
    } catch (error) {
      // 配置不存在或加载失败，使用本地存储作为备份
      console.debug('Backend config not found, trying localStorage');
      try {
        const stored = localStorage.getItem(VIDEO_WALL_CONFIG_KEY);
        if (stored) {
          return JSON.parse(stored);
        }
      } catch (e) {
        console.error('LocalStorage config parse failed:', e);
      }
    } finally {
      setIsLoading(false);
    }
    return null;
  }, []);

  // 加载配置
  useEffect(() => {
    const loadConfig = async () => {
      const loadedConfig = await loadConfigFromBackend();
      if (loadedConfig) {
        setConfig(loadedConfig);
      }
      setIsLoaded(true);
    };
    loadConfig();
  }, [loadConfigFromBackend]);

  // 保存配置到后端API，同时备份到localStorage
  const saveConfig = useCallback(async (newConfig) => {
    const updated = { ...config, ...newConfig };
    
    // 先保存到本地，确保响应式更新
    setConfig(updated);
    
    // 备份到localStorage
    try {
      localStorage.setItem(VIDEO_WALL_CONFIG_KEY, JSON.stringify(updated));
    } catch (e) {
      console.error('LocalStorage save failed:', e);
    }
    
    // 异步保存到后端
    try {
      await userConfigApi.saveConfig(VIDEO_WALL_CONFIG_KEY, JSON.stringify(updated));
    } catch (error) {
      console.error('Backend config save failed:', error);
      // 后端保存失败不影响本地使用
    }
  }, [config]);

  // 更新布局
  const setLayout = useCallback((layout) => {
    saveConfig({ layout });
  }, [saveConfig]);

  // 更新画质
  const setQuality = useCallback((quality) => {
    saveConfig({ quality });
  }, [saveConfig]);

  // 更新选中的摄像头
  const setSelectedCameras = useCallback((selectedCameras) => {
    saveConfig({ selectedCameras });
  }, [saveConfig]);

  // 重置配置
  const resetConfig = useCallback(async () => {
    localStorage.removeItem(VIDEO_WALL_CONFIG_KEY);
    setConfig(defaultConfig);
    
    try {
      await userConfigApi.deleteConfig(VIDEO_WALL_CONFIG_KEY);
    } catch (error) {
      console.error('Backend config delete failed:', error);
    }
  }, []);

  // 获取内置预设列表
  const getBuiltInPresets = useCallback(() => {
    return BUILT_IN_PRESETS;
  }, []);

  // 获取所有预设（内置 + 用户自定义）
  const getAllPresets = useCallback((userPresets = []) => {
    return [...BUILT_IN_PRESETS, ...userPresets];
  }, []);

  return {
    config,
    isLoaded,
    isLoading,
    setLayout,
    setQuality,
    setSelectedCameras,
    saveConfig,
    resetConfig,
    // 预设相关
    builtInPresets: BUILT_IN_PRESETS,
    getBuiltInPresets,
    getAllPresets,
    isBuiltInPreset,
    canEditPreset,
    canDeletePreset,
  };
};

export default useVideoWallConfig;
export { useVideoWallConfig };
