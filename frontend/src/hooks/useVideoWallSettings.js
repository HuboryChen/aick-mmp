/**
 * 视频墙设置 Hook（组合 Hook）
 *
 * 组合 useVideoWallConfig 和 useVideoWallPresets，提供统一的视频墙设置管理接口。
 * 协调配置与预设的加载流程，处理跨域状态更新（如配置变更时清除预设激活状态）。
 *
 * 加载流程: 数据库 -> localStorage -> 内置预设
 * 保存流程: localStorage(立即) -> 数据库(防抖延迟500ms)
 */

import { useState, useEffect, useCallback } from 'react';
import { videoWallConfigApi } from '../api/videoWallConfig';
import { BUILT_IN_PRESETS } from '../components/VideoWall/builtInPresets';
import useVideoWallConfig, { isAuthenticated } from './useVideoWallConfig';
import useVideoWallPresets from './useVideoWallPresets';

// ==================== 辅助函数 ====================

const getDefaultPreset = () => {
  return BUILT_IN_PRESETS.find(p => p.isDefault) || BUILT_IN_PRESETS[0];
};

// ==================== Hook ====================

const useVideoWallSettings = () => {
  // 子 Hook
  const configHook = useVideoWallConfig();
  const presetHook = useVideoWallPresets();

  // 组合层状态（覆盖子 Hook 的 isLoaded / isLoading / error）
  const [isLoaded, setIsLoaded] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  // ==================== 加载配置 ====================

  const loadPreferences = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    try {
      if (isAuthenticated()) {
        // 已认证: 从数据库加载，然后与 localStorage 合并（localStorage 始终是最新的）
        const dbPrefs = await configHook.loadFromDatabase();
        const localPrefs = configHook.loadFromLocalStorage();

        if (dbPrefs || localPrefs) {
          // DB 数据作为基础
          const mergedConfig = {
            layout: dbPrefs?.layout || configHook.DEFAULT_CONFIG.layout,
            quality: dbPrefs?.quality || configHook.DEFAULT_CONFIG.quality,
            bitrate: dbPrefs?.bitrate ?? configHook.DEFAULT_CONFIG.bitrate,
            cameraIds: dbPrefs?.cameraIds || [],
          };

          // localStorage 覆盖（始终是用户最近的操作）
          if (localPrefs) {
            mergedConfig.layout = localPrefs.layout || mergedConfig.layout;
            mergedConfig.quality = localPrefs.quality || mergedConfig.quality;
            mergedConfig.bitrate = localPrefs.bitrate ?? mergedConfig.bitrate;
            mergedConfig.cameraIds = localPrefs.cameraIds || mergedConfig.cameraIds;
          }

          configHook.setConfig(mergedConfig);

          // 加载预设
          const userPresets = await presetHook.loadUserPresetsFromDatabase();
          presetHook.setPresets(userPresets);
          presetHook.savePresetsToLocalStorage(userPresets);

          // 设置激活的预设 (如果有 lastPresetId)
          if (dbPrefs?.lastPresetId) {
            presetHook.setActivePresetId(dbPrefs.lastPresetId);
          }

          setIsLoaded(true);
          return;
        }
      }

      // 未认证或数据库加载失败: 从 localStorage 加载
      const localPrefs = configHook.loadFromLocalStorage();
      if (localPrefs) {
        configHook.setConfig({
          layout: localPrefs.layout || configHook.DEFAULT_CONFIG.layout,
          quality: localPrefs.quality || configHook.DEFAULT_CONFIG.quality,
          bitrate: localPrefs.bitrate || configHook.DEFAULT_CONFIG.bitrate,
          cameraIds: localPrefs.cameraIds || [],
        });

        // 加载本地预设
        const localPresets = presetHook.loadPresetsFromLocalStorage();
        presetHook.setPresets(localPresets);
      } else {
        // 都没有: 使用内置默认预设
        const defaultPreset = getDefaultPreset();
        configHook.setConfig({
          layout: defaultPreset.layout,
          quality: defaultPreset.quality,
          bitrate: defaultPreset.bitrate,
          cameraIds: defaultPreset.cameraIds || [],
        });
      }

      setIsLoaded(true);
    } catch (err) {
      console.error('Failed to load preferences:', err);
      setError('加载配置失败');

      // 出错时使用内置默认预设
      const defaultPreset = getDefaultPreset();
      configHook.setConfig({
        layout: defaultPreset.layout,
        quality: defaultPreset.quality,
        bitrate: defaultPreset.bitrate,
        cameraIds: defaultPreset.cameraIds || [],
      });

      setIsLoaded(true);
    } finally {
      setIsLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [configHook.loadFromDatabase, configHook.loadFromLocalStorage, configHook.DEFAULT_CONFIG,
      configHook.setConfig, presetHook.loadUserPresetsFromDatabase, presetHook.setPresets,
      presetHook.savePresetsToLocalStorage, presetHook.setActivePresetId,
      presetHook.loadPresetsFromLocalStorage]);

  // 初始化加载（仅挂载时执行一次）
  useEffect(() => {
    loadPreferences();

    // 清理定时器（由 configHook 负责）
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ==================== 跨域操作：配置变更时同时清除预设激活状态 ====================
  // 以下 useCallback 正确列出了所有实际依赖项，configHook/presetHook 属性已覆盖，
  // 无需将父对象加入 deps（它们会随每次渲染重建，导致无限循环）
  /* eslint-disable react-hooks/exhaustive-deps */

  /**
   * 更新布局（手动调整取消预设激活状态）
   */
  const setLayout = useCallback((layout) => {
    const newConfig = { ...configHook.config, layout };
    configHook.saveConfig(newConfig);
    presetHook.setActivePresetId(null);
  }, [configHook.config, configHook.saveConfig, presetHook.setActivePresetId]);

  /**
   * 更新画质（手动调整取消预设激活状态）
   */
  const setQuality = useCallback((quality) => {
    const newConfig = { ...configHook.config, quality };
    configHook.saveConfig(newConfig);
    presetHook.setActivePresetId(null);
  }, [configHook.config, configHook.saveConfig, presetHook.setActivePresetId]);

  /**
   * 更新码率（手动调整取消预设激活状态）
   */
  const setBitrate = useCallback((bitrate) => {
    const newConfig = { ...configHook.config, bitrate };
    configHook.saveConfig(newConfig);
    presetHook.setActivePresetId(null);
  }, [configHook.config, configHook.saveConfig, presetHook.setActivePresetId]);

  /**
   * 更新选中的摄像头（手动调整取消预设激活状态）
   */
  const setSelectedCameras = useCallback((cameraIds) => {
    const newConfig = { ...configHook.config, cameraIds };
    configHook.saveConfig(newConfig);
    presetHook.setActivePresetId(null);
  }, [configHook.config, configHook.saveConfig, presetHook.setActivePresetId]);

  /**
   * 重置为默认配置
   */
  const resetToDefaults = useCallback(() => {
    const defaultPreset = getDefaultPreset();
    const newConfig = {
      layout: defaultPreset.layout,
      quality: defaultPreset.quality,
      bitrate: defaultPreset.bitrate,
      cameraIds: defaultPreset.cameraIds || [],
    };

    configHook.setConfig(newConfig);
    presetHook.setActivePresetId(null);
    configHook.saveToLocalStorage(newConfig);

    // 通知后端重置
    if (isAuthenticated()) {
      videoWallConfigApi.updatePreferences(newConfig).catch(err => {
        console.error('Failed to reset preferences:', err);
      });
    }
  }, [configHook.setConfig, configHook.saveToLocalStorage, presetHook.setActivePresetId]);

  // ==================== 跨域操作：应用预设（同时更新配置） ====================

  /**
   * 应用预设
   */
  const applyPreset = useCallback(async (presetId) => {
    setIsLoading(true);
    try {
      let preset;

      // 先在用户预设中查找
      preset = presetHook.presets.find(p => p.id === presetId);

      // 如果没找到，在内置预设中查找
      if (!preset) {
        preset = BUILT_IN_PRESETS.find(p => p.id === presetId);
      }

      if (!preset) {
        throw new Error('预设不存在');
      }

      // 更新配置
      const newConfig = {
        layout: preset.layout,
        quality: preset.quality,
        bitrate: preset.bitrate,
        cameraIds: preset.cameraIds || [],
      };

      configHook.saveConfig(newConfig);
      presetHook.setActivePresetId(presetId);

      // 如果是用户预设，通知后端
      if (presetHook.presets.find(p => p.id === presetId)) {
        await videoWallConfigApi.applyPreset(presetId);
      }
    } catch (error) {
      console.error('Failed to apply preset:', error);
      setError('应用预设失败');
    } finally {
      setIsLoading(false);
    }
  }, [presetHook.presets, configHook.saveConfig, presetHook.setActivePresetId]);

  // ==================== 创建预设（需要当前配置作为回退） ====================

  /**
   * 创建新预设（使用当前配置作为空缺字段的回退）
   */
  const createPreset = useCallback(async (name, presetConfig) => {
    setIsLoading(true);
    setError(null);
    try {
      const presetData = {
        presetName: name,
        layout: presetConfig?.layout || configHook.config.layout,
        quality: presetConfig?.quality || configHook.config.quality,
        bitrate: presetConfig?.bitrate || configHook.config.bitrate,
        cameraIds: presetConfig?.cameraIds || configHook.config.cameraIds || [],
        isDefault: false,
      };

      const response = await videoWallConfigApi.createPreset(presetData);
      const newPreset = response.data;

      const newPresets = [...presetHook.presets, newPreset];
      presetHook.setPresets(newPresets);
      presetHook.savePresetsToLocalStorage(newPresets);

      return newPreset;
    } catch (error) {
      console.error('Failed to create preset:', error);
      setError('创建预设失败');
      throw error;
    } finally {
      setIsLoading(false);
    }
  }, [configHook.config, presetHook.presets, presetHook.setPresets, presetHook.savePresetsToLocalStorage]);

  // ==================== 转发函数（预设操作中不需要跨域处理的） ====================

  /**
   * 更新预设
   */
  const updatePreset = useCallback(async (presetId, updates) => {
    setIsLoading(true);
    try {
      return await presetHook.updatePreset(presetId, updates);
    } finally {
      setIsLoading(false);
    }
  }, [presetHook.updatePreset]);

  /**
   * 删除预设
   */
  const deletePreset = useCallback(async (presetId) => {
    setIsLoading(true);
    try {
      return await presetHook.deletePreset(presetId);
    } finally {
      setIsLoading(false);
    }
  }, [presetHook.deletePreset]);

  /**
   * 设为默认预设
   */
  const setAsDefaultPreset = useCallback(async (presetId) => {
    setIsLoading(true);
    try {
      return await presetHook.setAsDefaultPreset(presetId);
    } finally {
      setIsLoading(false);
    }
  }, [presetHook.setAsDefaultPreset]);

  /**
   * 重新排序预设
   */
  const reorderPresets = useCallback(async (newOrder) => {
    setIsLoading(true);
    try {
      return await presetHook.reorderPresets(newOrder);
    } finally {
      setIsLoading(false);
    }
  }, [presetHook.reorderPresets]);

  // ==================== 转发函数（预设操作中不需要跨域处理的） ====================

  /**
   * 获取所有预设（内置 + 用户自定义）
   */
  const getAllPresets = useCallback(() => {
    return [...BUILT_IN_PRESETS, ...presetHook.presets];
  }, [presetHook.presets]);

  /* eslint-enable react-hooks/exhaustive-deps */

  // ==================== 返回值 ====================

  return {
    // 配置状态
    config: configHook.config,
    isLoaded,
    isLoading,
    error,

    // 配置操作
    saveConfig: configHook.saveConfig,
    saveConfigImmediately: configHook.saveConfigImmediately,
    setLayout,
    setQuality,
    setBitrate,
    setSelectedCameras,
    resetToDefaults,

    // 预设状态
    presets: presetHook.presets,
    builtInPresets: BUILT_IN_PRESETS,
    activePresetId: presetHook.activePresetId,

    // 预设操作
    getAllPresets,
    applyPreset,
    createPreset,
    updatePreset,
    deletePreset,
    setAsDefaultPreset,
    reorderPresets,

    // 预设辅助函数
    isBuiltInPreset: presetHook.isBuiltInPreset,
    canEditPreset: presetHook.canEditPreset,
    canDeletePreset: presetHook.canDeletePreset,

    // 重新加载
    reload: loadPreferences,
  };
};

export default useVideoWallSettings;
export { useVideoWallSettings };
