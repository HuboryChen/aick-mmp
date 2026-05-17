/**
 * 视频墙设置抽屉组件
 *
 * 提供完整的视频墙设置界面，包括布局选择、画质设置和预设管理。
 * 所有配置状态由父组件（VideoWall）通过 props 传入，本组件只维护编辑阶段的本地临时状态。
 */

import React, { useState, useEffect } from 'react';
import { Drawer, Button, Divider, Space, Skeleton, Alert, message } from 'antd';
import { SettingOutlined, ReloadOutlined, CheckOutlined, LoadingOutlined } from '@ant-design/icons';
import LayoutSelector from './LayoutSelector';
import QualitySelector from './QualitySelector';
import PresetSelector from './PresetSelector';
import './VideoWallSettingsDrawer.css';

/**
 * 视频墙设置抽屉组件
 */
const VideoWallSettingsDrawer = ({
  visible,
  onClose,
  open = false,
  onOpenChange,
  onLayoutChange,
  onQualityChange,
  onBitrateChange,

  // 从父组件传入的 hook 状态和方法（消除独立 hook 实例）
  config,
  presets,
  builtInPresets,
  activePresetId,
  isLoading,
  isLoaded,
  error,
  reload,
  saveConfigImmediately,
  applyPreset,
  createPreset,
  updatePreset,
  deletePreset,
  setAsDefaultPreset,
  reorderPresets,
  resetToDefaults,
  canEditPreset,
  canDeletePreset,
  isBuiltInPreset,
}) => {
  // 本地状态用于编辑（点击"完成"前不提交）
  const [localLayout, setLocalLayout] = useState('4');
  const [localQuality, setLocalQuality] = useState('720p');
  const [localBitrate, setLocalBitrate] = useState(2048);
  // 保存状态：saving 为 true 时按钮显示加载/成功反馈
  const [saving, setSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);

  // 当抽屉打开时，从父组件同步最新配置到本地编辑状态
  useEffect(() => {
    if (visible || open) {
      setLocalLayout(config.layout);
      setLocalQuality(config.quality);
      setLocalBitrate(config.bitrate);
    }
  }, [visible, open, config]);

  // 处理布局变更
  const handleLayoutChange = (newLayout) => {
    setLocalLayout(newLayout);
  };

  // 处理画质变更
  const handleQualityChange = (newQuality) => {
    setLocalQuality(newQuality);
  };

  // 处理码率变更
  const handleBitrateChange = (newBitrate) => {
    setLocalBitrate(newBitrate);
  };

  // 应用当前设置并即时持久化
  const handleApplySettings = async () => {
    setSaving(true);
    setSaveSuccess(false);
    try {
      // 即时保存到父组件的 hook（localStorage + DB）
      await saveConfigImmediately({
        ...config,
        layout: localLayout,
        quality: localQuality,
        bitrate: localBitrate,
      });

      // 通知父组件更新本地 UI 状态
      if (onLayoutChange) {
        onLayoutChange(localLayout);
      }
      if (onQualityChange) {
        onQualityChange(localQuality);
      }
      if (onBitrateChange) {
        onBitrateChange(localBitrate);
      }

      message.success('设置已应用', 2);
      // 切换到成功状态，展示绿色对勾
      setSaveSuccess(true);
      // 保持成功显示 1.5 秒让用户看到反馈
      await new Promise((r) => setTimeout(r, 1500));
    } catch {
      message.error('设置保存失败', 2);
    } finally {
      setSaving(false);
      setSaveSuccess(false);
    }
  };

  // 选择预设
  const handleSelectPreset = (preset) => {
    applyPreset(preset.id);
    setLocalLayout(preset.layout);
    setLocalQuality(preset.quality);
    setLocalBitrate(preset.bitrate);
    message.success(`已应用预设: ${preset.presetName || preset.name}`, 2);
  };

  // 创建新预设
  const handleCreatePreset = async (name) => {
    try {
      await createPreset(name, {
        layout: localLayout,
        quality: localQuality,
        bitrate: localBitrate,
      });
      message.success('预设创建成功');
    } catch (err) {
      message.error('预设创建失败');
    }
  };

  // 更新预设
  const handleUpdatePreset = async (presetId, updates) => {
    try {
      await updatePreset(presetId, updates);
      message.success('预设更新成功');
    } catch (err) {
      message.error('预设更新失败');
    }
  };

  // 删除预设
  const handleDeletePreset = async (preset) => {
    try {
      await deletePreset(preset.id);
      message.success('预设已删除');
    } catch (err) {
      message.error('预设删除失败');
    }
  };

  // 设为默认
  const handleSetDefault = async (preset) => {
    try {
      await setAsDefaultPreset(preset.id);
      message.success('已设为默认预设');
    } catch (err) {
      message.error('设置默认失败');
    }
  };

  // 重置设置
  const handleReset = () => {
    resetToDefaults();
    setLocalLayout('4');
    setLocalQuality('720p');
    setLocalBitrate(2048);
    message.success('已重置为默认设置');
  };

  // 完成并关闭
  const handleDone = async () => {
    await handleApplySettings();
    if (onClose) {
      onClose();
    }
    if (onOpenChange) {
      onOpenChange(false);
    }
  };

  return (
    <Drawer
      title={
        <Space>
          <SettingOutlined />
          <span>视频墙设置</span>
        </Space>
      }
      placement="right"
      width={360}
      open={visible || open}
      onClose={handleDone}
      className="video-wall-settings-drawer"
      maskClosable={false}
      closable={false}
      footer={
        <div className="drawer-footer">
          <Button
            icon={<ReloadOutlined />}
            onClick={handleReset}
            disabled={isLoading && !isLoaded}
          >
            重置
          </Button>
          <Space>
            <Button onClick={() => {
              if (onClose) onClose();
              if (onOpenChange) onOpenChange(false);
            }}>
              取消
            </Button>
            <Button
              type="primary"
              icon={saving ? (saveSuccess ? <CheckOutlined style={{ color: '#52c41a' }} /> : <LoadingOutlined />) : <CheckOutlined />}
              onClick={handleDone}
              disabled={(isLoading && !isLoaded) || saving}
            >
              {saving ? (saveSuccess ? '已保存' : '保存中...') : '完成'}
            </Button>
          </Space>
        </div>
      }
    >
      {/* 错误提示横幅 */}
      {error && (
        <div className="drawer-error-banner">
          <Alert
            type="error"
            showIcon
            message={error}
            action={
              <Button size="small" danger onClick={reload}>
                重试
              </Button>
            }
          />
        </div>
      )}

      {/* 初始加载骨架屏 */}
      {isLoading && !isLoaded ? (
        <div className="drawer-loading-skeleton">
          <Skeleton active paragraph={{ rows: 1 }} title={{ width: '60%' }} style={{ marginBottom: 16 }} />
          <Skeleton active paragraph={{ rows: 2 }} title={{ width: '40%' }} style={{ marginBottom: 16 }} />
          <Skeleton active paragraph={{ rows: 3 }} title={{ width: '50%' }} />
        </div>
      ) : (
        <>
          {/* 布局设置区域 */}
          <div className="drawer-section">
            <LayoutSelector
              value={localLayout}
              onChange={handleLayoutChange}
            />
          </div>

          <Divider />

          {/* 画质设置区域 */}
          <div className="drawer-section">
            <QualitySelector
              quality={localQuality}
              bitrate={localBitrate}
              onQualityChange={handleQualityChange}
              onBitrateChange={handleBitrateChange}
            />
          </div>

          <Divider />

          {/* 预设管理区域 */}
          <div className="drawer-section">
            <PresetSelector
              presets={presets}
              builtInPresets={builtInPresets}
              activePresetId={activePresetId}
              isBuiltInPreset={isBuiltInPreset}
              canEditPreset={canEditPreset}
              canDeletePreset={canDeletePreset}
              onSelect={handleSelectPreset}
              onCreate={handleCreatePreset}
              onEdit={handleUpdatePreset}
              onDelete={handleDeletePreset}
              onSetDefault={handleSetDefault}
              onReorder={reorderPresets}
              loading={isLoading}
            />
          </div>

          {/* 当前设置预览 */}
          <div className="settings-preview">
            <div className="preview-title">当前设置预览</div>
            <div className="preview-content">
              <div className="preview-item">
                <span className="preview-label">布局:</span>
                <span className="preview-value">{localLayout}宫格</span>
              </div>
              <div className="preview-item">
                <span className="preview-label">画质:</span>
                <span className="preview-value">{localQuality}</span>
              </div>
              <div className="preview-item">
                <span className="preview-label">码率:</span>
                <span className="preview-value">{localBitrate} kbps</span>
              </div>
            </div>
          </div>
        </>
      )}
    </Drawer>
  );
};

export default VideoWallSettingsDrawer;
