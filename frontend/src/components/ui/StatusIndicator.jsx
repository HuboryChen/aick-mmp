import React from 'react';

/**
 * StatusIndicator - 状态指示器（带脉冲动画）
 * @prop {'online'|'offline'|'warning'} status - 状态类型
 * @prop {boolean} showPulse - 是否显示脉冲动画（默认 true）
 * @prop {number} size - 尺寸（默认 8px）
 */
const StatusIndicator = ({ status = 'offline', showPulse = true, size = 8 }) => {
  const statusColors = {
    online: '#00ff88',
    offline: '#ff4757',
    warning: '#fbbf24',
  };

  const color = statusColors[status] || statusColors.offline;

  return (
    <span
      className="inline-block rounded-full"
      style={{
        width: size,
        height: size,
        backgroundColor: color,
        boxShadow: showPulse
          ? `0 0 ${size / 1.3}px ${color}, 0 0 ${size}px ${color}`
          : `0 0 ${size / 2}px ${color}`,
        animation: showPulse && status === 'online' ? 'pulse-glow 2s ease-in-out infinite' : 'none',
      }}
    />
  );
};

export default StatusIndicator;
