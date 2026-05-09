/**
 * 视频墙布局选择器组件
 * 
 * 提供 2x2 网格布局选择功能，支持 1/4/9/16 分屏切换
 */

import React from 'react';
import { Card, Button, Space, Typography, Row, Col, Tooltip } from 'antd';
import { 
  LayoutOutlined, 
  AppstoreOutlined, 
  BorderOutlined,
  TableOutlined,
  BankOutlined 
} from '@ant-design/icons';

const { Text } = Typography;

/**
 * 布局选项配置
 */
const layouts = [
  { 
    value: '1', 
    label: '单屏', 
    description: '1x1',
    cols: 1,
    rows: 1,
    icon: <BorderOutlined /> 
  },
  { 
    value: '4', 
    label: '四分屏', 
    description: '2x2',
    cols: 2,
    rows: 2,
    icon: <AppstoreOutlined /> 
  },
  { 
    value: '9', 
    label: '九分屏', 
    description: '3x3',
    cols: 3,
    rows: 3,
    icon: <TableOutlined /> 
  },
  { 
    value: '16', 
    label: '十六分屏', 
    description: '4x4',
    cols: 4,
    rows: 4,
    icon: <BankOutlined /> 
  }
];

/**
 * 渲染布局网格可视化图标
 */
const LayoutGridIcon = ({ cols, rows }) => {
  const cells = [];
  const cellSize = 6;
  const gap = 1;
  
  for (let i = 0; i < rows; i++) {
    for (let j = 0; j < cols; j++) {
      cells.push(
        <div
          key={`cell-${i}-${j}`}
          style={{
            width: cellSize,
            height: cellSize,
            backgroundColor: 'currentColor',
            borderRadius: '1px',
            opacity: 0.8,
          }}
        />
      );
    }
  }
  
  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: `repeat(${cols}, ${cellSize}px)`,
        gap: `${gap}px`,
      }}
    >
      {cells}
    </div>
  );
};

/**
 * 布局选择器组件
 */
const LayoutSelector = ({ 
  value,
  layout,
  onChange, 
  onLayoutChange,
  disabled = false,
}) => {
  // 兼容 layout 和 value prop
  const currentLayout = layout || value;
  // 兼容 onLayoutChange 和 onChange prop
  const handleLayoutChangeCallback = onLayoutChange || onChange;

  return (
    <div className="layout-selector">
      <div className="layout-selector-title" style={{ marginBottom: '12px' }}>
        <Space>
          <LayoutOutlined />
          <span>布局</span>
        </Space>
      </div>
      <Row gutter={[8, 8]}>
        {layouts.map(layoutOption => (
          <Col span={12} key={layoutOption.value}>
            <Tooltip 
              title={layoutOption.description}
              placement="top"
            >
              <Button
                type={currentLayout === layoutOption.value ? 'primary' : 'default'}
                block
                disabled={disabled}
                onClick={() => handleLayoutChangeCallback?.(layoutOption.value)}
                style={{ 
                  height: '60px',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '4px'
                }}
              >
                <LayoutGridIcon cols={layoutOption.cols} rows={layoutOption.rows} />
                <span style={{ fontSize: '12px' }}>{layoutOption.label}</span>
              </Button>
            </Tooltip>
          </Col>
        ))}
      </Row>
    </div>
  );
};

export default LayoutSelector;
