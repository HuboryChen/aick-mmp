import React, { useState } from 'react';
import { Modal, Button, Spin, Space } from 'antd';

const QUALITY_DESCRIPTIONS = {
  '1080p': '高清画质（1080P），建议在网络良好的环境下使用',
  '720p': '标清画质（720P），平衡画质与流量',
  '480p': '流畅画质（480P），适合网络不稳定的情况',
  '360p': '省流画质（360P），适合低带宽网络',
};

export function QualityConfirmDialog({
  isOpen,
  currentQuality,
  targetQuality,
  onConfirm,
  onCancel,
  isLoading = false,
  error = null,
}) {
  const [localError, setLocalError] = useState(null);
  const [retryCount, setRetryCount] = useState(0);

  const handleConfirm = async () => {
    setLocalError(null);
    try {
      setRetryCount(0);
      await onConfirm();
    } catch (e) {
      setLocalError(e.message || '画质切换失败，请重试');
      setRetryCount((prev) => prev + 1);
    }
  };

  const handleCancel = () => {
    setLocalError(null);
    setRetryCount(0);
    onCancel();
  };

  const displayError = error || localError;

  return (
    <Modal
      open={isOpen}
      onCancel={handleCancel}
      title="确认画质切换"
      footer={null}
      centered
      width={420}
      className="quality-confirm-dialog"
      closable={!isLoading}
      maskClosable={!isLoading}
      destroyOnClose
    >
      <div className="dialog-content">
        <div className="quality-compare">
          <div className="quality-item current">
            <span className="label">当前画质</span>
            <span className="value">{currentQuality}</span>
          </div>
          <div className="arrow">→</div>
          <div className="quality-item target">
            <span className="label">目标画质</span>
            <span className="value">{targetQuality}</span>
          </div>
        </div>

        <p className="description">
          {QUALITY_DESCRIPTIONS[targetQuality] || '画质切换将重新建立视频连接'}
        </p>

        {displayError && (
          <div className="error-message">
            <span className="error-icon">⚠️</span>
            <span>{displayError}</span>
          </div>
        )}

        <div className="dialog-actions">
          <Button
            onClick={handleCancel}
            disabled={isLoading}
          >
            取消
          </Button>
          <Button
            type="primary"
            onClick={handleConfirm}
            disabled={isLoading}
          >
            {isLoading ? (
              <Space>
                <Spin size="small" />
                <span>切换中...</span>
              </Space>
            ) : displayError && retryCount > 0 ? (
              '重试'
            ) : (
              '确定'
            )}
          </Button>
        </div>
      </div>
    </Modal>
  );
}

export default QualityConfirmDialog;
