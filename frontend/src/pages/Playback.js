import React, { useState, useEffect } from 'react';
import { Card, DatePicker, Select, Button, Row, Col, List, Tag, Space, message, Checkbox, Modal, Progress } from 'antd';
import { PlayCircleOutlined, DownloadOutlined, SearchOutlined, VideoCameraOutlined, CheckSquareOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { recordingApi, cameraApi } from '../utils/api';
import PageContainer from '../components/PageContainer';
import { Typography } from 'antd';
const { Title } = Typography;

const { RangePicker } = DatePicker;
const { Option } = Select;
const { confirm } = Modal;

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

  // Task 5 & 6: 新增批量选择相关状态
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [selectedRows, setSelectedRows] = useState([]);
  const [downloading, setDownloading] = useState(false);
  const [downloadProgress, setDownloadProgress] = useState(0);

  // Task 6: 新增录像状态筛选
  const [statusFilter, setStatusFilter] = useState(null);

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

  // Task 6: 更新 handleSearch 方法以包含状态筛选
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

    // Task 6: 新增状态筛选
    if (statusFilter) {
      params.status = statusFilter;
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

  // Task 5: 实现单个录像下载
  const handleDownload = async (recording) => {
    setDownloading(true);
    setDownloadProgress(0);

    try {
      const response = await recordingApi.downloadRecording(recording.id);

      // 创建下载链接
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      const contentDisposition = response.headers['content-disposition'];
      let filename = `recording_${recording.id}.mp4`;
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
        if (filenameMatch && filenameMatch[1]) {
          filename = filenameMatch[1].replace(/['"]/g, '');
        }
      }
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

      setDownloadProgress(100);
      message.success('下载开始');
    } catch (error) {
      console.error('下载失败:', error);
      message.error('下载失败: ' + (error.message || '未知错误'));
    } finally {
      setDownloading(false);
    }
  };

  // Task 5: 实现批量下载
  const handleBatchDownload = async () => {
    if (selectedRows.length === 0) {
      message.warning('请先选择要下载的录像');
      return;
    }

    confirm({
      title: '确认批量下载',
      icon: <ExclamationCircleOutlined />,
      content: `确定要下载选中的 ${selectedRows.length} 个录像文件吗？`,
      onOk: async () => {
        setDownloading(true);
        setDownloadProgress(0);

        try {
          // 如果只选了一个，直接下载单个文件
          if (selectedRows.length === 1) {
            await handleDownload(selectedRows[0]);
            setSelectedRowKeys([]);
            setSelectedRows([]);
            return;
          }

          // 多个文件依次下载（因为后端不支持真正的批量打包下载）
          for (let i = 0; i < selectedRows.length; i++) {
            setDownloadProgress(Math.round((i / selectedRows.length) * 100));
            await handleDownload(selectedRows[i]);
            // 添加小延迟以避免请求过于密集
            await new Promise(resolve => setTimeout(resolve, 500));
          }

          setDownloadProgress(100);
          message.success('批量下载开始');
        } catch (error) {
          console.error('批量下载失败:', error);
          message.error('批量下载失败');
        } finally {
          setDownloading(false);
          setSelectedRowKeys([]);
          setSelectedRows([]);
        }
      },
    });
  };

  // Task 5: 行选择变更处理
  const onSelectChange = (newSelectedRowKeys, newSelectedRows) => {
    setSelectedRowKeys(newSelectedRowKeys);
    setSelectedRows(newSelectedRows);
  };

  const rowSelection = {
    selectedRowKeys,
    onChange: onSelectChange,
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

          {/* Task 6: 新增录像状态筛选 */}
          <Col xs={24} sm={8} md={6}>
            <div>
              <div style={{ marginBottom: '8px' }}>录像状态:</div>
              <Select
                style={{ width: '100%' }}
                placeholder="全部状态"
                value={statusFilter}
                onChange={(value) => setStatusFilter(value)}
                allowClear
              >
                <Option value="COMPLETED">已完成</Option>
                <Option value="RECORDING">录像中</Option>
                <Option value="PENDING">待处理</Option>
                <Option value="CORRUPTED">已损坏</Option>
              </Select>
            </div>
          </Col>
        </Row>
      </Card>

      {/* 录像列表 */}
      <Card
        title="录像文件"
        extra={
          <Space>
            <span>已选择 {selectedRowKeys.length} 项</span>
            <Button
              type="primary"
              icon={<DownloadOutlined />}
              onClick={handleBatchDownload}
              disabled={selectedRowKeys.length === 0}
              loading={downloading}
            >
              批量下载
            </Button>
          </Space>
        }
      >
        <List
          loading={loading}
          dataSource={recordings}
          rowSelection={rowSelection}
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
                  loading={downloading && selectedRows.some(r => r.id === item.id)}
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

                    {/* Task 6: 新增录像状态标签 */}
                    {item.status === 'COMPLETED' && <Tag color="success">已完成</Tag>}
                    {item.status === 'RECORDING' && <Tag color="processing">录像中</Tag>}
                    {item.status === 'PENDING' && <Tag color="default">待处理</Tag>}
                    {item.status === 'CORRUPTED' && <Tag color="error">已损坏</Tag>}

                    {/* Task 6: 新增完整性状态指示 */}
                    {item.integrityStatus === 'CORRUPTED' && (
                      <Tag color="red" icon={<ExclamationCircleOutlined />}>损坏</Tag>
                    )}
                  </Space>
                }
                description={
                  <div>
                    <div>时间: {new Date(item.startTime).toLocaleString()} - {new Date(item.endTime).toLocaleString()}</div>
                    <div>
                      时长: {Math.floor(item.duration / 60)}分钟 {item.duration % 60}秒 |
                      大小: {(item.size / (1024 * 1024)).toFixed(2)} MB
                      {/* Task 6: 新增文件MD5显示 */}
                      {item.md5 && <span> | MD5: {item.md5.substring(0, 8)}...</span>}
                    </div>
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

        {/* Task 5: 下载进度显示 */}
        {downloading && (
          <div style={{ marginTop: 16 }}>
            <Progress percent={downloadProgress} status="active" />
          </div>
        )}
      </Card>
    </div>
  );
};

export default Playback;