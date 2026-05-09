/**
 * 视频墙配置 - 前端可调整的参数
 *
 * 后端配置参考: backend/aick-mmp-edge/src/main/resources/application-edge.yml
 */

const videoConfig = {
  // 画质选项定义
  quality: {
    '1080p': { bitrate: 4096, resolution: { width: 1920, height: 1080 } },
    '720p': { bitrate: 2048, resolution: { width: 1280, height: 720 } },
    '480p': { bitrate: 1024, resolution: { width: 854, height: 480 } },
    '360p': { bitrate: 512, resolution: { width: 640, height: 360 } },
    // 兼容旧配置
    'low': { bitrate: 512, resolution: { width: 640, height: 480 } },
    'medium': { bitrate: 2048, resolution: { width: 1280, height: 720 } },
    'high': { bitrate: 4096, resolution: { width: 1920, height: 1080 } },
    'auto': { bitrate: 'auto', resolution: 'auto' },
  },

  // 自动重连配置
  stream: {
    reconnect: {
      enabled: true,
      maxRetries: 5,
      initialDelayMs: 1000,
      maxDelayMs: 30000,
      jitterFactor: 0.3,
    },

    // WebRTC 配置
    webrtc: {
      iceServers: [
        { urls: 'stun:stun.l.google.com:19302' },
        { urls: 'stun:stun1.l.google.com:19302' },
      ],
    },

    // 网络质量阈值（用于自适应画质）
    networkThresholds: {
      excellent: { rtt: 100, packetLoss: 1 },    // 恢复原画质
      moderate: { rtt: 300, packetLoss: 3 },      // 降一级
      poor: { rtt: Infinity, packetLoss: Infinity }, // 降两级
    },
  },
};

export default videoConfig;
