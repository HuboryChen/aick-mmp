import React, { useState } from 'react';
import { Card, Typography, DatePicker, Select, Button, Row, Col, List, Tag, Space } from 'antd';
import { PlayCircleOutlined, DownloadOutlined, SearchOutlined } from '@ant-design/icons';

const { Title } = Typography;
const { RangePicker } = DatePicker;
const { Option } = Select;

const Playback = () => {
  const [searchParams, setSearchParams] = useState({
    camera: null,
    dateRange: null,
    region: null
  });
  const [recordings, setRecordings] = useState([]);
  const [loading, setLoading] = useState(false);

  const mockRecordings = [
    {
      id: 1,
      camera: 'Camera-A-01',
      region: '地区A',
      startTime: '2024-01-15 09:00:00',
      endTime: '2024-01-15 10:00:00',
      duration: '01:00:00',
      size: '1.2 GB',
      quality: '1080P'
    },
    {
      id: 2,
      camera: 'Camera-A-02',
      region: '地区A',
      startTime: '2024-01-15 09:30:00',
      endTime: '2024-01-15 10:30:00',
      duration: '01:00:00',
      size: '856 MB',
      quality: '720P'
    },
    {
      id: 3,
      camera: 'Camera-B-01',
      region: '地区B',
      startTime: '2024-01-15 08:00:00',
      endTime: '2024-01-15 09:00:00',
      duration: '01:00:00',
      size: '1.1 GB',
      quality: '1080P'
    }
  ];

  const handleSearch = () => {
    setLoading(true);
    // 模拟搜索
    setTimeout(() => {
      setRecordings(mockRecordings);
      setLoading(false);
    }, 1000);
  };

  const handlePlay = (recording) => {
    console.log('播放录像:', recording);
  };

  const handleDownload = (recording) => {
    console.log('下载录像:', recording);
  };

  return (
    <div>
      <Title level={2}>视频回放</Title>
      
      {/* 搜索条件 */}
      <Card style={{ marginBottom: '24px' }}>
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} sm={8} md={6}>
            <div>
              <div style={{ marginBottom: '8px' }}>选择地区:</div>
              <Select
                style={{ width: '100%' }}
                placeholder="请选择地区"
                value={searchParams.region}
                onChange={(value) => setSearchParams({...searchParams, region: value})}
              >
                <Option value="">全部地区</Option>
                <Option value="地区A">地区A</Option>
                <Option value="地区B">地区B</Option>
                <Option value="地区C">地区C</Option>
              </Select>
            </div>
          </Col>
          
          <Col xs={24} sm={8} md={6}>
            <div>
              <div style={{ marginBottom: '8px' }}>选择摄像头:</div>
              <Select
                style={{ width: '100%' }}
                placeholder="请选择摄像头"
                value={searchParams.camera}
                onChange={(value) => setSearchParams({...searchParams, camera: value})}
              >
                <Option value="">全部摄像头</Option>
                <Option value="Camera-A-01">Camera-A-01</Option>
                <Option value="Camera-A-02">Camera-A-02</Option>
                <Option value="Camera-B-01">Camera-B-01</Option>
              </Select>
            </div>
          </Col>
          
          <Col xs={24} sm={8} md={8}>
            <div>
              <div style={{ marginBottom: '8px' }}>时间范围:</div>
              <RangePicker
                style={{ width: '100%' }}
                showTime
                value={searchParams.dateRange}
                onChange={(dates) => setSearchParams({...searchParams, dateRange: dates})}
              />
            </div>
          </Col>
          
          <Col xs={24} sm={24} md={4}>
            <Button
              type="primary"
              icon={<SearchOutlined />}
              onClick={handleSearch}
              loading={loading}
              style={{ width: '100%', marginTop: '24px' }}
            >
              搜索
            </Button>
          </Col>
        </Row>
      </Card>

      {/* 录像列表 */}
      <Card title="录像文件">
        <List
          loading={loading}
          dataSource={recordings}
          renderItem={item => (
            <List.Item
              actions={[
                <Button
                  type="primary"
                  icon={<PlayCircleOutlined />}
                  onClick={() => handlePlay(item)}
                >
                  播放
                </Button>,
                <Button
                  icon={<DownloadOutlined />}
                  onClick={() => handleDownload(item)}
                >
                  下载
                </Button>
              ]}
            >
              <List.Item.Meta
                title={
                  <Space>
                    <span>{item.camera}</span>
                    <Tag color="blue">{item.region}</Tag>
                    <Tag color="green">{item.quality}</Tag>
                  </Space>
                }
                description={
                  <div>
                    <div>时间: {item.startTime} - {item.endTime}</div>
                    <div>时长: {item.duration} | 大小: {item.size}</div>
                  </div>
                }
              />
            </List.Item>
          )}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 个录像文件`,
          }}
        />
      </Card>
    </div>
  );
};

export default Playback;