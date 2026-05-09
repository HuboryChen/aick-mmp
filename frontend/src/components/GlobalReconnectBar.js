import React from 'react';
import { Alert } from 'antd';
import { ConnectionState } from '../contexts/StreamHealthContext';
import './GlobalReconnectBar.css';

/**
 * GlobalReconnectBar - 显示在 VideoWall 顶部的全局重连状态栏
 *
 * Props:
 *   camerasInfo: Array<{
 *     cameraId: string,
 *     cameraName: string,
 *     connectionState: ConnectionState,
 *     retryCount: number
 *   }>
 *   onRetryAll: function (可选) - 点击重试全部按钮的回调
 */
export function GlobalReconnectBar({ camerasInfo = [], onRetryAll }) {
  if (!camerasInfo || camerasInfo.length === 0) {
    return null;
  }

  // 单个摄像头时隐藏（由 ConnectionStatusBadge 接管）
  if (camerasInfo.length <= 1) {
    return null;
  }

  const reconnectingCameras = camerasInfo.filter(
    (c) =>
      c.connectionState === ConnectionState.RECONNECTING ||
      c.connectionState === ConnectionState.CONNECTING ||
      c.connectionState === ConnectionState.DISCONNECTED
  );

  const failedCameras = camerasInfo.filter(
    (c) => c.connectionState === ConnectionState.FAILED
  );

  // 全部正常时隐藏
  if (reconnectingCameras.length === 0 && failedCameras.length === 0) {
    return null;
  }

  const maxRetryCount = Math.max(
    ...reconnectingCameras.map((c) => c.retryCount || 0),
    0
  );

  const reconnectingNames = reconnectingCameras
    .map((c) => c.cameraName || c.cameraId)
    .join('、');
  const failedNames = failedCameras
    .map((c) => c.cameraName || c.cameraId)
    .join('、');

  return (
    <div className="global-reconnect-bar">
      {reconnectingCameras.length > 0 && (
        <Alert
          type="warning"
          showIcon
          icon={<span className="bar-icon">🔄</span>}
          message={
            <span>
              {reconnectingCameras.length} 个摄像头正在重连
              {maxRetryCount > 0 && ` (第 ${maxRetryCount} 次)`}
              {reconnectingNames && (
                <span className="camera-names">: {reconnectingNames}</span>
              )}
            </span>
          }
          className="reconnect-alert reconnecting"
        />
      )}
      {failedCameras.length > 0 && (
        <Alert
          type="error"
          showIcon
          icon={<span className="bar-icon">⚠️</span>}
          message={
            <span>
              {failedCameras.length} 个摄像头连接失败
              {failedNames && (
                <span className="camera-names">: {failedNames}</span>
              )}
              {onRetryAll && (
                <span
                  className="retry-all-link"
                  onClick={onRetryAll}
                  role="button"
                  tabIndex={0}
                >
                  全部重试
                </span>
              )}
            </span>
          }
          className="reconnect-alert failed"
        />
      )}
    </div>
  );
}

export default GlobalReconnectBar;
