import React from 'react';
import { Button } from 'antd';
import { useStreamHealth, ConnectionState } from '../contexts/StreamHealthContext';
import './ConnectionStatusBadge.css';

export function ConnectionStatusBadge({ cameraId, onRetry }) {
  const { connectionState, retryCount, error, nextRetryIn, isReconnecting } =
    useStreamHealth();

  // 只在非正常状态显示
  if (
    connectionState === ConnectionState.CONNECTED ||
    connectionState === ConnectionState.IDLE ||
    connectionState === ConnectionState.CLOSED
  ) {
    return null;
  }

  const getStatusConfig = () => {
    switch (connectionState) {
      case ConnectionState.RECONNECTING:
        return {
          className: 'status-reconnecting',
          dotClass: 'dot-warning',
          text: `第 ${retryCount} 次重连中`,
          subtext:
            nextRetryIn > 0
              ? `${Math.ceil(nextRetryIn / 1000)}s 后重试`
              : '正在重连...',
        };
      case ConnectionState.CONNECTING:
        return {
          className: 'status-connecting',
          dotClass: 'dot-info',
          text: '连接中...',
          subtext: '',
        };
      case ConnectionState.DISCONNECTED:
        return {
          className: 'status-disconnected',
          dotClass: 'dot-warning',
          text: '连接断开',
          subtext: '正在重连...',
        };
      case ConnectionState.FAILED:
        return {
          className: 'status-failed',
          dotClass: 'dot-error',
          text: '连接失败',
          subtext: error || '请检查网络',
        };
      default:
        return null;
    }
  };

  const config = getStatusConfig();
  if (!config) return null;

  return (
    <div className={`connection-status-badge ${config.className}`}>
      <div className="status-content">
        <span className={`status-dot ${config.dotClass}`} />
        <div className="status-text">
          <span className="status-main">{config.text}</span>
          {config.subtext && (
            <span className="status-sub">{config.subtext}</span>
          )}
        </div>
      </div>
      {connectionState === ConnectionState.FAILED && onRetry && (
        <Button
          size="small"
          type="primary"
          className="retry-button"
          onClick={onRetry}
        >
          重试
        </Button>
      )}
    </div>
  );
}

export default ConnectionStatusBadge;
