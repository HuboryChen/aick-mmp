import axiosInstance from './axios';
import axios from './axios';

// 用户认证相关API
export const authApi = {
  login: (credentials) => axios.post('/auth/login', credentials),
  logout: () => axios.post('/auth/logout'),
  getCurrentUser: () => axios.get('/auth/me'),
  refreshToken: () => axios.post('/auth/refresh'),
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
};

// 边缘节点管理相关API
export const edgeNodeApi = {
  getEdgeNodes: (params) => axios.get('/edge-nodes', { params }),
  getEdgeNode: (id) => axios.get(`/edge-nodes/${id}`),
  createEdgeNode: (node) => axios.post('/edge-nodes', node),
  updateEdgeNode: (id, node) => axios.put(`/edge-nodes/${id}`, node),
  deleteEdgeNode: (id) => axios.delete(`/edge-nodes/${id}`),
  updateStatus: (id, status) => axios.patch(`/edge-nodes/${id}/status`, { status }),
  register: (nodeInfo) => axios.post('/registry/edge-nodes', nodeInfo),
  heartbeat: (nodeId, metrics) => axios.post(`/registry/edge-nodes/${nodeId}/heartbeat`, metrics),
};

// 流媒体管理相关API
export const streamingApi = {
  startStream: (cameraId) => axios.post(`/streaming/${cameraId}/start`),
  stopStream: (cameraId) => axios.post(`/streaming/${cameraId}/stop`),
  getStreamUrl: (cameraId) => axios.get(`/streaming/${cameraId}/url`),
  getActiveStreams: () => axios.get('/streaming/active'),
};

// 录像管理相关API
export const recordingApi = {
  searchRecordings: (params) => axios.get('/recordings/search', { params }),
  getRecording: (id) => axios.get(`/recordings/${id}`),
  deleteRecording: (id) => axios.delete(`/recordings/${id}`),
  getRecordingUrl: (id) => axios.get(`/recordings/${id}/url`),
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

// 地区管理相关API
export const regionApi = {
  getAllRegions: () => axios.get('/regions/list'),
  getRegion: (id) => axios.get(`/regions/${id}`),
  createRegion: (region) => axios.post('/regions', region),
  updateRegion: (id, region) => axios.put(`/regions/${id}`, region),
  deleteRegion: (id) => axios.delete(`/regions/${id}`),
  getChildRegions: (parentId) => axios.get(`/regions/children/${parentId}`),
};

export default axiosInstance;