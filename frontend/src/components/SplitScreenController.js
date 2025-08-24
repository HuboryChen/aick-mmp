import React from 'react';
import { Card, Button, Space, Typography, Row, Col } from 'antd';
import { 
  LayoutOutlined, 
  AppstoreOutlined, 
  BorderOutlined,
  TableOutlined 
} from '@ant-design/icons';

const { Text } = Typography;

const SplitScreenController = ({ layout, onLayoutChange, disabled = false }) => {
  const layouts = [
    { value: '1', label: '单屏', icon: <BorderOutlined />, description: '1x1' },
    { value: '4', label: '四分屏', icon: <AppstoreOutlined />, description: '2x2' },
    { value: '9', label: '九分屏', icon: <TableOutlined />, description: '3x3' },
    { value: '16', label: '十六分屏', icon: <LayoutOutlined />, description: '4x4' }
  ];

  return (
    <Card 
      title={
        <Space>
          <LayoutOutlined />
          <span>分屏布局</span>
        </Space>
      }
      size="small"
      style={{ width: '100%' }}
    >
      <Row gutter={[8, 8]}>
        {layouts.map(layoutOption => (
          <Col span={12} key={layoutOption.value}>
            <Button
              type={layout === layoutOption.value ? 'primary' : 'default'}
              block
              disabled={disabled}
              onClick={() => onLayoutChange(layoutOption.value)}
              style={{
                height: '60px',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '8px'
              }}
            >
              <div style={{ fontSize: '20px', marginBottom: '4px' }}>
                {layoutOption.icon}
              </div>
              <div style={{ fontSize: '12px', lineHeight: '1.2' }}>
                <div>{layoutOption.label}</div>
                <Text type="secondary" style={{ fontSize: '10px' }}>
                  {layoutOption.description}
                </Text>
              </div>
            </Button>
          </Col>
        ))}
      </Row>
      
      <div style={{ marginTop: '12px', padding: '8px', background: '#f5f5f5', borderRadius: '4px' }}>
        <Text type="secondary" style={{ fontSize: '12px' }}>
          当前布局: <strong>{layouts.find(l => l.value === layout)?.label}</strong>
        </Text>
      </div>
    </Card>
  );
};

export default SplitScreenController;