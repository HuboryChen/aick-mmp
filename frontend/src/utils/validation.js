/**
 * 验证工具函数
 */

/**
 * 验证手机号
 * @param {string} phone - 手机号
 * @returns {boolean}
 */
export const validatePhone = (phone) => {
  if (!phone) return false;
  const reg = /^1[3-9]\d{9}$/;
  return reg.test(phone);
};

/**
 * 验证邮箱
 * @param {string} email - 邮箱地址
 * @returns {boolean}
 */
export const validateEmail = (email) => {
  if (!email) return false;
  const reg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return reg.test(email);
};

/**
 * 验证用户名（字母、数字、下划线，4-20位）
 * @param {string} username - 用户名
 * @returns {boolean}
 */
export const validateUsername = (username) => {
  if (!username) return false;
  const reg = /^[a-zA-Z0-9_]{4,20}$/;
  return reg.test(username);
};

/**
 * 验证密码（至少8位，包含字母和数字）
 * @param {string} password - 密码
 * @returns {boolean}
 */
export const validatePassword = (password) => {
  if (!password) return false;
  if (password.length < 8 || password.length > 50) return false;
  const hasLetter = /[a-zA-Z]/.test(password);
  const hasNumber = /[0-9]/.test(password);
  return hasLetter && hasNumber;
};

/**
 * 验证URL
 * @param {string} url - URL地址
 * @returns {boolean}
 */
export const validateUrl = (url) => {
  if (!url) return false;
  try {
    new URL(url);
    return true;
  } catch {
    return false;
  }
};

/**
 * 验证IP地址
 * @param {string} ip - IP地址
 * @returns {boolean}
 */
export const validateIp = (ip) => {
  if (!ip) return false;
  const reg = /^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))$/;
  return reg.test(ip);
};

/**
 * 验证端口号
 * @param {number|string} port - 端口号
 * @returns {boolean}
 */
export const validatePort = (port) => {
  if (port === null || port === undefined || port === '') return false;
  const portNum = Number(port);
  return !isNaN(portNum) && portNum >= 1 && portNum <= 65535;
};

/**
 * 验证视频流URL
 * @param {string} url - 视频流URL
 * @param {string} protocol - 协议类型
 * @returns {boolean}
 */
export const validateStreamUrl = (url, protocol = 'RTSP') => {
  if (!url) return false;

  switch (protocol) {
    case 'RTSP':
      return url.startsWith('rtsp://');
    case 'RTMP':
      return url.startsWith('rtmp://') || url.startsWith('rtmps://');
    case 'HTTP':
      return url.startsWith('http://') || url.startsWith('https://');
    default:
      return true;
  }
};

/**
 * 验证坐标（经度或纬度）
 * @param {number} coord - 坐标值
 * @param {string} type - 类型：'lng'（经度）或'lat'（纬度）
 * @returns {boolean}
 */
export const validateCoordinate = (coord, type = 'lat') => {
  if (coord === null || coord === undefined || coord === '') return false;
  const num = Number(coord);
  
  if (isNaN(num)) return false;

  if (type === 'lng') {
    return num >= -180 && num <= 180;
  } else {
    return num >= -90 && num <= 90;
  }
};

/**
 * 验证数字范围
 * @param {number|string} value - 值
 * @param {number} min - 最小值
 * @param {number} max - 最大值
 * @returns {boolean}
 */
export const validateRange = (value, min, max) => {
  if (value === null || value === undefined || value === '') return false;
  const num = Number(value);
  if (isNaN(num)) return false;
  return num >= min && num <= max;
};

/**
 * 验证JSON字符串
 * @param {string} str - JSON字符串
 * @returns {boolean}
 */
export const validateJson = (str) => {
  if (!str) return false;
  try {
    JSON.parse(str);
    return true;
  } catch {
    return false;
  }
};

/**
 * 去除字符串两端的空格
 * @param {string} str - 字符串
 * @returns {string}
 */
export const trim = (str) => {
  if (!str) return '';
  return str.trim();
};

/**
 * 判断是否为空字符串
 * @param {string} str - 字符串
 * @returns {boolean}
 */
export const isEmpty = (str) => {
  if (str === null || str === undefined) return true;
  if (typeof str === 'string') return str.trim() === '';
  return false;
};

/**
 * 判断是否为空对象或空数组
 * @param {*} obj - 对象或数组
 * @returns {boolean}
 */
export const isObjectEmpty = (obj) => {
  if (obj === null || obj === undefined) return true;
  if (Array.isArray(obj)) return obj.length === 0;
  if (typeof obj === 'object') return Object.keys(obj).length === 0;
  return false;
};
