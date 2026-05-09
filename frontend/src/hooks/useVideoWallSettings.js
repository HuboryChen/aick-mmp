/**
 * 视频墙设置 Hook
 * 
 * 提供视频墙配置的完整状态管理和预设操作功能
 * 
 * 加载流程: 数据库 -> localStorage -> 内置预设
 * 保存流程: localStorage(立即) -> 数据库(防抖延迟500ms)
 */

import { useState, useEffect, useCallback, useRef } from 'react';
import Cookies from 'js-cookie';
import { videoWallConfigApi } from '../api/videoWallConfig';
import { 
  BUILT_IN_PRESETS, 
  isBuiltInPreset, 
  canEditPreset, 
  canDeletePreset 
} from '../components/VideoWall/builtInPresets';

// localStorage 键名
const VIDEO_WALL_CONFIG_KEY = 'VIDEO_WALL_CONFIG';
const VIDEO_WALL_PRESETS_KEY = 'VIDEO_WALL_PRESETS';

// 防抖延迟 (毫秒)
const DEBOUNCE_DELAY = 500;

// 默认配置 (四分屏作为默认)
const DEFAULT_CONFIG = {
  layout: '4',
  quality: '720p',
  bitrate: 2048,
  cameraIds: [],
  savedAt: 0,
};

// 获取默认预设 (isDefault: true 的内置预设)
const getDefaultPreset = () => {
  return BUILT_IN_PRESETS.find(p => p.isDefault) || BUILT_IN_PRESETS[0];
};

/**
 * 检查用户是否已认证
 * @returns {boolean}
 */
const isAuthenticated = () => {
  const token = Cookies.get('token');
  return !!token;
};

const useVideoWallSettings = () => {
  // ==================== 状态 ====================
  const [config, setConfig] = useState(DEFAULT_CONFIG);
  const [presets, setPresets] = useState([]);
  const [activePresetId, setActivePresetId] = useState(null);
  const [isLoaded, setIsLoaded] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  // 防抖定时器引用
  const debounceTimerRef = useRef(null);

  // ==================== 辅助函数 ====================

  /**
   * 保存配置到 localStorage
   */
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

  /**
   * 从 localStorage 加载配置
   */
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

  /**
   * 保存用户预设到 localStorage
   */
  const savePresetsToLocalStorage = useCallback((userPresets) => {
    try {
      localStorage.setItem(VIDEO_WALL_PRESETS_KEY, JSON.stringify(userPresets));
    } catch (e) {
      console.error('localStorage presets save failed:', e);
    }
  }, []);

  /**
   * 从 localStorage 加载用户预设
   */
  const loadPresetsFromLocalStorage = useCallback(() => {
    try {
      const stored = localStorage.getItem(VIDEO_WALL_PRESETS_KEY);
      if (stored) {
        return JSON.parse(stored);
      }
    } catch (e) {
      console.error('localStorage presets load failed:', e);
    }
    return [];
  }, []);

  /**
   * 防抖保存到数据库
   */
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

  /**
   * 立即保存到数据库（用于显式用户确认操作，如点击"完成"）
   */
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

  // ==================== 加载配置 ====================

  /**
   * 从数据库加载偏好设置
   */
  const loadFromDatabase = useCallback(async () => {
    try {
      const response = await videoWallConfigApi.getPreferences();
      return response.data;
    } catch (error) {
      console.debug('Failed to load from database:', error);
      return null;
    }
  }, []);

  /**
   * 从数据库加载用户预设
   */
  const loadUserPresetsFromDatabase = useCallback(async () => {
    try {
      const response = await videoWallConfigApi.getPresets();
      return response.data || [];
    } catch (error) {
      console.debug('Failed to load presets from database:', error);
      return [];
    }
  }, []);

  /**
   * 加载所有配置 (按优先级: 数据库 -> localStorage -> 内置预设)
   */
  const loadPreferences = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    try {
      if (isAuthenticated()) {
        // 已认证: 从数据库加载，然后与 localStorage 合并（localStorage 始终是最新的）
        const dbPrefs = await loadFromDatabase();
        const localPrefs = loadFromLocalStorage();

        if (dbPrefs || localPrefs) {
          // DB 数据作为基础
          const mergedConfig = {
            layout: dbPrefs?.layout || DEFAULT_CONFIG.layout,
            quality: dbPrefs?.quality || DEFAULT_CONFIG.quality,
            bitrate: dbPrefs?.bitrate ?? DEFAULT_CONFIG.bitrate,
            cameraIds: dbPrefs?.cameraIds || [],
          };

          // localStorage 覆盖（始终是用户最近的操作）
          if (localPrefs) {
            mergedConfig.layout = localPrefs.layout || mergedConfig.layout;
            mergedConfig.quality = localPrefs.quality || mergedConfig.quality;
            mergedConfig.bitrate = localPrefs.bitrate ?? mergedConfig.bitrate;
            mergedConfig.cameraIds = localPrefs.cameraIds || mergedConfig.cameraIds;
          }

          setConfig(mergedConfig);

          // 加载预设
          const userPresets = await loadUserPresetsFromDatabase();
          setPresets(userPresets);
          savePresetsToLocalStorage(userPresets);

          // 设置激活的预设 (如果有 lastPresetId)
          if (dbPrefs?.lastPresetId) {
            setActivePresetId(dbPrefs.lastPresetId);
          }

          setIsLoaded(true);
          return;
        }
      }

      // 未认证或数据库加载失败: 从 localStorage 加载
      const localPrefs = loadFromLocalStorage();
      if (localPrefs) {
        setConfig({
          layout: localPrefs.layout || DEFAULT_CONFIG.layout,
          quality: localPrefs.quality || DEFAULT_CONFIG.quality,
          bitrate: localPrefs.bitrate || DEFAULT_CONFIG.bitrate,
          cameraIds: localPrefs.cameraIds || [],
        });
        
        // 加载本地预设
        const localPresets = loadPresetsFromLocalStorage();
        setPresets(localPresets);
      } else {
        // 都没有: 使用内置默认预设
        const defaultPreset = getDefaultPreset();
        setConfig({
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
      setConfig({
        layout: defaultPreset.layout,
        quality: defaultPreset.quality,
        bitrate: defaultPreset.bitrate,
        cameraIds: defaultPreset.cameraIds || [],
      });
      
      setIsLoaded(true);
    } finally {
      setIsLoading(false);
    }
  }, [loadFromDatabase, loadUserPresetsFromDatabase, loadFromLocalStorage, loadPresetsFromLocalStorage, savePresetsToLocalStorage]);

  // 初始化加载
  useEffect(() => {
    loadPreferences();
    
    // 清理定时器
    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, []);

  // ==================== 配置操作 ====================

  /**
   * 保存配置 (双重同步: localStorage + 防抖数据库)
   */
  const saveConfig = useCallback((newConfig) => {
    // 更新本地状态
    setConfig(newConfig);

    // 立即保存到 localStorage
    saveToLocalStorage(newConfig);

    // 防抖保存到数据库
    if (isAuthenticated()) {
      debouncedSaveToDatabase(newConfig);
    }
  }, [saveToLocalStorage, debouncedSaveToDatabase]);

  /**
   * 立即保存配置（用于用户显式确认操作，如点击"完成"）
   * 同步 localStorage + 即时 DB 写入（无防抖）
   */
  const saveConfigImmediately = useCallback(async (newConfig) => {
    // 更新本地状态
    setConfig(newConfig);

    // 立即保存到 localStorage
    saveToLocalStorage(newConfig);

    // 即时保存到数据库
    if (isAuthenticated()) {
      await saveToDatabaseImmediately(newConfig);
    }
  }, [saveToLocalStorage, saveToDatabaseImmediately]);

  /**
   * 更新布局
   * 手动调整会取消预设激活状态
   */
  const setLayout = useCallback((layout) => {
    const newConfig = { ...config, layout };
    saveConfig(newConfig);
    setActivePresetId(null); // 手动调整，取消预设激活
  }, [config, saveConfig]);

  /**
   * 更新画质
   */
  const setQuality = useCallback((quality) => {
    const newConfig = { ...config, quality };
    saveConfig(newConfig);
    setActivePresetId(null);
  }, [config, saveConfig]);

  /**
   * 更新码率
   */
  const setBitrate = useCallback((bitrate) => {
    const newConfig = { ...config, bitrate };
    saveConfig(newConfig);
    setActivePresetId(null);
  }, [config, saveConfig]);

  /**
   * 更新选中的摄像头
   */
  const setSelectedCameras = useCallback((cameraIds) => {
    const newConfig = { ...config, cameraIds };
    saveConfig(newConfig);
    setActivePresetId(null);
  }, [config, saveConfig]);

  // ==================== 预设操作 ====================

  /**
   * 获取所有预设 (内置 + 用户自定义)
   */
  const getAllPresets = useCallback(() => {
    return [...BUILT_IN_PRESETS, ...presets];
  }, [presets]);

  /**
   * 应用预设
   */
  const applyPreset = useCallback(async (presetId) => {
    setIsLoading(true);
    try {
      let preset;
      
      // 先在用户预设中查找
      preset = presets.find(p => p.id === presetId);
      
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
      
      // 更新状态
      setConfig(newConfig);
      setActivePresetId(presetId);
      saveToLocalStorage(newConfig);

      // 如果是用户预设，通知后端
      if (presets.find(p => p.id === presetId)) {
        await videoWallConfigApi.applyPreset(presetId);
      }

    } catch (error) {
      console.error('Failed to apply preset:', error);
      setError('应用预设失败');
    } finally {
      setIsLoading(false);
    }
  }, [presets, saveToLocalStorage]);

  /**
   * 创建新预设
   */
  const createPreset = useCallback(async (name, presetConfig) => {
    setIsLoading(true);
    try {
      const presetData = {
        presetName: name,
        layout: presetConfig?.layout || config.layout,
        quality: presetConfig?.quality || config.quality,
        bitrate: presetConfig?.bitrate || config.bitrate,
        cameraIds: presetConfig?.cameraIds || config.cameraIds,
        isDefault: false,
      };

      const response = await videoWallConfigApi.createPreset(presetData);
      const newPreset = response.data;
      
      // 更新预设列表
      const newPresets = [...presets, newPreset];
      setPresets(newPresets);
      savePresetsToLocalStorage(newPresets);

      return newPreset;
    } catch (error) {
      console.error('Failed to create preset:', error);
      setError('创建预设失败');
      throw error;
    } finally {
      setIsLoading(false);
    }
  }, [config, presets, savePresetsToLocalStorage]);

  /**
   * 更新预设
   */
  const updatePreset = useCallback(async (presetId, updates) => {
    setIsLoading(true);
    try {
      // 确保只能更新用户预设
      const existingPreset = presets.find(p => p.id === presetId);
      if (!existingPreset) {
        throw new Error('预设不存在或不可编辑');
      }

      const response = await videoWallConfigApi.updatePreset(presetId, updates);
      const updatedPreset = response.data;
      
      // 更新预设列表
      const newPresets = presets.map(p => 
        p.id === presetId ? updatedPreset : p
      );
      setPresets(newPresets);
      savePresetsToLocalStorage(newPresets);

      return updatedPreset;
    } catch (error) {
      console.error('Failed to update preset:', error);
      setError('更新预设失败');
      throw error;
    } finally {
      setIsLoading(false);
    }
  }, [presets, savePresetsToLocalStorage]);

  /**
   * 删除预设
   */
  const deletePreset = useCallback(async (presetId) => {
    setIsLoading(true);
    try {
      // 确保只能删除用户预设
      const existingPreset = presets.find(p => p.id === presetId);
      if (!existingPreset) {
        throw new Error('预设不存在或不可删除');
      }

      await videoWallConfigApi.deletePreset(presetId);
      
      // 更新预设列表
      const newPresets = presets.filter(p => p.id !== presetId);
      setPresets(newPresets);
      savePresetsToLocalStorage(newPresets);

      // 如果删除的是当前激活的预设，重置激活状态
      if (activePresetId === presetId) {
        setActivePresetId(null);
      }
    } catch (error) {
      console.error('Failed to delete preset:', error);
      setError('删除预设失败');
      throw error;
    } finally {
      setIsLoading(false);
    }
  }, [presets, activePresetId, savePresetsToLocalStorage]);

  /**
   * 设为默认预设
   */
  const setAsDefaultPreset = useCallback(async (presetId) => {
    setIsLoading(true);
    try {
      // 确保只能设置用户预设为默认
      const existingPreset = presets.find(p => p.id === presetId);
      if (!existingPreset) {
        throw new Error('预设不存在或不可编辑');
      }

      await videoWallConfigApi.setDefaultPreset(presetId);
      
      // 更新预设列表中的默认状态
      const newPresets = presets.map(p => ({
        ...p,
        isDefault: p.id === presetId,
      }));
      setPresets(newPresets);
      savePresetsToLocalStorage(newPresets);
    } catch (error) {
      console.error('Failed to set default preset:', error);
      setError('设置默认预设失败');
      throw error;
    } finally {
      setIsLoading(false);
    }
  }, [presets, savePresetsToLocalStorage]);

  /**
   * 重新排序预设
   */
  const reorderPresets = useCallback(async (newOrder) => {
    setIsLoading(true);
    try {
      // newOrder 是新的排序顺序
      const presetIds = newOrder.map(p => p.id || p);
      
      await videoWallConfigApi.reorderPresets(presetIds);
      
      // 更新预设列表顺序
      const reorderedPresets = presetIds.map((id, index) => {
        const preset = presets.find(p => p.id === id);
        return preset ? { ...preset, sortOrder: index } : null;
      }).filter(Boolean);
      
      setPresets(reorderedPresets);
      savePresetsToLocalStorage(reorderedPresets);
    } catch (error) {
      console.error('Failed to reorder presets:', error);
      setError('重新排序失败');
      throw error;
    } finally {
      setIsLoading(false);
    }
  }, [presets, savePresetsToLocalStorage]);

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
    
    setConfig(newConfig);
    setActivePresetId(null);
    saveToLocalStorage(newConfig);
    
    // 通知后端重置
    if (isAuthenticated()) {
      videoWallConfigApi.updatePreferences(newConfig).catch(err => {
        console.error('Failed to reset preferences:', err);
      });
    }
  }, [saveToLocalStorage]);

  // ==================== 辅助函数导出 ====================

  /**
   * 检查预设是否为内置预设
   */
  const checkIsBuiltIn = useCallback((preset) => {
    return isBuiltInPreset(preset);
  }, []);

  /**
   * 检查预设是否可编辑
   */
  const checkCanEdit = useCallback((preset) => {
    return canEditPreset(preset);
  }, []);

  /**
   * 检查预设是否可删除
   */
  const checkCanDelete = useCallback((preset) => {
    return canDeletePreset(preset);
  }, []);

  // ==================== 返回值 ====================

  return {
    // 配置状态
    config,
    isLoaded,
    isLoading,
    error,
    
    // 配置操作
    saveConfig,
    saveConfigImmediately,
    setLayout,
    setQuality,
    setBitrate,
    setSelectedCameras,
    resetToDefaults,
    
    // 预设状态
    presets,
    builtInPresets: BUILT_IN_PRESETS,
    activePresetId,
    
    // 预设操作
    getAllPresets,
    applyPreset,
    createPreset,
    updatePreset,
    deletePreset,
    setAsDefaultPreset,
    reorderPresets,
    
    // 预设辅助函数
    isBuiltInPreset: checkIsBuiltIn,
    canEditPreset: checkCanEdit,
    canDeletePreset: checkCanDelete,
    
    // 重新加载
    reload: loadPreferences,
  };
};

export default useVideoWallSettings;
export { useVideoWallSettings };
