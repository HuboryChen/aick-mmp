import React from 'react';
import { Card, Select, Slider, Space, Typography, Row, Col, Tag } from 'antd';
import { SettingOutlined, ThunderboltOutlined } from '@ant-design/icons';

const { Text } = Typography;
const { Option } = Select;

const VideoQualityController = ({ 
  quality, 
  onQualityChange, 
  bitrate, 
  onBitrateChange,
  disabled = false 
}) => {
  const qualityOptions = [
    { value: '480p', label: '标清 (480p)', bitrate: 1024, color: 'blue' },
    { value: '720p', label: '高清 (720p)', bitrate: 2048, color: 'green' },
    { value: '1080p', label: '全高清 (1080p)', bitrate: 4096, color: 'purple' }
  ];

  const getBitrateColor = (value) => {
    if (value < 1500) return '#52c41a';  // 绿色 - 低
    if (value < 3000) return '#faad14';  // 橙色 - 中
    return '#f5222d';  // 红色 - 高
  };

  const getBitrateLevel = (value) => {
    if (value < 1500) return '低';
    if (value < 3000) return '中';
    return '高';
  };

  const handleQualityChange = (value) => {
    const selected = qualityOptions.find(option => option.value === value);
    if (selected) {
      onQualityChange(value);
      onBitrateChange(selected.bitrate);
    }
  };

  const currentQuality = qualityOptions.find(option => option.value === quality);

  return (
    <Card 
      title={
        <Space>
          <SettingOutlined />
          <span>画质控制</span>
        </Space>
      }
      size="small"
      style={{ width: '100%' }}
    >
      <Space direction="vertical" style={{ width: '100%' }}>
        {/* 预设画质选择 */}
        <div>
          <Text strong style={{ fontSize: '12px', marginBottom: '8px', display: 'block' }}>
            预设画质
          </Text>
          <Select
            value={quality}
            onChange={handleQualityChange}
            disabled={disabled}
            style={{ width: '100%' }}
            size="small"
          >
            {qualityOptions.map(option => (
              <Option key={option.value} value={option.value}>
                <Space>
                  {option.label}
                  <Tag color={option.color} size="small">
                    {option.bitrate}kbps
                  </Tag>
                </Space>
              </Option>
            ))}
          </Select>
        </div>

        {/* 自定义码率 */}
        <div>
          <Row justify="space-between" align="middle" style={{ marginBottom: '8px' }}>
            <Col>
              <Text strong style={{ fontSize: '12px' }}>
                自定义码率
              </Text>
            </Col>
            <Col>
              <Space>
                <Tag color={getBitrateColor(bitrate)} size="small">
                  {getBitrateLevel(bitrate)}
                </Tag>
                <Text style={{ fontSize: '11px', color: getBitrateColor(bitrate) }}>
                  {bitrate} kbps
                </Text>
              </Space>
            </Col>
          </Row>
          
          <Slider
            min={512}
            max={8192}
            step={128}
            value={bitrate}
            onChange={onBitrateChange}
            disabled={disabled}
            tooltip={{
              formatter: (value) => `${value} kbps`
            }}
            marks={{
              512: '512k',
              2048: '2M',
              4096: '4M',
              8192: '8M'
            }}
          />
        </div>

        {/* 当前状态信息 */}
        <div style={{ 
          padding: '8px', 
          background: '#f5f5f5', 
          borderRadius: '4px',
          border: `1px solid ${currentQuality?.color === 'blue' ? '#1890ff' : currentQuality?.color === 'green' ? '#52c41a' : '#722ed1'}`
        }}>
          <Row justify="space-between" align="middle">
            <Col>
              <Space>
                <ThunderboltOutlined style={{ color: currentQuality?.color === 'blue' ? '#1890ff' : currentQuality?.color === 'green' ? '#52c41a' : '#722ed1' }} />
                <Text style={{ fontSize: '12px' }}>
                  当前: <strong>{quality}</strong>
                </Text>
              </Space>
            </Col>
            <Col>
              <Text type="secondary" style={{ fontSize: '11px' }}>
                码率: {bitrate}kbps
              </Text>
            </Col>
          </Row>
        </div>
      </Space>
    </Card>
  );
};

export default VideoQualityController;