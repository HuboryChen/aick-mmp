/**
 * 视频墙配置 API
 * 提供视频墙预设和偏好设置的 API 调用方法
 */

import axios from '../utils/axios';

// 视频墙配置相关 API
export const videoWallConfigApi = {
  // ==================== 用户偏好设置 ====================

  /**
   * 获取当前用户偏好设置
   * @returns {Promise} 包含 VideoWallPreferencesDTO 的响应
   */
  getPreferences: () => axios.get('/api/video-wall/preferences'),

  /**
   * 更新当前用户偏好设置
   * @param {Object} data - 偏好设置数据
   * @returns {Promise} 包含更新后的 VideoWallPreferencesDTO 的响应
   */
  updatePreferences: (data) => axios.put('/api/video-wall/preferences', data),

  // ==================== 预设管理 ====================

  /**
   * 获取当前用户所有预设
   * @returns {Promise} 包含 VideoWallPresetDTO[] 的响应
   */
  getPresets: () => axios.get('/api/video-wall/presets'),

  /**
   * 创建新预设
   * @param {Object} data - 预设数据
   * @returns {Promise} 包含创建的 VideoWallPresetDTO 的响应
   */
  createPreset: (data) => axios.post('/api/video-wall/presets', data),

  /**
   * 更新指定预设
   * @param {number} id - 预设 ID
   * @param {Object} data - 预设数据
   * @returns {Promise} 包含更新后的 VideoWallPresetDTO 的响应
   */
  updatePreset: (id, data) => axios.put(`/api/video-wall/presets/${id}`, data),

  /**
   * 删除指定预设
   * @param {number} id - 预设 ID
   * @returns {Promise} 空响应 (204 No Content)
   */
  deletePreset: (id) => axios.delete(`/api/video-wall/presets/${id}`),

  /**
   * 应用预设
   * 将预设配置应用到用户的偏好设置
   * @param {number} id - 预设 ID
   * @returns {Promise} 包含应用后的 VideoWallPresetDTO 的响应
   */
  applyPreset: (id) => axios.post(`/api/video-wall/presets/${id}/apply`),

  /**
   * 设置默认预设
   * @param {number} id - 预设 ID
   * @returns {Promise} 包含更新后的 VideoWallPresetDTO 的响应
   */
  setDefaultPreset: (id) => axios.post(`/api/video-wall/presets/${id}/set-default`),

  /**
   * 批量更新预设排序
   * @param {number[]} presetIds - 预设 ID 数组 (按新排序顺序)
   * @returns {Promise} 空响应
   */
  reorderPresets: (presetIds) => axios.put('/api/video-wall/presets/reorder', { presetIds }),
};

export default videoWallConfigApi;
