/**
 * 通用工具函数
 */

/**
 * 生成唯一ID
 * @returns {string}
 */
export const generateId = () => {
  return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
};

/**
 * 深拷贝对象
 * @param {Object} obj - 对象
 * @returns {Object}
 */
export const deepClone = (obj) => {
  if (obj === null || typeof obj !== 'object') return obj;
  if (obj instanceof Date) return new Date(obj.getTime());
  if (obj instanceof Array) return obj.map(item => deepClone(item));
  if (obj instanceof Object) {
    const clonedObj = {};
    for (const key in obj) {
      if (obj.hasOwnProperty(key)) {
        clonedObj[key] = deepClone(obj[key]);
      }
    }
    return clonedObj;
  }
};

/**
 * 防抖函数
 * @param {Function} func - 要防抖的函数
 * @param {number} delay - 延迟时间（毫秒）
 * @returns {Function}
 */
export const debounce = (func, delay = 300) => {
  let timer = null;
  return function(...args) {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => {
      func.apply(this, args);
    }, delay);
  };
};

/**
 * 节流函数
 * @param {Function} func - 要节流的函数
 * @param {number} delay - 延迟时间（毫秒）
 * @returns {Function}
 */
export const throttle = (func, delay = 300) => {
  let timer = null;
  return function(...args) {
    if (!timer) {
      timer = setTimeout(() => {
        func.apply(this, args);
        timer = null;
      }, delay);
    }
  };
};

/**
 * 数组去重
 * @param {Array} arr - 数组
 * @param {string} key - 对象数组的key
 * @returns {Array}
 */
export const uniqueArray = (arr, key) => {
  if (!Array.isArray(arr)) return [];
  
  if (key) {
    const seen = new Set();
    return arr.filter(item => {
      const k = item[key];
      if (seen.has(k)) {
        return false;
      }
      seen.add(k);
      return true;
    });
  } else {
    return [...new Set(arr)];
  }
};

/**
 * 数组分组
 * @param {Array} arr - 数组
 * @param {string} key - 分组依据的key
 * @returns {Object}
 */
export const groupBy = (arr, key) => {
  if (!Array.isArray(arr)) return {};
  
  return arr.reduce((result, item) => {
    const groupKey = item[key];
    if (!result[groupKey]) {
      result[groupKey] = [];
    }
    result[groupKey].push(item);
    return result;
  }, {});
};

/**
 * 数组排序
 * @param {Array} arr - 数组
 * @param {string} key - 排序依据的key
 * @param {string} order - 排序方式：'asc'或'desc'
 * @returns {Array}
 */
export const sortBy = (arr, key, order = 'asc') => {
  if (!Array.isArray(arr)) return [];
  
  return [...arr].sort((a, b) => {
    const valueA = a[key];
    const valueB = b[key];
    
    if (valueA === valueB) return 0;
    
    if (order === 'asc') {
      return valueA > valueB ? 1 : -1;
    } else {
      return valueA < valueB ? 1 : -1;
    }
  });
};

/**
 * 对象数组查找
 * @param {Array} arr - 数组
 * @param {string} key - 查找的key
 * @param {*} value - 查找的值
 * @returns {Object|undefined}
 */
export const findBy = (arr, key, value) => {
  if (!Array.isArray(arr)) return undefined;
  return arr.find(item => item[key] === value);
};

/**
 * 对象数组过滤
 * @param {Array} arr - 数组
 * @param {Function} predicate - 过滤条件函数
 * @returns {Array}
 */
export const filterBy = (arr, predicate) => {
  if (!Array.isArray(arr)) return [];
  return arr.filter(predicate);
};

/**
 * 对象数组映射
 * @param {Array} arr - 数组
 * @param {string} key - 要提取的key
 * @returns {Array}
 */
export const pluck = (arr, key) => {
  if (!Array.isArray(arr)) return [];
  return arr.map(item => item[key]);
};

/**
 * 对象数组分页
 * @param {Array} arr - 数组
 * @param {number} page - 页码（从1开始）
 * @param {number} pageSize - 每页数量
 * @returns {Object}
 */
export const paginate = (arr, page = 1, pageSize = 10) => {
  if (!Array.isArray(arr)) {
    return {
      data: [],
      total: 0,
      page: 1,
      pageSize,
      totalPages: 0
    };
  }
  
  const total = arr.length;
  const totalPages = Math.ceil(total / pageSize);
  const startIndex = (page - 1) * pageSize;
  const endIndex = Math.min(startIndex + pageSize, total);
  const data = arr.slice(startIndex, endIndex);
  
  return {
    data,
    total,
    page,
    pageSize,
    totalPages
  };
};

/**
 * 格式化数字（千分位分隔）
 * @param {number} num - 数字
 * @returns {string}
 */
export const formatNumber = (num) => {
  if (num === null || num === undefined || num === '') return '0';
  return Number(num).toLocaleString();
};

/**
 * 格式化百分比
 * @param {number} num - 数字
 * @param {number} decimals - 小数位数
 * @returns {string}
 */
export const formatPercent = (num, decimals = 2) => {
  if (num === null || num === undefined || isNaN(num)) return '0%';
  return `${(Number(num) * 100).toFixed(decimals)}%`;
};

/**
 * 睡眠函数
 * @param {number} ms - 延迟时间（毫秒）
 * @returns {Promise}
 */
export const sleep = (ms) => {
  return new Promise(resolve => setTimeout(resolve, ms));
};

/**
 * 重试函数
 * @param {Function} fn - 要重试的函数
 * @param {number} maxRetries - 最大重试次数
 * @param {number} delay - 重试延迟时间（毫秒）
 * @returns {Promise}
 */
export const retry = async (fn, maxRetries = 3, delay = 1000) => {
  let lastError;
  
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error;
      if (i < maxRetries - 1) {
        await sleep(delay);
      }
    }
  }
  
  throw lastError;
};

/**
 * 获取URL参数
 * @param {string} name - 参数名
 * @param {string} url - URL地址，默认当前页面URL
 * @returns {string|null}
 */
export const getUrlParam = (name, url = window.location.href) => {
  const name = name.replace(/[\[\]]/g, '\\$&');
  const regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)');
  const results = regex.exec(url);
  
  if (!results) return null;
  if (!results[2]) return '';
  return decodeURIComponent(results[2].replace(/\+/g, ' '));
};

/**
 * 设置URL参数
 * @param {string} name - 参数名
 * @param {string} value - 参数值
 * @param {string} url - URL地址，默认当前页面URL
 * @returns {string}
 */
export const setUrlParam = (name, value, url = window.location.href) => {
  const hashIndex = url.indexOf('#');
  let baseUrl = url;
  let hash = '';
  
  if (hashIndex !== -1) {
    baseUrl = url.substring(0, hashIndex);
    hash = url.substring(hashIndex);
  }
  
  const [pathname, search] = baseUrl.split('?');
  const params = new URLSearchParams(search);
  
  if (value === null || value === '') {
    params.delete(name);
  } else {
    params.set(name, value);
  }
  
  const newSearch = params.toString();
  return `${pathname}${newSearch ? '?' : ''}${newSearch}${hash}`;
};

/**
 * 复制到剪贴板
 * @param {string} text - 要复制的文本
 * @returns {Promise<boolean>}
 */
export const copyToClipboard = async (text) => {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
      return true;
    } else {
      // Fallback for older browsers
      const textArea = document.createElement('textarea');
      textArea.value = text;
      textArea.style.position = 'fixed';
      textArea.style.left = '-999999px';
      document.body.appendChild(textArea);
      textArea.select();
      try {
        document.execCommand('copy');
        textArea.remove();
        return true;
      } catch (err) {
        textArea.remove();
        return false;
      }
    }
  } catch (error) {
    console.error('复制失败:', error);
    return false;
  }
};

/**
 * 生成随机颜色
 * @param {string} prefix - 颜色前缀，默认'#'
 * @returns {string}
 */
export const randomColor = (prefix = '#') => {
  const randomColor = Math.floor(Math.random() * 16777215).toString(16);
  return `${prefix}${randomColor.padStart(6, '0')}`;
};

/**
 * 检查是否移动设备
 * @returns {boolean}
 */
export const isMobile = () => {
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
};

/**
 * 检查是否微信浏览器
 * @returns {boolean}
 */
export const isWeChat = () => {
  return /MicroMessenger/i.test(navigator.userAgent);
};

/**
 * 获取设备信息
 * @returns {Object}
 */
export const getDeviceInfo = () => {
  const ua = navigator.userAgent;
  
  return {
    userAgent: ua,
    isMobile: isMobile(),
    isWeChat: isWeChat(),
    isIOS: /iPad|iPhone|iPod/.test(ua),
    isAndroid: /Android/.test(ua),
    isDesktop: !isMobile(),
    browser: getBrowserName(ua),
    os: getOSName(ua)
  };
};

/**
 * 获取浏览器名称
 * @param {string} ua - userAgent
 * @returns {string}
 */
const getBrowserName = (ua) => {
  if (ua.includes('Chrome')) return 'Chrome';
  if (ua.includes('Firefox')) return 'Firefox';
  if (ua.includes('Safari')) return 'Safari';
  if (ua.includes('Edge')) return 'Edge';
  if (ua.includes('Opera')) return 'Opera';
  return 'Unknown';
};

/**
 * 获取操作系统名称
 * @param {string} ua - userAgent
 * @returns {string}
 */
const getOSName = (ua) => {
  if (ua.includes('Windows')) return 'Windows';
  if (ua.includes('Mac OS X')) return 'macOS';
  if (ua.includes('Linux')) return 'Linux';
  if (ua.includes('Android')) return 'Android';
  if (ua.includes('iOS')) return 'iOS';
  return 'Unknown';
};
