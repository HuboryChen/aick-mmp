import React, { useState, useEffect } from 'react';
import { Modal, List, Card, Tag, Button, Input, Space, Typography } from 'antd';
import { VideoCameraOutlined, SearchOutlined, CheckCircleOutlined } from '@ant-design/icons';

const { Search } = Input;
const { Text } = Typography;

const CameraSelector = ({ visible, cameras, selectedCameras, onSelect, onCancel }) => {
  const [filteredCameras, setFilteredCameras] = useState([]);
  const [searchText, setSearchText] = useState('');

  useEffect(() => {
    if (cameras) {
      const filtered = cameras.filter(camera => 
        camera.name.toLowerCase().includes(searchText.toLowerCase()) ||
        camera.location.toLowerCase().includes(searchText.toLowerCase())
      );
      setFilteredCameras(filtered);
    }
  }, [cameras, searchText]);

  const handleSearch = (value) => {
    setSearchText(value);
  };

  const handleSelect = (camera) => {
    onSelect(camera);
  };

  const isSelected = (cameraId) => {
    return selectedCameras.some(camera => camera && camera.id === cameraId);
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'ONLINE': return 'green';
      case 'OFFLINE': return 'red';
      case 'CONNECTING': return 'orange';
      default: return 'default';
    }
  };

  const getStatusText = (status) => {
    switch (status) {
      case 'ONLINE': return '在线';
      case 'OFFLINE': return '离线';
      case 'CONNECTING': return '连接中';
      default: return '未知';
    }
  };

  return (
    <Modal
      title="选择摄像头"
      open={visible}
      onCancel={onCancel}
      footer={null}
      width={600}
      bodyStyle={{ maxHeight: '60vh', overflow: 'auto' }}
    >
      <div style={{ marginBottom: '16px' }}>
        <Search
          placeholder="搜索摄像头名称或位置"
          allowClear
          enterButton={<SearchOutlined />}
          size="large"
          onSearch={handleSearch}
          onChange={(e) => handleSearch(e.target.value)}
        />
      </div>

      <List
        grid={{ gutter: 16, column: 2 }}
        dataSource={filteredCameras}
        renderItem={camera => (
          <List.Item>
            <Card
              size="small"
              hoverable
              className={isSelected(camera.id) ? 'selected-camera' : ''}
              onClick={() => handleSelect(camera)}
              style={{
                border: isSelected(camera.id) ? '2px solid #1890ff' : '1px solid #d9d9d9',
                position: 'relative'
              }}
            >
              {isSelected(camera.id) && (
                <div style={{
                  position: 'absolute',
                  top: '8px',
                  right: '8px',
                  zIndex: 1
                }}>
                  <CheckCircleOutlined style={{ color: '#1890ff', fontSize: '16px' }} />
                </div>
              )}
              
              <Card.Meta
                avatar={<VideoCameraOutlined style={{ fontSize: '24px', color: '#1890ff' }} />}
                title={
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontSize: '14px' }}>{camera.name}</span>
                    <Tag color={getStatusColor(camera.status)} style={{ fontSize: '10px' }}>
                      {getStatusText(camera.status)}
                    </Tag>
                  </div>
                }
                description={
                  <div>
                    <Text type="secondary" style={{ fontSize: '12px' }}>
                      {camera.location}
                    </Text>
                    <br />
                    <Text type="secondary" style={{ fontSize: '11px' }}>
                      {camera.resolution} | {camera.protocol}
                    </Text>
                  </div>
                }
              />
            </Card>
          </List.Item>
        )}
      />

      {filteredCameras.length === 0 && (
        <div style={{ 
          textAlign: 'center', 
          padding: '40px',
          color: '#999'
        }}>
          <VideoCameraOutlined style={{ fontSize: '48px', marginBottom: '16px' }} />
          <div>没有找到匹配的摄像头</div>
        </div>
      )}
    </Modal>
  );
};

export default CameraSelector;