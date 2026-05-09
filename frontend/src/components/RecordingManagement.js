import React, { useState, useEffect, useCallback } from 'react';
import { Table, Button, Space, Modal, Form, Select, Tag, Popconfirm, message, Card, Row, Col, DatePicker, Statistic, Badge, Tooltip } from 'antd';
import { 
  VideoCameraOutlined, DeleteOutlined, ReloadOutlined, UndoOutlined, 
  ClockCircleOutlined, ExclamationCircleOutlined, WarningOutlined,
  FileSearchOutlined, HistoryOutlined, ClearOutlined
} from '@ant-design/icons';
import { enhancedRecordingApi, recordingScheduleApi } from '../utils/api';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;

/**
 * 录像管理增强组件
 * 提供已删除录像、孤立录像的查询、恢复和清理功能
 */
const RecordingManagement = ({ cameraId, cameraName, visible, onClose }) => {
  const [activeTab, setActiveTab] = useState('orphaned'); // orphaned | deleted
  const [recordings, setRecordings] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [stats, setStats] = useState({ orphanedCount: 0, deletedCount: 0 });
  const [dateRange, setDateRange] = useState(null);

  // 加载统计数据
  const loadStats = useCallback(async () => {
    try {
      const response = await enhancedRecordingApi.getOrphanedRecordingsCount();
      setStats({
        orphanedCount: response.data?.orphanedCount || 0,
        deletedCount: response.data?.deletedCount || 0,
      });
    } catch (error) {
      console.error('加载统计数据失败:', error);
    }
  }, []);

  // 加载录像列表
  const loadRecordings = useCallback(async () => {
    setLoading(true);
    try {
      const params = {
        page: pagination.current - 1,
        size: pagination.pageSize,
      };
      
      if (dateRange && dateRange.length === 2) {
        params.startTime = dateRange[0].format('YYYY-MM-DDTHH:mm:ss');
        params.endTime = dateRange[1].format('YYYY-MM-DDTHH:mm:ss');
      }

      let response;
      if (activeTab === 'orphaned') {
        response = await enhancedRecordingApi.getOrphanedRecordings(params);
      } else {
        response = await enhancedRecordingApi.getDeletedRecordings(params);
      }

      const data = response.data;
      const content = data?.content || data || [];
      const total = data?.totalElements || content.length;

      setRecordings(Array.isArray(content) ? content : []);
      setPagination(prev => ({ ...prev, total, current: 1 }));
    } catch (error) {
      console.error('加载录像列表失败:', error);
      message.error('加载录像列表失败');
      setRecordings([]);
    } finally {
      setLoading(false);
    }
  }, [activeTab, pagination.current, pagination.pageSize, dateRange]);

  useEffect(() => {
    if (visible) {
      loadStats();
      loadRecordings();
    }
  }, [visible, loadStats, loadRecordings]);

  // 恢复录像
  const handleRestore = async (id) => {
    try {
      await enhancedRecordingApi.restoreRecording(id);
      message.success('录像恢复成功');
      loadRecordings();
      loadStats();
    } catch (error) {
      console.error('恢复录像失败:', error);
      message.error('恢复录像失败');
    }
  };

  // 清理孤立录像
  const handleCleanup = async (daysOld = 30) => {
    try {
      const response = await enhancedRecordingApi.cleanupOrphanedRecordings(daysOld);
      message.success(`成功清理 ${response.data?.cleanedCount || 0} 条孤立录像`);
      loadRecordings();
      loadStats();
    } catch (error) {
      console.error('清理孤立录像失败:', error);
      message.error('清理孤立录像失败');
    }
  };

  // 格式化时间
  const formatDateTime = (datetime) => {
    if (!datetime) return '-';
    return dayjs(datetime).format('YYYY-MM-DD HH:mm:ss');
  };

  // 格式化文件大小
  const formatFileSize = (bytes) => {
    if (!bytes) return '-';
    const gb = bytes / (1024 * 1024 * 1024);
    if (gb >= 1) return `${gb.toFixed(2)} GB`;
    const mb = bytes / (1024 * 1024);
    return `${mb.toFixed(2)} MB`;
  };

  const columns = [
    {
      title: '录像ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '摄像头',
      dataIndex: 'cameraName',
      key: 'cameraName',
      width: 150,
      render: (text, record) => text || record.cameraId || '-',
    },
    {
      title: '开始时间',
      dataIndex: 'startTime',
      key: 'startTime',
      width: 180,
      render: formatDateTime,
    },
    {
      title: '结束时间',
      dataIndex: 'endTime',
      key: 'endTime',
      width: 180,
      render: formatDateTime,
    },
    {
      title: '文件大小',
      dataIndex: 'fileSize',
      key: 'fileSize',
      width: 120,
      render: formatFileSize,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status, record) => {
        if (activeTab === 'orphaned') {
          return <Tag icon={<WarningOutlined />} color="orange">孤立</Tag>;
        }
        return <Tag icon={<DeleteOutlined />} color="red">已删除</Tag>;
      },
    },
    {
      title: '孤立时间',
      dataIndex: 'orphanedAt',
      key: 'orphanedAt',
      width: 180,
      render: formatDateTime,
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      render: (_, record) => (
        <Space>
          {activeTab !== 'deleted' && (
            <Tooltip title="恢复录像到正常状态">
              <Button 
                type="link" 
                size="small" 
                icon={<UndoOutlined />}
                onClick={() => handleRestore(record.id)}
              >
                恢复
              </Button>
            </Tooltip>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Modal
      title={
        <Space>
          <VideoCameraOutlined />
          <span>录像管理 - {cameraName}</span>
        </Space>
      }
      open={visible}
      onCancel={onClose}
      footer={null}
      width={1000}
      destroyOnClose
    >
      {/* 统计卡片 */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <Card size="small">
            <Statistic
              title="孤立录像"
              value={stats.orphanedCount}
              prefix={<WarningOutlined />}
              valueStyle={{ color: '#fa8c16' }}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card size="small">
            <Statistic
              title="已删除录像"
              value={stats.deletedCount}
              prefix={<DeleteOutlined />}
              valueStyle={{ color: '#cf1322' }}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card size="small">
            <Space>
              <Button 
                size="small" 
                icon={<ClearOutlined />}
                onClick={() => handleCleanup(7)}
              >
                清理7天前
              </Button>
              <Button 
                size="small" 
                danger
                icon={<ClearOutlined />}
                onClick={() => handleCleanup(30)}
              >
                清理30天前
              </Button>
            </Space>
          </Card>
        </Col>
      </Row>

      {/* Tab切换 */}
      <div style={{ marginBottom: 16 }}>
        <Space>
          <Button 
            type={activeTab === 'orphaned' ? 'primary' : 'default'}
            onClick={() => setActiveTab('orphaned')}
          >
            <Badge count={stats.orphanedCount} style={{ backgroundColor: '#fa8c16' }}>
              孤立录像
            </Badge>
          </Button>
          <Button 
            type={activeTab === 'deleted' ? 'primary' : 'default'}
            onClick={() => setActiveTab('deleted')}
          >
            <Badge count={stats.deletedCount} style={{ backgroundColor: '#cf1322' }}>
              已删除录像
            </Badge>
          </Button>
        </Space>
        <Space style={{ float: 'right' }}>
          <RangePicker 
            showTime
            value={dateRange}
            onChange={setDateRange}
            placeholder={['开始时间', '结束时间']}
          />
          <Button icon={<ReloadOutlined />} onClick={loadRecordings}>
            刷新
          </Button>
        </Space>
      </div>

      {/* 录像列表 */}
      <Table
        columns={columns}
        dataSource={recordings}
        rowKey="id"
        loading={loading}
        pagination={{
          ...pagination,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条`,
          onChange: (page, pageSize) => setPagination(prev => ({ ...prev, current: page, pageSize })),
        }}
        scroll={{ x: 900 }}
      />
    </Modal>
  );
};

export default RecordingManagement;
