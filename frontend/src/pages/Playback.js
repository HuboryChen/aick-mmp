import React, { useState, useEffect } from 'react';
import { Card, Typography, DatePicker, Select, Button, Row, Col, List, Tag, Space, message } from 'antd';
import { PlayCircleOutlined, DownloadOutlined, SearchOutlined } from '@ant-design/icons';
import { recordingApi, cameraApi } from '../utils/api';

const { Title } = Typography;
const { RangePicker } = DatePicker;
const { Option } = Select;

const Playback = () => {
  const [searchParams, setSearchParams] = useState({
    cameraId: null,
    dateRange: null,
    location: null
  });
  const [recordings, setRecordings] = useState([]);
  const [loading, setLoading] = useState(false);
  const [cameras, setCameras] = useState([]);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  useEffect(() => {
    fetchCameras();
  }, []);

  const fetchCameras = async () => {
    try {
      const response = await cameraApi.getCameras({ size: 1000 });
      setCameras(response.data.content);
    } catch (error) {
      console.error('获取摄像头列表失败:', error);
      message.error('获取摄像头列表失败');
    }
  };

  const fetchRecordings = async (params = {}) => {
    setLoading(true);
    try {
      const response = await recordingApi.getRecordings({
        page: pagination.current - 1,
        size: pagination.pageSize,
        ...params
      });
      
      setRecordings(response.data.content);
      setPagination({
        ...pagination,
        total: response.data.totalElements,
        current: response.data.number + 1,
      });
    } catch (error) {
      console.error('获取录像列表失败:', error);
      message.error('获取录像列表失败');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    const params = {};
    
    if (searchParams.cameraId) {
      params.cameraId = searchParams.cameraId;
    }
    
    if (searchParams.location) {
      params.location = searchParams.location;
    }
    
    if (searchParams.dateRange && searchParams.dateRange.length === 2) {
      params.startTime = searchParams.dateRange[0].toISOString();
      params.endTime = searchParams.dateRange[1].toISOString();
    }
    
    fetchRecordings(params);
  };

  const handleTableChange = (pager) => {
    setPagination(pager);
    fetchRecordings();
  };

  const handlePlay = async (recording) => {
    try {
      const response = await recordingApi.getRecordingUrl(recording.id);
      const url = response.data.url;
      // 在新窗口中打开录像播放链接
      window.open(url, '_blank');
    } catch (error) {
      console.error('获取录像播放链接失败:', error);
      message.error('获取录像播放链接失败');
    }
  };

  const handleDownload = (recording) => {
    // 下载录像功能
    console.log('下载录像:', recording);
    message.info('下载功能将在后续版本中实现');
  };

  // 获取所有地区
  const getAllLocations = () => {
    const locations = [...new Set(cameras.map(camera => camera.location))];
    return locations.filter(location => location); // 过滤掉空值
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
                value={searchParams.location}
                onChange={(value) => setSearchParams({...searchParams, location: value})}
              >
                <Option value="">全部地区</Option>
                {getAllLocations().map(location => (
                  <Option key={location} value={location}>{location}</Option>
                ))}
              </Select>
            </div>
          </Col>
          
          <Col xs={24} sm={8} md={6}>
            <div>
              <div style={{ marginBottom: '8px' }}>选择摄像头:</div>
              <Select
                style={{ width: '100%' }}
                placeholder="请选择摄像头"
                value={searchParams.cameraId}
                onChange={(value) => setSearchParams({...searchParams, cameraId: value})}
                showSearch
                optionFilterProp="children"
              >
                <Option value="">全部摄像头</Option>
                {cameras.map(camera => (
                  <Option key={camera.id} value={camera.id}>{camera.name}</Option>
                ))}
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
                    <span>{item.cameraName}</span>
                    <Tag color="blue">{item.location}</Tag>
                    <Tag color="green">{item.quality}</Tag>
                  </Space>
                }
                description={
                  <div>
                    <div>时间: {new Date(item.startTime).toLocaleString()} - {new Date(item.endTime).toLocaleString()}</div>
                    <div>时长: {Math.floor(item.duration / 60)}分钟 {item.duration % 60}秒 | 大小: {(item.size / (1024 * 1024)).toFixed(2)} MB</div>
                  </div>
                }
              />
            </List.Item>
          )}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 个录像文件`,
            onChange: (page, pageSize) => {
              handleTableChange({ current: page, pageSize });
            }
          }}
        />
      </Card>
    </div>
  );
};

export default Playback;