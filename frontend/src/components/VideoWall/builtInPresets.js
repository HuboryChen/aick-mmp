/**
 * 内置预设模块
 * 
 * 定义系统预置的视频墙预设，这些预设不可被用户删除或编辑。
 * 提供辅助函数用于判断预设属性。
 */

/**
 * 内置预设常量
 * 包含三种常见的视频墙布局预设：
 * - 单屏监控: 1画面，适合重点监控单个摄像头
 * - 四分屏: 2x2画面，适合一般监控场景
 * - 九宫格: 3x3画面，适合概览大量摄像头
 */
export const BUILT_IN_PRESETS = [
  {
    id: 'sys-1',
    name: '单屏监控',
    layout: '1',
    quality: '1080p',
    bitrate: 4096,
    cameraIds: [],
    isDefault: false,
    isBuiltIn: true
  },
  {
    id: 'sys-2',
    name: '四分屏',
    layout: '4',
    quality: '720p',
    bitrate: 2048,
    cameraIds: [],
    isDefault: true,
    isBuiltIn: true
  },
  {
    id: 'sys-3',
    name: '九宫格',
    layout: '9',
    quality: '480p',
    bitrate: 1024,
    cameraIds: [],
    isDefault: false,
    isBuiltIn: true
  }
];

/**
 * 判断是否为内置预设
 * @param {Object} preset - 预设对象
 * @returns {boolean} 是否为内置预设
 */
export const isBuiltInPreset = (preset) => {
  return preset?.isBuiltIn === true;
};

/**
 * 判断预设是否可以编辑
 * 内置预设不可编辑，用户自定义预设可以编辑
 * @param {Object} preset - 预设对象
 * @returns {boolean} 是否可以编辑
 */
export const canEditPreset = (preset) => {
  return !isBuiltInPreset(preset);
};

/**
 * 判断预设是否可以删除
 * 内置预设不可删除，用户自定义预设可以删除
 * @param {Object} preset - 预设对象
 * @returns {boolean} 是否可以删除
 */
export const canDeletePreset = (preset) => {
  return !isBuiltInPreset(preset);
};

export default BUILT_IN_PRESETS;
