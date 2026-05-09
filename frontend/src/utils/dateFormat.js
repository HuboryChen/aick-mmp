/**
 * 日期格式化工具函数
 */

/**
 * 格式化日期
 * @param {Date|string|number} date - 日期对象、时间戳或日期字符串
 * @param {string} format - 格式化字符串，默认 'YYYY-MM-DD HH:mm:ss'
 * @returns {string} 格式化后的日期字符串
 */
export const formatDate = (date, format = 'YYYY-MM-DD HH:mm:ss') => {
  if (!date) return '-';

  const d = new Date(date);
  if (isNaN(d.getTime())) return '-';

  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  const seconds = String(d.getSeconds()).padStart(2, '0');

  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds);
};

/**
 * 格式化日期（仅日期部分）
 */
export const formatOnlyDate = (date) => {
  return formatDate(date, 'YYYY-MM-DD');
};

/**
 * 格式化时间（仅时间部分）
 */
export const formatOnlyTime = (date) => {
  return formatDate(date, 'HH:mm:ss');
};

/**
 * 获取相对时间（如：5分钟前、1小时前）
 */
export const getRelativeTime = (date) => {
  if (!date) return '-';

  const now = new Date();
  const d = new Date(date);
  const diff = now - d;

  if (diff < 60000) { // 小于1分钟
    return '刚刚';
  } else if (diff < 3600000) { // 小于1小时
    const minutes = Math.floor(diff / 60000);
    return `${minutes}分钟前`;
  } else if (diff < 86400000) { // 小于1天
    const hours = Math.floor(diff / 3600000);
    return `${hours}小时前`;
  } else if (diff < 604800000) { // 小于7天
    const days = Math.floor(diff / 86400000);
    return `${days}天前`;
  } else {
    return formatDate(date, 'YYYY-MM-DD');
  }
};

/**
 * 判断是否为今天
 */
export const isToday = (date) => {
  if (!date) return false;
  const d = new Date(date);
  const today = new Date();
  return d.getDate() === today.getDate() &&
         d.getMonth() === today.getMonth() &&
         d.getFullYear() === today.getFullYear();
};

/**
 * 判断是否为本周
 */
export const isThisWeek = (date) => {
  if (!date) return false;
  const d = new Date(date);
  const today = new Date();
  const firstDayOfWeek = new Date(today);
  firstDayOfWeek.setDate(today.getDate() - today.getDay());
  const lastDayOfWeek = new Date(firstDayOfWeek);
  lastDayOfWeek.setDate(firstDayOfWeek.getDate() + 6);

  return d >= firstDayOfWeek && d <= lastDayOfWeek;
};

/**
 * 计算两个日期之间的天数差
 */
export const getDaysDiff = (date1, date2) => {
  if (!date1 || !date2) return 0;
  const d1 = new Date(date1);
  const d2 = new Date(date2);
  const diffTime = Math.abs(d2 - d1);
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
};
