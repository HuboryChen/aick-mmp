import { useState, useEffect, useCallback } from 'react';
import { settingsApi } from '../utils/api';

/**
 * 获取并管理系统全局设置
 * 主要用于 Header/Sidebar 等需要展示系统名称等配置的组件
 */
const useSystemSettings = () => {
  const [settings, setSettings] = useState({
    systemName: '',
    maxConcurrentStreams: 50,
    defaultResolution: '1280x720',
    defaultFrameRate: 25,
  });
  const [loading, setLoading] = useState(false);

  const fetchSettings = useCallback(async () => {
    setLoading(true);
    try {
      const response = await settingsApi.getSettings();
      if (response.data) {
        setSettings(prev => ({ ...prev, ...response.data }));
      }
    } catch (error) {
      // 使用默认值，不阻断渲染
      console.warn('获取系统设置失败，使用默认值:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSettings();
  }, [fetchSettings]);

  /**
   * 系统名称：优先使用设置值，为空时返回默认值
   */
  const systemName = settings.systemName && settings.systemName.trim() !== ''
    ? settings.systemName.trim()
    : 'AICK视频监控平台';

  return {
    settings,
    loading,
    systemName,
    refetch: fetchSettings,
  };
};

export default useSystemSettings;
export { useSystemSettings };
