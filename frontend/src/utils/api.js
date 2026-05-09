import axiosInstance from './axios';
import axios from './axios';

// 用户认证相关API
export const authApi = {
  login: (credentials) => axios.post('/auth/login', credentials),
  logout: () => axios.post('/auth/logout'),
  getCurrentUser: () => axios.get('/auth/me'),
  refreshToken: (refreshToken) => axios.post('/auth/refresh', { refreshToken }),
};

// 用户管理相关API
export const userApi = {
  getUsers: (params) => axios.get('/users', { params }),
  searchUsers: (params) => axios.get('/users/search', { params }),
  getUser: (id) => axios.get(`/users/${id}`),
  createUser: (user) => axios.post('/users', user),
  updateUser: (id, user) => axios.put(`/users/${id}`, user),
  deleteUser: (id) => axios.delete(`/users/${id}`),
  getRoles: () => axios.get('/users/roles'),
  changePassword: (id, passwordData) => axios.post(`/users/${id}/change-password`, passwordData),
  resetPassword: (id, newPassword) => axios.post(`/users/${id}/reset-password`, null, { params: { newPassword } }),
  batchDeleteUsers: (batchData) => axios.post('/users/batch-delete', batchData),
  batchEnableUsers: (batchData, enabled) => axios.post('/users/batch-enable', batchData, { params: { enabled } }),
  batchUpdateUserRole: (batchData, role) => axios.post('/users/batch-update-role', batchData, { params: { role } }),
};

// 摄像头管理相关API
export const cameraApi = {
  getCameras: (params) => axios.get('/cameras', { params }),
  getCamera: (id) => axios.get(`/cameras/${id}`),
  createCamera: (camera) => axios.post('/cameras', camera),
  updateCamera: (id, camera) => axios.put(`/cameras/${id}`, camera),
  deleteCamera: (id) => axios.delete(`/cameras/${id}`),
  startStream: (id) => axios.post(`/cameras/${id}/start`),
  stopStream: (id) => axios.post(`/cameras/${id}/stop`),
  batchDeleteCameras: (cameraIds) => axios.post('/cameras/batch-operation', {
    operation: 'DELETE',
    cameraIds: Array.isArray(cameraIds) ? cameraIds : cameraIds.cameraIds
  }),
  batchUpdateEdgeNode: (batchData, edgeNodeId) => axios.post('/cameras/batch-update-edge-node', { ...batchData, edgeNodeId }),
  autoAssignCameras: () => axios.post('/cameras/auto-assign'),
  getOptimalEdgeNode: (cameraName) => axios.get('/cameras/optimal-edge-node', { params: { cameraName } }),
};

// 边缘节点管理相关API
export const edgeNodeApi = {
  getEdgeNodes: (params) => axios.get('/edge-nodes', { params }),
  searchEdgeNodes: (params) => axios.get('/edge-nodes/search', { params }),
  getEdgeNode: (id) => axios.get(`/edge-nodes/${id}`),
  createEdgeNode: (node) => axios.post('/edge-nodes', node),
  updateEdgeNode: (id, node) => axios.put(`/edge-nodes/${id}`, node),
  deleteEdgeNode: (id) => axios.delete(`/edge-nodes/${id}`),
  updateStatus: (id, status) => axios.patch(`/edge-nodes/${id}/status`, { status }),
  register: (nodeInfo) => axios.post('/edge-nodes/register', nodeInfo),
  heartbeat: (nodeId, metrics) => axios.post(`/edge-nodes/${nodeId}/heartbeat`, metrics),
  testConnection: (id) => axios.post(`/edge-nodes/${id}/test-connection`),
  restartNode: (id) => axios.post(`/edge-nodes/${id}/restart`),

  // 单个节点启用/禁用
  enableEdgeNode: (id, enabled) => axios.post('/edge-nodes/batch-enable', [id], { params: { enabled } }),

  // 批量操作
  batchDeleteEdgeNodes: (nodeIds) => axios.post('/edge-nodes/batch-delete', nodeIds),
  batchEnableEdgeNodes: (nodeIds, enabled) => axios.post('/edge-nodes/batch-enable', nodeIds, { params: { enabled } }),
  batchUpdateEdgeNodeStatus: (nodeIds, status) => axios.post('/edge-nodes/batch-update-status', nodeIds, { params: { status } }),

  // 健康检查
  getEdgeNodeHealthStatus: (id) => axios.get(`/edge-nodes/${id}/health-status`),
  getEdgeNodeHealthDetails: (id) => axios.get(`/edge-nodes/${id}/health-details`),

  // 统计和详情
  getEdgeNodeDetails: (id) => axios.get(`/edge-nodes/${id}/details`),
  updateEdgeNodeCredentials: (id, credentials) => axios.put(`/edge-nodes/${id}/credentials`, credentials),
};

// 流媒体管理相关API
export const streamingApi = {
  // 基础流管理
  startStream: (cameraId) => axios.post(`/streaming/${cameraId}/start`),
  stopStream: (cameraId) => axios.post(`/streaming/${cameraId}/stop`),
  getStreamUrl: (cameraId) => axios.get(`/streaming/${cameraId}/url`),
  getActiveStreams: () => axios.get('/streaming/active'),
  getStreamStatus: (cameraId) => axios.get(`/streaming/${cameraId}/status`),
  
  // WebRTC支持
  generateWebRtcOffer: (cameraId) => axios.post(`/streaming/${cameraId}/webrtc/offer`),
  processWebRtcAnswer: (cameraId, answer) => axios.post(`/streaming/${cameraId}/webrtc/answer`, answer),
  
  // 流控制
  pauseStream: (cameraId) => axios.post(`/streaming/${cameraId}/pause`),
  resumeStream: (cameraId) => axios.post(`/streaming/${cameraId}/resume`),
  updateQuality: (cameraId, qualitySettings) => axios.put(`/streaming/${cameraId}/quality`, qualitySettings),
  
  // 录像
  getRecording: (cameraId, startTime, endTime) => 
    axios.get(`/streaming/${cameraId}/recording`, { params: { startTime, endTime } }),
  startRecording: (cameraId) => axios.post(`/streaming/${cameraId}/recording/start`),
  stopRecording: (cameraId) => axios.post(`/streaming/${cameraId}/recording/stop`),
};

// 录像管理相关API
export const recordingApi = {
  searchRecordings: (params) => axios.get('/recordings/search', { params }),
  getRecording: (id) => axios.get(`/recordings/${id}`),
  deleteRecording: (id) => axios.delete(`/recordings/${id}`),
  getRecordingUrl: (id) => axios.get(`/recordings/${id}/url`),
  
  // 获取录像列表（支持增强查询参数）
  getRecordings: (params) => axios.get('/recordings', { params }),
  
  // 查询参数增强
  getRecordingsWithFilters: (params) => {
    const {
      page = 0,
      size = 20,
      cameraId,
      location,
      startTime,
      endTime,
      status,
      integrityStatus,
      recordingType,
      minFileSize,
      maxFileSize,
      sort = 'startTime,desc',
      ...rest
    } = params;
    
    return axios.get('/recordings', {
      params: {
        page,
        size,
        cameraId,
        location,
        startTime,
        endTime,
        status,
        integrityStatus,
        recordingType,
        minFileSize,
        maxFileSize,
        sort,
        ...rest,
      },
    });
  },

  // 录像下载相关API
  downloadRecording: (id) => {
    return axios.get(`/recordings/${id}/download`, {
      responseType: 'blob',
    });
  },

  batchDownloadPrepare: (recordingIds) => {
    return axios.post('/recordings/batch-download', { recordingIds });
  },

  getDownloadStatus: () => {
    return axios.get('/recordings/download/status');
  },

  cancelDownload: (id) => {
    return axios.post(`/recordings/${id}/download/cancel`);
  },
};

// 仪表盘相关API
export const dashboardApi = {
  getStats: () => axios.get('/dashboard/stats'),
  getRecentAlerts: () => axios.get('/dashboard/alerts'),
  getSystemHealth: () => axios.get('/dashboard/health'),
};

// 系统设置相关API
export const settingsApi = {
  getSettings: () => axios.get('/settings'),
  updateSettings: (settings) => axios.put('/settings', settings),
};

// 系统配置相关API (新)
export const systemConfigApi = {
  // 列表和查询
  getAllConfigs: () => axios.get('/v1/system-configs'),
  getConfig: (configKey) => axios.get(`/v1/system-configs/${configKey}`),
  getConfigValue: (configKey) => axios.get(`/v1/system-configs/${configKey}/value`),
  getByCategory: (category) => axios.get(`/v1/system-configs/category/${category}`),
  getByGroup: (group) => axios.get(`/v1/system-configs/group/${group}`),
  getAllGroups: () => axios.get('/v1/system-configs/groups'),
  
  // CRUD
  createConfig: (data) => axios.post('/v1/system-configs', data),
  updateConfig: (id, data) => axios.put(`/v1/system-configs/${id}`, data),
  deleteConfig: (id) => axios.delete(`/v1/system-configs/${id}`),
  
  // 批量操作
  batchUpdate: (data) => axios.put('/v1/system-configs/batch', data),
  
  // 重置
  resetConfig: (configKey) => axios.post(`/v1/system-configs/${configKey}/reset`),
  resetAllConfigs: () => axios.post('/v1/system-configs/reset-all'),
  
  // 历史
  getConfigHistory: (configKey) => axios.get(`/v1/system-configs/${configKey}/history`),
  getConfigHistoryPage: (configKey, params) => axios.get(`/v1/system-configs/${configKey}/history/page`, { params }),
  
  // 回滚
  rollbackConfig: (configKey) => axios.post(`/v1/system-configs/${configKey}/rollback`),
  
  // 邮件测试
  testEmail: (data) => axios.post('/v1/system-configs/email/test', data),
  
  // 初始化
  initialize: () => axios.post('/v1/system-configs/initialize'),
};

// 地区管理相关API
export const regionApi = {
  // 列表和查询
  getAllRegions: () => axios.get('/v1/regions/list'),
  getRegionsFlat: () => axios.get('/v1/regions/flat'),
  getRegionTree: () => axios.get('/v1/regions/tree'),
  searchRegions: (keyword) => axios.get('/v1/regions/search', { params: { keyword } }),
  getRegionsByLevel: (level) => axios.get(`/v1/regions/level/${level}`),
  getChildRegions: (parentId) => axios.get(`/v1/regions/children/${parentId}`),
  
  // CRUD
  getRegion: (id) => axios.get(`/v1/regions/${id}`),
  createRegion: (region) => axios.post('/v1/regions', region),
  updateRegion: (id, region) => axios.put(`/v1/regions/${id}`, region),
  deleteRegion: (id, force = false) => axios.delete(`/v1/regions/${id}`, { params: { force } }),
  
  // 区域操作
  moveRegion: (id, newParentId) => axios.patch(`/v1/regions/${id}/move`, { newParentId }),
  
  // 区域统计和摄像头
  getRegionStats: (id) => axios.get(`/v1/regions/${id}/stats`),
  getRegionCameras: (id, recursive = false) => axios.get(`/v1/regions/${id}/cameras`, { params: { recursive } }),
  assignCameraToRegion: (cameraId, regionId) => axios.post(`/v1/regions/cameras/${cameraId}/assign`, null, { params: { regionId } }),
  removeCameraFromRegion: (cameraId) => axios.delete(`/v1/regions/cameras/${cameraId}/remove`),
};

// 系统应用管理相关API
export const systemAppApi = {
  list: (params) => axios.get('/system-apps', { params }),
  get: (id) => axios.get(`/system-apps/${id}`),
  create: (data) => axios.post('/system-apps', data),
  update: (id, data) => axios.put(`/system-apps/${id}`, data),
  delete: (id) => axios.delete(`/system-apps/${id}`),
  updateStatus: (id, status) => axios.put(`/system-apps/${id}/status`, { status }),
  batchDelete: (ids) => axios.post('/system-apps/batch-delete', ids),
  batchUpdateStatus: (ids, status) => axios.post('/system-apps/batch-update-status', ids, { params: { status } }),
  
  // 凭证管理
  getCredentials: (id) => axios.get(`/system-apps/${id}/credentials`),
  regenerateCredentials: (id) => axios.post(`/system-apps/${id}/credentials/regenerate`),
};

// 告警规则管理相关API
export const alertRuleApi = {
  list: (params) => axios.get('/v1/alert-rules', { params }),
  get: (id) => axios.get(`/v1/alert-rules/${id}`),
  create: (data) => axios.post('/v1/alert-rules', data),
  update: (id, data) => axios.put(`/v1/alert-rules/${id}`, data),
  delete: (id) => axios.delete(`/v1/alert-rules/${id}`),
  enable: (id) => axios.post(`/v1/alert-rules/${id}/enable`),
  disable: (id) => axios.post(`/v1/alert-rules/${id}/disable`),
  test: (id) => axios.post(`/v1/alert-rules/${id}/test`),
  getByType: (alertType) => axios.get(`/v1/alert-rules/by-type/${alertType}`),
  getHistory: (id, params) => axios.get(`/v1/alert-rules/${id}/history`, { params }),
  getStats: (id, params) => axios.get(`/v1/alert-rules/${id}/stats`, { params }),
  // 模板相关
  getTemplates: () => axios.get('/v1/alert-rules/templates'),
  getTemplate: (id) => axios.get(`/v1/alert-rules/templates/${id}`),
  getTemplatesByType: (type) => axios.get(`/v1/alert-rules/templates/by-type/${type}`),
  getRecommendedTemplates: () => axios.get('/v1/alert-rules/templates/recommended'),
  searchTemplates: (keyword) => axios.get('/v1/alert-rules/templates/search', { params: { keyword } }),
  // 升级相关
  getAlertEscalations: (alertId) => axios.get(`/v1/alert-rules/alerts/${alertId}/escalations`),
  triggerEscalation: (alertId) => axios.post(`/v1/alert-rules/alerts/${alertId}/escalate`),
};

// 告警记录管理相关API
export const alertRecordApi = {
  list: (params) => axios.get('/alerts/records', { params }),
  get: (id) => axios.get(`/alerts/records/${id}`),
  getByRule: (ruleId, params) => axios.get(`/alerts/records/by-rule/${ruleId}`, { params }),
  getByLevel: (level, params) => axios.get(`/alerts/records/by-level/${level}`, { params }),
  getByCamera: (cameraId, params) => axios.get(`/alerts/records/by-camera/${cameraId}`, { params }),
  getByTimeRange: (startTime, endTime, params) => 
    axios.get('/alerts/records/by-time-range', { params: { startTime, endTime, ...params } }),
  getUnresolved: () => axios.get('/alerts/records/unresolved'),
  getRecent: (limit = 10) => axios.get('/alerts/records/recent', { params: { limit } }),
  getToday: () => axios.get('/alerts/records/today'),
  getStatistics: (params) => axios.get('/alerts/records/statistics', { params }),
  acknowledge: (id) => axios.post(`/alerts/records/${id}/acknowledge`),
  resolve: (id, data) => axios.post(`/alerts/records/${id}/resolve`, data),
  batchResolve: (ids, data) => axios.post('/alerts/records/batch-resolve', { ids, ...data }),
};

// 用户配置相关API
export const userConfigApi = {
  // 保存单个配置
  saveConfig: (configKey, configValue) => axios.post('/user-configs', { configKey, configValue }),
  // 批量保存配置
  saveConfigs: (configs) => axios.post('/user-configs/batch', configs),
  // 获取指定配置
  getConfig: (configKey) => axios.get(`/user-configs/${configKey}`),
  // 获取用户所有配置
  getUserConfigs: () => axios.get('/user-configs'),
  // 删除指定配置
  deleteConfig: (configKey) => axios.delete(`/user-configs/${configKey}`),
  // 删除所有配置
  deleteAllConfigs: () => axios.delete('/user-configs'),
};

// API Key管理相关API (仅用户级)
export const apiKeyApi = {
  // 用户级API Key
  createForUser: (data) => axios.post('/api-keys/me', data),
  listForUser: (params) => axios.get('/api-keys/me', { params }),
  getUserKey: (id) => axios.get(`/api-keys/me/${id}`),
  updateUserKeyStatus: (id, status) => axios.put(`/api-keys/me/${id}/status`, { status }),
  deleteUserKey: (id) => axios.delete(`/api-keys/me/${id}`),
};

// CDN节点管理相关API
export const cdnNodeApi = {
  // 列表和查询
  getCdnNodes: (params) => axios.get('/v1/cdn-nodes', { params }),
  getAllCdnNodes: () => axios.get('/v1/cdn-nodes/all'),
  getCdnNode: (id) => axios.get(`/v1/cdn-nodes/${id}`),
  searchCdnNodes: (keyword) => axios.get('/v1/cdn-nodes/search', { params: { keyword } }),
  getCdnNodesByRegion: (region) => axios.get('/v1/cdn-nodes/by-region', { params: { region } }),
  getCdnNodesByStatus: (status) => axios.get('/v1/cdn-nodes/by-status', { params: { status } }),
  
  // CRUD
  createCdnNode: (node) => axios.post('/v1/cdn-nodes', node),
  updateCdnNode: (id, node) => axios.put(`/v1/cdn-nodes/${id}`, node),
  deleteCdnNode: (id) => axios.delete(`/v1/cdn-nodes/${id}`),
  
  // 状态操作
  enableCdnNode: (id) => axios.post(`/v1/cdn-nodes/${id}/enable`),
  disableCdnNode: (id) => axios.post(`/v1/cdn-nodes/${id}/disable`),
  updateStatus: (id, status) => axios.patch(`/v1/cdn-nodes/${id}/status`, { status }),
  
  // 负载和连通性
  getCdnNodeLoad: (id, params) => axios.get(`/v1/cdn-nodes/${id}/load`, { params }),
  reportCdnNodeLoad: (id, data) => axios.post(`/v1/cdn-nodes/${id}/report`, data),
  testConnectivity: (id) => axios.post(`/v1/cdn-nodes/${id}/test-connectivity`),
  getCdnNodeStats: (id) => axios.get(`/v1/cdn-nodes/${id}/stats`),
  
  // 健康检查
  getCdnNodeHealthStatus: (id) => axios.get(`/v1/cdn-nodes/${id}/health-status`),
  batchHealthCheck: () => axios.post('/v1/cdn-nodes/batch-health-check'),
};

// 数据分析相关API
export const analyticsApi = {
  // 设备使用统计
  getDeviceUsageStats: (params) => axios.get('/v1/analytics/device-usage', { params }),
  
  // 带宽统计
  getBandwidthStats: (params) => axios.get('/v1/analytics/bandwidth', { params }),
  
  // 存储统计
  getStorageStats: (params) => axios.get('/v1/analytics/storage', { params }),
  
  // 告警统计
  getAlertStats: (params) => axios.get('/v1/analytics/alerts', { params }),
  
  // 通用分析查询
  queryAnalytics: (data) => axios.post('/v1/analytics/query', data),
  
  // 趋势数据
  getTrends: (params) => axios.get('/v1/analytics/trends', { params }),
  
  // 可用维度
  getAvailableDimensions: () => axios.get('/v1/analytics/dimensions'),
  
  // 导出报表
  exportReport: (data) => axios.post('/v1/analytics/reports/export', data, { responseType: 'blob' }),
  
  // 订阅管理
  getSubscriptions: () => axios.get('/v1/analytics/subscriptions'),
  getSubscription: (id) => axios.get(`/v1/analytics/subscriptions/${id}`),
  createSubscription: (data) => axios.post('/v1/analytics/subscriptions', data),
  updateSubscription: (id, data) => axios.put(`/v1/analytics/subscriptions/${id}`, data),
  deleteSubscription: (id) => axios.delete(`/v1/analytics/subscriptions/${id}`),
  toggleSubscription: (id, enabled) => axios.patch(`/v1/analytics/subscriptions/${id}/toggle`, null, { params: { enabled } }),
  triggerReport: (id) => axios.post(`/v1/analytics/subscriptions/${id}/trigger`, null, { responseType: 'blob' }),
  
  // 报表模板
  getReportTemplates: () => axios.get('/v1/analytics/report-templates'),
};

// ========== 录像计划管理相关API ==========
export const recordingScheduleApi = {
  getSchedules: (params) => axios.get('/v1/recording-schedules', { params }),
  getSchedule: (id) => axios.get(`/v1/recording-schedules/${id}`),
  createSchedule: (schedule) => axios.post('/v1/recording-schedules', schedule),
  updateSchedule: (id, schedule) => axios.put(`/v1/recording-schedules/${id}`, schedule),
  deleteSchedule: (id) => axios.delete(`/v1/recording-schedules/${id}`),
  enableSchedule: (id, enabled) => axios.patch(`/v1/recording-schedules/${id}/enabled`, { enabled }),
  getSchedulesByCamera: (cameraId) => axios.get(`/v1/recording-schedules/camera/${cameraId}`),
};

// ========== 移动侦测事件相关API ==========
export const motionEventApi = {
  // 移动事件查询
  getMotionEvents: (params) => axios.get('/v1/motion-events', { params }),
  getMotionEvent: (id) => axios.get(`/v1/motion-events/${id}`),
  getMotionEventsByCamera: (cameraId, params) => axios.get(`/v1/motion-events/camera/${cameraId}`, { params }),
  getMotionEventsByTimeRange: (cameraId, startTime, endTime) => 
    axios.get(`/v1/motion-events/camera/${cameraId}/time-range`, { params: { startTime, endTime } }),
  getTriggeredRecordingEvents: (params) => axios.get('/v1/motion-events/triggered', { params }),
  
  // 统计
  countByCamera: (cameraId) => axios.get(`/v1/motion-events/camera/${cameraId}/count`),
  countByTimeRange: (cameraId, startTime, endTime) => 
    axios.get(`/v1/motion-events/camera/${cameraId}/count/time-range`, { params: { startTime, endTime } }),
  
  // 清理
  cleanupOldEvents: (daysOld = 30) => axios.delete('/v1/motion-events/cleanup', { params: { daysOld } }),
  
  // 关联录像
  linkRecording: (eventId, recordingId) => 
    axios.post(`/v1/motion-events/${eventId}/link-recording`, null, { params: { recordingId } }),
};

// ========== 录像管理增强相关API ==========
export const enhancedRecordingApi = {
  // 录像搜索
  searchRecordings: (params) => axios.get('/recordings/search', { params }),
  getRecordings: (params) => axios.get('/recordings', { params }),
  
  // 已删除录像
  getDeletedRecordings: (params) => axios.get('/cameras/recordings/deleted', { params }),
  
  // 孤立录像
  getOrphanedRecordings: (params) => axios.get('/cameras/recordings/orphaned', { params }),
  getOrphanedRecordingsCount: () => axios.get('/cameras/recordings/orphaned/count'),
  cleanupOrphanedRecordings: (daysOld = 30) => axios.post('/cameras/recordings/cleanup', null, { params: { daysOld } }),
  
  // 录像恢复
  restoreRecording: (id) => axios.post(`/cameras/recordings/${id}/restore`),
  
  // 录像下载
  getRecordingUrl: (id) => axios.get(`/recordings/${id}/url`),
  downloadRecording: (id) => {
    return axios.get(`/recordings/${id}/download`, { responseType: 'blob' });
  },
};

// ========== 摄像头配置模板相关API ==========
export const cameraConfigTemplateApi = {
  getTemplates: (params) => axios.get('/api/camera-config-templates', { params }),
  getTemplateById: (id) => axios.get(`/api/camera-config-templates/${id}`),
  createTemplate: (data) => axios.post('/api/camera-config-templates', data),
  updateTemplate: (id, data) => axios.put(`/api/camera-config-templates/${id}`, data),
  deleteTemplate: (id) => axios.delete(`/api/camera-config-templates/${id}`),
  generateUrl: (id, params) => axios.post(`/api/camera-config-templates/${id}/generate-url`, params),
  matchTemplate: (brand, model) => axios.post('/api/camera-config-templates/match', null, { params: { brand, model } }),
  importTemplates: (data) => axios.post('/api/camera-config-templates/import', data),
  exportTemplates: (ids) => axios.get('/api/camera-config-templates/export', { params: { ids } }),
  getBrands: () => axios.get('/api/camera-config-templates/brands'),
};

// ========== 网络发现相关API ==========
export const cameraDiscoveryApi = {
  startScan: (networkSegment) => axios.post('/api/camera-discovery/scan', { networkSegment }),
  getScanProgress: (taskId) => axios.get(`/api/camera-discovery/scan/${taskId}/progress`),
  cancelScan: (taskId) => axios.delete(`/api/camera-discovery/scan/${taskId}`),
  testConnectivity: (ip, port) => axios.post('/api/camera-discovery/test-connectivity', { ip, port }),
  identifyDevice: (ip, port) => axios.post('/api/camera-discovery/identify', { ip, port }),
  getScanHistory: (params) => axios.get('/api/camera-discovery/history', { params }),
};

// ========== 批量导入相关API ==========
export const cameraBatchImportApi = {
  downloadTemplate: () => axios.get('/api/camera-batch-import/template', { responseType: 'blob' }),
  startImport: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return axios.post('/api/camera-batch-import/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  getImportProgress: (taskId) => axios.get(`/api/camera-batch-import/${taskId}/progress`),
  cancelImport: (taskId) => axios.delete(`/api/camera-batch-import/${taskId}`),
  downloadErrorReport: (taskId) => axios.get(`/api/camera-batch-import/${taskId}/errors`, { responseType: 'blob' }),
  getImportHistory: (params) => axios.get('/api/camera-batch-import/history', { params }),
  validateImportData: (data) => axios.post('/api/camera-batch-import/validate', data),
};

export default axiosInstance;