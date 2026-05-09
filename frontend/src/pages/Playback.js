import React, { useState, useEffect } from 'react';
import { Card, DatePicker, Select, Button, Row, Col, List, Tag, Space, message, Checkbox, Modal, Progress, Popover, Badge, Tooltip, Descriptions, Divider } from 'antd';
import { PlayCircleOutlined, DownloadOutlined, SearchOutlined, VideoCameraOutlined, CheckSquareOutlined, ExclamationCircleOutlined, InfoCircleOutlined, CloseCircleOutlined, InboxOutlined } from '@ant-design/icons';
import { recordingApi, cameraApi } from '../utils/api';
import PageContainer from '../components/PageContainer';
import { Typography } from 'antd';
import dayjs from 'dayjs';
const { Title } = Typography;

const { RangePicker } = DatePicker;
const { Option } = Select;
const { confirm } = Modal;

// 下载队列项组件
const DownloadQueueItem = ({ item, onRemove, onRetry }) => {
  const statusColors = {
    pending: 'default',
    downloading: 'processing',
    completed: 'success',
    failed: 'error',
  };

  const statusText = {
    pending: '等待中',
    downloading: '下载中',
    completed: '已完成',
    failed: '失败',
  };

  return (
    <div style={{ padding: '8px 0', borderBottom: '1px solid #f0f0f0' }}>
      <Row align="middle">
        <Col flex="1">
          <div style={{ fontWeight: 500 }}>{item.fileName || `录像 #${item.recordingId}`}</div>
          <div style={{ fontSize: 12, color: '#888' }}>
            {item.size ? `${(item.size / (1024 * 1024)).toFixed(2)} MB` : '-'}
          </div>
        </Col>
        <Col>
          <Space>
            <Badge status={statusColors[item.status]} text={statusText[item.status]} />
            {item.status === 'failed' && (
              <>
                <Button size="small" onClick={() => onRetry(item)}>重试</Button>
                <Button size="small" danger icon={<CloseCircleOutlined />} onClick={() => onRemove(item)} />
              </>
            )}
            {item.status === 'completed' && (
              <Button size="small" danger icon={<CloseCircleOutlined />} onClick={() => onRemove(item)} />
            )}
          </Space>
        </Col>
      </Row>
      {item.status === 'downloading' && item.progress > 0 && (
        <Progress percent={item.progress} size="small" style={{ marginTop: 8 }} />
      )}
    </div>
  );
};

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

  // 批量选择相关状态
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [selectedRows, setSelectedRows] = useState([]);

  // 录像状态筛选
  const [statusFilter, setStatusFilter] = useState(null);

  // 下载队列状态
  const [downloadQueue, setDownloadQueue] = useState([]);
  const [downloadQueueVisible, setDownloadQueueVisible] = useState(false);

  // 录像详情弹窗状态
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [currentRecording, setCurrentRecording] = useState(null);

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
      window.open(url, '_blank');
    } catch (error) {
      console.error('获取录像播放链接失败:', error);
      message.error('获取录像播放链接失败');
    }
  };

  // 添加到下载队列
  const addToDownloadQueue = (recording) => {
    const queueItem = {
      key: Date.now(),
      recordingId: recording.id,
      fileName: `${recording.cameraName || 'recording'}_${dayjs(recording.startTime).format('YYYYMMDD_HHmmss')}.mp4`,
      size: recording.size,
      status: 'pending',
      progress: 0,
    };
    setDownloadQueue(prev => [...prev, queueItem]);
    
    // 自动开始下载
    processDownloadQueue();
  };

  // 处理下载队列
  const processDownloadQueue = async () => {
    const pendingItems = downloadQueue.filter(item => item.status === 'pending');
    if (pendingItems.length === 0) return;

    const item = pendingItems[0];
    updateQueueItem(item.key, { status: 'downloading', progress: 0 });

    try {
      const response = await recordingApi.downloadRecording(item.recordingId);

      // 创建下载链接
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', item.fileName);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

      updateQueueItem(item.key, { status: 'completed', progress: 100 });
      message.success(`${item.fileName} 下载完成`);
    } catch (error) {
      console.error('下载失败:', error);
      updateQueueItem(item.key, { status: 'failed' });
      message.error(`${item.fileName} 下载失败`);
    }

    // 处理队列中的下一个
    const remaining = downloadQueue.filter(i => i.status === 'pending' || i.status === 'downloading');
    if (remaining.length > 0) {
      setTimeout(() => processDownloadQueue(), 500);
    }
  };

  // 更新队列项
  const updateQueueItem = (key, updates) => {
    setDownloadQueue(prev => prev.map(item => 
      item.key === key ? { ...item, ...updates } : item
    ));
  };

  // 从队列移除
  const removeFromQueue = (item) => {
    setDownloadQueue(prev => prev.filter(i => i.key !== item.key));
  };

  // 重试下载
  const retryDownload = (item) => {
    updateQueueItem(item.key, { status: 'pending', progress: 0 });
    processDownloadQueue();
  };

  // 单个录像下载
  const handleDownload = async (recording) => {
    addToDownloadQueue(recording);
    setDownloadQueueVisible(true);
  };

  // 批量下载
  const handleBatchDownload = async () => {
    if (selectedRows.length === 0) {
      message.warning('请先选择要下载的录像');
      return;
    }

    confirm({
      title: '确认批量下载',
      icon: <ExclamationCircleOutlined />,
      content: `确定要下载选中的 ${selectedRows.length} 个录像文件吗？`,
      onOk: () => {
        selectedRows.forEach(recording => {
          addToDownloadQueue(recording);
        });
        setDownloadQueueVisible(true);
        setSelectedRowKeys([]);
        setSelectedRows([]);
      },
    });
  };

  // 行选择变更处理
  const onSelectChange = (newSelectedRowKeys, newSelectedRows) => {
    setSelectedRowKeys(newSelectedRowKeys);
    setSelectedRows(newSelectedRows);
  };

  const rowSelection = {
    selectedRowKeys,
    onChange: onSelectChange,
  };

  // 查看录像详情
  const handleViewDetail = (recording) => {
    setCurrentRecording(recording);
    setDetailModalVisible(true);
  };

  // 获取所有地区
  const getAllLocations = () => {
    const locations = [...new Set(cameras.map(camera => camera.location))];
    return locations.filter(location => location);
  };

  // 格式化文件大小
  const formatFileSize = (bytes) => {
    if (!bytes) return '-';
    const gb = bytes / (1024 * 1024 * 1024);
    if (gb >= 1) return `${gb.toFixed(2)} GB`;
    const mb = bytes / (1024 * 1024);
    return `${mb.toFixed(2)} MB`;
  };

  // 格式化时间
  const formatDateTime = (datetime) => {
    if (!datetime) return '-';
    return dayjs(datetime).format('YYYY-MM-DD HH:mm:ss');
  };

  // 下载队列Popover内容
  const downloadQueueContent = (
    <div style={{ width: 300, maxHeight: 400, overflowY: 'auto' }}>
      {downloadQueue.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 20, color: '#888' }}>
          暂无下载任务
        </div>
      ) : (
        downloadQueue.map(item => (
          <DownloadQueueItem
            key={item.key}
            item={item}
            onRemove={removeFromQueue}
            onRetry={retryDownload}
          />
        ))
      )}
    </div>
  );

  // 下载状态统计
  const downloadStats = {
    total: downloadQueue.length,
    completed: downloadQueue.filter(i => i.status === 'completed').length,
    failed: downloadQueue.filter(i => i.status === 'failed').length,
    active: downloadQueue.filter(i => i.status === 'downloading').length,
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
          
          <Col xs={24} sm={8} md={6}>
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
          
          <Col xs={24} sm={24} md={24}>
            <Button
              type="primary"
              icon={<SearchOutlined />}
              onClick={handleSearch}
              loading={loading}
            >
              搜索
            </Button>
          </Col>
        </Row>
      </Card>

      {/* 录像列表 */}
      <Card
        title="录像文件"
        extra={
          <Space>
            <span>已选择 {selectedRowKeys.length} 项</span>
            <Popover
              content={downloadQueueContent}
              title="下载队列"
              trigger="click"
              open={downloadQueueVisible}
              onOpenChange={setDownloadQueueVisible}
              placement="bottomRight"
            >
              <Badge count={downloadQueue.filter(i => i.status !== 'completed').length} overflowCount={99}>
                <Button icon={<InboxOutlined />}>
                  下载队列 {downloadQueue.length > 0 && `(${downloadStats.completed}/${downloadStats.total})`}
                </Button>
              </Badge>
            </Popover>
            <Button
              type="primary"
              icon={<DownloadOutlined />}
              onClick={handleBatchDownload}
              disabled={selectedRowKeys.length === 0}
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
                <Tooltip title="查看详情">
                  <Button
                    type="text"
                    icon={<InfoCircleOutlined />}
                    onClick={() => handleViewDetail(item)}
                  />
                </Tooltip>,
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
                    <Tag color="green">{item.quality || '未设置'}</Tag>
                    {item.status === 'COMPLETED' && <Tag color="success">已完成</Tag>}
                    {item.status === 'RECORDING' && <Tag color="processing">录像中</Tag>}
                    {item.status === 'PENDING' && <Tag color="default">待处理</Tag>}
                    {item.status === 'CORRUPTED' && <Tag color="error">已损坏</Tag>}
                    {item.integrityStatus === 'CORRUPTED' && (
                      <Tag color="red" icon={<ExclamationCircleOutlined />}>损坏</Tag>
                    )}
                    {item.lockStatus && <Tag color="orange">锁定中</Tag>}
                  </Space>
                }
                description={
                  <div>
                    <div>时间: {formatDateTime(item.startTime)} - {formatDateTime(item.endTime)}</div>
                    <div>
                      时长: {item.duration ? `${Math.floor(item.duration / 60)}分${item.duration % 60}秒` : '-'} |
                      大小: {formatFileSize(item.size)}
                      {item.md5 && <span> | MD5: {item.md5.substring(0, 8)}...</span>}
                      {item.recordingType && <span> | 类型: {item.recordingType}</span>}
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
      </Card>

      {/* 录像详情弹窗 */}
      <Modal
        title={
          <Space>
            <VideoCameraOutlined />
            <span>录像详情</span>
          </Space>
        }
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            关闭
          </Button>,
          <Button key="download" type="primary" icon={<DownloadOutlined />} onClick={() => {
            if (currentRecording) {
              handleDownload(currentRecording);
            }
          }}>
            下载
          </Button>,
        ]}
        width={700}
      >
        {currentRecording && (
          <Descriptions bordered column={2}>
            <Descriptions.Item label="录像ID">{currentRecording.id}</Descriptions.Item>
            <Descriptions.Item label="摄像头">{currentRecording.cameraName || currentRecording.cameraId}</Descriptions.Item>
            <Descriptions.Item label="录像类型">{currentRecording.recordingType || '-'}</Descriptions.Item>
            <Descriptions.Item label="录像格式">{currentRecording.format || '-'}</Descriptions.Item>
            <Descriptions.Item label="录像状态">
              {currentRecording.status === 'COMPLETED' && <Tag color="success">已完成</Tag>}
              {currentRecording.status === 'RECORDING' && <Tag color="processing">录像中</Tag>}
              {currentRecording.status === 'PENDING' && <Tag color="default">待处理</Tag>}
              {currentRecording.status === 'CORRUPTED' && <Tag color="error">已损坏</Tag>}
            </Descriptions.Item>
            <Descriptions.Item label="完整性状态">
              {currentRecording.integrityStatus === 'COMPLETED' && <Tag color="success">正常</Tag>}
              {currentRecording.integrityStatus === 'CORRUPTED' && <Tag color="error">已损坏</Tag>}
              {currentRecording.integrityStatus === 'PENDING' && <Tag color="default">待验证</Tag>}
            </Descriptions.Item>
            <Descriptions.Item label="开始时间">{formatDateTime(currentRecording.startTime)}</Descriptions.Item>
            <Descriptions.Item label="结束时间">{formatDateTime(currentRecording.endTime)}</Descriptions.Item>
            <Descriptions.Item label="时长">{currentRecording.duration ? `${currentRecording.duration}秒` : '-'}</Descriptions.Item>
            <Descriptions.Item label="文件大小">{formatFileSize(currentRecording.size)}</Descriptions.Item>
            <Descriptions.Item label="分辨率">{currentRecording.quality || '-'}</Descriptions.Item>
            <Descriptions.Item label="锁定状态">
              {currentRecording.lockStatus ? <Tag color="orange">已锁定</Tag> : <Tag color="green">未锁定</Tag>}
            </Descriptions.Item>
            <Descriptions.Item label="MD5校验码" span={2}>
              {currentRecording.md5 || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="存储路径" span={2}>
              {currentRecording.storagePath || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">{formatDateTime(currentRecording.createdAt)}</Descriptions.Item>
            <Descriptions.Item label="更新时间">{formatDateTime(currentRecording.updatedAt)}</Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  );
};

export default Playback;