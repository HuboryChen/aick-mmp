/**
 * 文件操作工具函数
 */

/**
 * 下载文件
 * @param {string} url - 文件URL
 * @param {string} filename - 下载的文件名
 */
export const downloadFile = async (url, filename) => {
  try {
    const response = await fetch(url);
    const blob = await response.blob();
    const downloadUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = filename || `download_${Date.now()}`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(downloadUrl);
  } catch (error) {
    console.error('下载文件失败:', error);
    throw error;
  }
};

/**
 * 下载文件（使用Blob）
 * @param {Blob} blob - Blob对象
 * @param {string} filename - 下载的文件名
 */
export const downloadBlob = (blob, filename) => {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename || `download_${Date.now()}`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
};

/**
 * 格式化文件大小
 * @param {number} bytes - 文件大小（字节）
 * @returns {string} 格式化后的文件大小
 */
export const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B';

  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
};

/**
 * 获取文件扩展名
 * @param {string} filename - 文件名
 * @returns {string} 文件扩展名
 */
export const getFileExtension = (filename) => {
  if (!filename) return '';
  const lastDotIndex = filename.lastIndexOf('.');
  if (lastDotIndex === -1) return '';
  return filename.substring(lastDotIndex + 1).toLowerCase();
};

/**
 * 判断是否为图片文件
 * @param {string} filename - 文件名
 * @returns {boolean}
 */
export const isImageFile = (filename) => {
  const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'];
  const ext = getFileExtension(filename);
  return imageExtensions.includes(ext);
};

/**
 * 判断是否为视频文件
 * @param {string} filename - 文件名
 * @returns {boolean}
 */
export const isVideoFile = (filename) => {
  const videoExtensions = ['mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv'];
  const ext = getFileExtension(filename);
  return videoExtensions.includes(ext);
};

/**
 * 读取文件为DataURL
 * @param {File} file - 文件对象
 * @returns {Promise<string>}
 */
export const readFileAsDataURL = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
};

/**
 * 读取文件为文本
 * @param {File} file - 文件对象
 * @returns {Promise<string>}
 */
export const readFileAsText = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = reject;
    reader.readAsText(file);
  });
};

/**
 * 验证文件类型
 * @param {File} file - 文件对象
 * @param {Array<string>} allowedTypes - 允许的文件类型
 * @returns {boolean}
 */
export const validateFileType = (file, allowedTypes) => {
  if (!file) return false;
  const ext = getFileExtension(file.name);
  return allowedTypes.includes(ext.toLowerCase());
};

/**
 * 验证文件大小
 * @param {File} file - 文件对象
 * @param {number} maxSize - 最大文件大小（字节）
 * @returns {boolean}
 */
export const validateFileSize = (file, maxSize) => {
  if (!file) return false;
  return file.size <= maxSize;
};

/**
 * 格式化时长（秒转换为HH:mm:ss）
 * @param {number} seconds - 秒数
 * @returns {string}
 */
export const formatDuration = (seconds) => {
  if (!seconds) return '00:00:00';

  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const secs = Math.floor(seconds % 60);

  return [
    String(hours).padStart(2, '0'),
    String(minutes).padStart(2, '0'),
    String(secs).padStart(2, '0')
  ].join(':');
};
