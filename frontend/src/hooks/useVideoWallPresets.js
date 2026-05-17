/**
 * 视频墙预设 Hook
 *
 * 管理用户自定义预设和内置预设的增删改查、排序、应用等操作，
 * 提供 localStorage 同步和数据库持久化功能。
 *
 * 注意：本 Hook 不处理配置状态，应用预设时需由调用方同步更新配置状态。
 */

import { useState, useCallback } from 'react';
import { videoWallConfigApi } from '../api/videoWallConfig';
import {
  BUILT_IN_PRESETS,
  isBuiltInPreset,
  canEditPreset,
  canDeletePreset,
} from '../components/VideoWall/builtInPresets';

// ==================== 常量 ====================

const VIDEO_WALL_PRESETS_KEY = 'VIDEO_WALL_PRESETS';

const PRESET_FALLBACK_CONFIG = {
  layout: '4',
  quality: '720p',
  bitrate: 2048,
  cameraIds: [],
};

// ==================== Hook ====================

const useVideoWallPresets = () => {
  const [presets, setPresets] = useState([]);
  const [activePresetId, setActivePresetId] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  // ==================== localStorage 同步 ====================

  const savePresetsToLocalStorage = useCallback((userPresets) => {
    try {
      localStorage.setItem(VIDEO_WALL_PRESETS_KEY, JSON.stringify(userPresets));
    } catch (e) {
      console.error('localStorage presets save failed:', e);
    }
  }, []);

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

  // ==================== 数据库同步 ====================

  const loadUserPresetsFromDatabase = useCallback(async () => {
    try {
      const response = await videoWallConfigApi.getPresets();
      return response.data || [];
    } catch (error) {
      console.debug('Failed to load presets from database:', error);
      return [];
    }
  }, []);

  // ==================== 预设查询 ====================

  /**
   * 获取所有预设（内置预设 + 用户自定义预设）
   */
  const getAllPresets = useCallback(() => {
    return [...BUILT_IN_PRESETS, ...presets];
  }, [presets]);

  // ==================== 预设 CRUD ====================

  /**
   * 创建新预设
   */
  const createPreset = useCallback(async (name, presetConfig) => {
    setIsLoading(true);
    try {
      const effectiveConfig = presetConfig || {};

      const presetData = {
        presetName: name,
        layout: effectiveConfig.layout || PRESET_FALLBACK_CONFIG.layout,
        quality: effectiveConfig.quality || PRESET_FALLBACK_CONFIG.quality,
        bitrate: effectiveConfig.bitrate || PRESET_FALLBACK_CONFIG.bitrate,
        cameraIds: effectiveConfig.cameraIds || [],
        isDefault: false,
      };

      const response = await videoWallConfigApi.createPreset(presetData);
      const newPreset = response.data;

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
  }, [presets, savePresetsToLocalStorage]);

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
      const existingPreset = presets.find(p => p.id === presetId);
      if (!existingPreset) {
        throw new Error('预设不存在或不可删除');
      }

      await videoWallConfigApi.deletePreset(presetId);

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
      const existingPreset = presets.find(p => p.id === presetId);
      if (!existingPreset) {
        throw new Error('预设不存在或不可编辑');
      }

      await videoWallConfigApi.setDefaultPreset(presetId);

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
      const presetIds = newOrder.map(p => p.id || p);

      await videoWallConfigApi.reorderPresets(presetIds);

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

  // ==================== 返回值 ====================

  return {
    // 状态
    presets,
    activePresetId,
    isLoading,
    error,

    // 内部 setter（供组合 Hook 使用）
    setPresets,
    setActivePresetId,
    setError,
    setIsLoading,

    // 内置预设
    builtInPresets: BUILT_IN_PRESETS,

    // 预设查询
    getAllPresets,

    // 预设 CRUD
    createPreset,
    updatePreset,
    deletePreset,
    setAsDefaultPreset,
    reorderPresets,

    // 预设辅助函数
    isBuiltInPreset,
    canEditPreset,
    canDeletePreset,

    // localStorage 同步
    savePresetsToLocalStorage,
    loadPresetsFromLocalStorage,

    // 数据库同步
    loadUserPresetsFromDatabase,

    // 常量
    PRESET_FALLBACK_CONFIG,
  };
};

export default useVideoWallPresets;
export { useVideoWallPresets };
