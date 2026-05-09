/**
 * 视频墙画质选择器组件
 * 
 * 提供画质预设（480p/720p/1080p）和码率滑块调整功能
 */

import React from 'react';
import { Card, Radio, Slider, Space, Typography, Row, Col, Badge } from 'antd';
import { 
  VideoCameraOutlined,
  ThunderboltOutlined
} from '@ant-design/icons';

const { Text } = Typography;

/**
 * 画质选项配置
 */
const qualityOptions = [
  { 
    value: '480p', 
    label: '480p', 
    description: '标清',
    bitrateRange: [512, 2048],
    defaultBitrate: 1024,
  },
  { 
    value: '720p', 
    label: '720p', 
    description: '高清',
    bitrateRange: [1024, 4096],
    defaultBitrate: 2048,
  },
  { 
    value: '1080p', 
    label: '1080p', 
    description: '全高清',
    bitrateRange: [2048, 8192],
    defaultBitrate: 4096,
  },
];

/**
 * 获取画质选项
 */
const getQualityOption = (quality) => {
  return qualityOptions.find(q => q.value === quality) || qualityOptions[1];
};

/**
 * 画质选择器组件
 */
const QualitySelector = ({ 
  value,
  quality,
  bitrate,
  onChange,
  onQualityChange,
  onBitrateChange,
  disabled = false,
}) => {
  // 兼容 quality/value prop
  const currentQuality = quality || value;
  const currentQualityOption = getQualityOption(currentQuality);
  const currentBitrate = bitrate ?? currentQualityOption.defaultBitrate;

  // 处理画质变更
  const handleQualityChange = (newQuality) => {
    const newOption = getQualityOption(newQuality);
    // 如果当前码率不在新画质范围内，调整码率
    let newBitrate = currentBitrate;
    if (currentBitrate < newOption.bitrateRange[0]) {
      newBitrate = newOption.bitrateRange[0];
    } else if (currentBitrate > newOption.bitrateRange[1]) {
      newBitrate = newOption.bitrateRange[1];
    }
    
    // 回调
    if (onQualityChange) {
      onQualityChange(newQuality);
    }
    if (onChange) {
      onChange(newQuality);
    }
    if (onBitrateChange && newBitrate !== currentBitrate) {
      onBitrateChange(newBitrate);
    }
  };

  // 处理码率变更
  const handleBitrateChange = (newBitrate) => {
    if (onBitrateChange) {
      onBitrateChange(newBitrate);
    }
  };

  // 格式化码率显示
  const formatBitrate = (kbps) => {
    if (kbps >= 1024) {
      return `${(kbps / 1024).toFixed(1)} Mbps`;
    }
    return `${kbps} kbps`;
  };

  return (
    <div className="quality-selector">
      {/* 画质选择 */}
      <div className="quality-selector-section" style={{ marginBottom: '16px' }}>
        <div className="quality-selector-title" style={{ marginBottom: '12px' }}>
          <Space>
            <VideoCameraOutlined />
            <span>画质</span>
          </Space>
        </div>
        <Radio.Group
          value={currentQuality}
          onChange={(e) => handleQualityChange(e.target.value)}
          disabled={disabled}
          style={{ width: '100%' }}
        >
          <Row gutter={[8, 8]}>
            {qualityOptions.map(option => (
              <Col span={8} key={option.value}>
                <Radio.Button
                  value={option.value}
                  style={{
                    width: '100%',
                    textAlign: 'center',
                    height: '50px',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  <div style={{ fontWeight: 'bold' }}>{option.label}</div>
                  <div style={{ fontSize: '10px', opacity: 0.7 }}>{option.description}</div>
                </Radio.Button>
              </Col>
            ))}
          </Row>
        </Radio.Group>
      </div>

      {/* 码率滑块 */}
      <div className="quality-selector-section">
        <div className="quality-selector-title" style={{ marginBottom: '12px' }}>
          <Space>
            <ThunderboltOutlined />
            <span>码率</span>
            <Text type="secondary" style={{ fontSize: '12px' }}>
              {formatBitrate(currentBitrate)}
            </Text>
          </Space>
        </div>
        <Slider
          min={currentQualityOption.bitrateRange[0]}
          max={currentQualityOption.bitrateRange[1]}
          step={128}
          value={currentBitrate}
          onChange={handleBitrateChange}
          disabled={disabled}
          tooltip={{ formatter: formatBitrate }}
          marks={{
            [currentQualityOption.bitrateRange[0]]: formatBitrate(currentQualityOption.bitrateRange[0]),
            [currentQualityOption.bitrateRange[1]]: formatBitrate(currentQualityOption.bitrateRange[1]),
          }}
        />
        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '4px' }}>
          <Text type="secondary" style={{ fontSize: '10px' }}>低码率</Text>
          <Text type="secondary" style={{ fontSize: '10px' }}>高码率</Text>
        </div>
      </div>
    </div>
  );
};

export default QualitySelector;
