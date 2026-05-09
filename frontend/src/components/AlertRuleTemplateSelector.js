import React, { useState, useEffect } from 'react';
import { Card, Select, Tag, Space, Button, Empty, Spin, Input, message, Divider } from 'antd';
import { SearchOutlined, CheckCircleOutlined, FileTextOutlined, StarOutlined } from '@ant-design/icons';
import { alertRuleApi } from '../utils/api';

const { Option } = Select;

/**
 * 告警规则模板选择器组件
 */
const AlertRuleTemplateSelector = ({ value, onChange, alertType, disabled = false }) => {
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [selectedTemplate, setSelectedTemplate] = useState(value);

  useEffect(() => {
    loadTemplates();
  }, [alertType]);

  useEffect(() => {
    setSelectedTemplate(value);
  }, [value]);

  const loadTemplates = async () => {
    setLoading(true);
    try {
      let response;
      if (alertType) {
        response = await alertRuleApi.getTemplatesByType(alertType);
      } else {
        response = await alertRuleApi.getRecommendedTemplates();
      }
      setTemplates(response.data || []);
    } catch (error) {
      console.error('Failed to load templates:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async () => {
    if (!searchKeyword.trim()) {
      loadTemplates();
      return;
    }
    
    setLoading(true);
    try {
      const response = await alertRuleApi.searchTemplates(searchKeyword);
      setTemplates(response.data || []);
    } catch (error) {
      message.error('搜索模板失败');
    } finally {
      setLoading(false);
    }
  };

  const selectTemplate = (template) => {
    setSelectedTemplate(template);
    onChange?.(template);
  };

  const clearSelection = () => {
    setSelectedTemplate(null);
    onChange?.(null);
  };

  // 告警类型映射
  const alertTypeLabels = {
    'CPU_USAGE': 'CPU使用率',
    'MEMORY_USAGE': '内存使用率',
    'DISK_USAGE': '磁盘使用率',
    'NETWORK_LATENCY': '网络延迟',
    'CAMERA_OFFLINE': '摄像头离线',
    'CAMERA_ERROR': '摄像头错误',
    'EDGE_NODE_OFFLINE': '边缘节点离线',
    'STREAM_INTERRUPTED': '视频流中断',
    'MOTION_DETECTED': '移动侦测',
    'RECORDING_FAILED': '录像失败',
    'SYSTEM_ERROR': '系统错误',
  };

  // 告警级别颜色
  const levelColors = {
    'INFO': 'blue',
    'WARNING': 'orange',
    'ERROR': 'red',
    'CRITICAL': 'purple',
  };

  const renderTemplateCard = (template) => {
    const isSelected = selectedTemplate?.id === template.id;
    const isSystem = template.isSystem;
    const usageCount = template.usageCount || 0;

    return (
      <Card
        key={template.id}
        size="small"
        className={`template-card ${isSelected ? 'selected' : ''}`}
        onClick={() => !disabled && selectTemplate(template)}
        style={{ 
          cursor: disabled ? 'default' : 'pointer',
          borderColor: isSelected ? '#1890ff' : undefined,
          backgroundColor: isSelected ? '#e6f7ff' : undefined,
        }}
      >
        <div className="template-header">
          <Space>
            <FileTextOutlined />
            <span className="template-name">{template.name}</span>
            {isSystem && <Tag color="gold" icon={<StarOutlined />}>系统</Tag>}
          </Space>
          {isSelected && <CheckCircleOutlined style={{ color: '#1890ff' }} />}
        </div>
        
        <div className="template-info" style={{ marginTop: 8, fontSize: 12, color: '#666' }}>
          <Space split={<Divider type="vertical" />}>
            <span>类型: {alertTypeLabels[template.alertType] || template.alertType}</span>
            <span>
              <Tag color={levelColors[template.level]} style={{ marginRight: 4 }}>
                {template.level}
              </Tag>
            </span>
          </Space>
        </div>

        {template.description && (
          <div className="template-desc" style={{ marginTop: 8, fontSize: 12, color: '#999' }}>
            {template.description.length > 80 
              ? template.description.substring(0, 80) + '...' 
              : template.description}
          </div>
        )}

        <div className="template-meta" style={{ marginTop: 8, fontSize: 11, color: '#999' }}>
          使用次数: {usageCount} | 
          {template.category && ` 分类: ${template.category}`}
        </div>
      </Card>
    );
  };

  return (
    <div className="alert-rule-template-selector">
      {/* 搜索栏 */}
      {!disabled && (
        <div className="template-search" style={{ marginBottom: 16 }}>
          <Space.Compact style={{ width: '100%' }}>
            <Input
              placeholder="搜索模板..."
              prefix={<SearchOutlined />}
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              onPressEnter={handleSearch}
              allowClear
            />
            <Button type="primary" onClick={handleSearch}>搜索</Button>
          </Space.Compact>
        </div>
      )}

      {/* 已选模板 */}
      {selectedTemplate && (
        <Card 
          size="small" 
          title="已选模板" 
          className="selected-template-card"
          extra={
            !disabled && (
              <Button type="link" danger size="small" onClick={clearSelection}>
                清除选择
              </Button>
            )
          }
        >
          <div className="template-name" style={{ fontWeight: 'bold' }}>
            {selectedTemplate.name}
          </div>
          {selectedTemplate.description && (
            <div style={{ fontSize: 12, color: '#666', marginTop: 4 }}>
              {selectedTemplate.description}
            </div>
          )}
        </Card>
      )}

      {/* 模板列表 */}
      <div className="template-list" style={{ marginTop: 16 }}>
        {loading ? (
          <div style={{ textAlign: 'center', padding: 24 }}>
            <Spin />
          </div>
        ) : templates.length === 0 ? (
          <Empty description="暂无可用模板" />
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 12 }}>
            {templates.map(template => renderTemplateCard(template))}
          </div>
        )}
      </div>

      <style>{`
        .alert-rule-template-selector .template-card {
          transition: all 0.2s;
        }
        .alert-rule-template-selector .template-card:hover:not(.selected) {
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }
        .alert-rule-template-selector .template-card.selected {
          box-shadow: 0 0 0 2px #1890ff;
        }
        .alert-rule-template-selector .template-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
        }
        .alert-rule-template-selector .template-name {
          font-weight: 500;
        }
        .alert-rule-template-selector .selected-template-card {
          background-color: #f6ffed;
          border-color: #b7eb8f;
        }
      `}</style>
    </div>
  );
};

export default AlertRuleTemplateSelector;
