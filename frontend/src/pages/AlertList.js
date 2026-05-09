import React, { useState, useEffect, useCallback } from 'react';
import { 
  Table, Button, Space, Modal, Form, Input, Select, DatePicker, 
  Tag, message, Card, Row, Col, Statistic, Popconfirm, Tooltip, Badge
} from 'antd';
import { 
  ReloadOutlined, CheckOutlined, EyeOutlined, 
  ExclamationCircleOutlined, BellOutlined, FilterOutlined, DownloadOutlined
} from '@ant-design/icons';
import { alertRecordApi, alertRuleApi } from '../utils/api';
import moment from 'moment';

const { Option } = Select;
const { RangePicker } = DatePicker;

/**
 * 告警记录列表页面
 */
const AlertList = () => {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(false);
  const [statistics, setStatistics] = useState(null);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0
  });
  const [filters, setFilters] = useState({
    level: null,
    status: null,
    timeRange: null
  });
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [selectedRecord, setSelectedRecord] = useState(null);
  const [resolveForm] = Form.useForm();

  // 告警级别映射
  const levelMap = {
    'INFO': { label: '信息', color: 'blue' },
    'WARNING': { label: '警告', color: 'orange' },
    'ERROR': { label: '错误', color: 'red' },
    'CRITICAL': { label: '严重', color: 'purple' }
  };

  // 告警状态映射
  const statusMap = {
    'UNRESOLVED': { label: '未处理', color: 'red' },
    'ACKNOWLEDGED': { label: '已确认', color: 'orange' },
    'RESOLVED': { label: '已解决', color: 'green' },
    'IGNORED': { label: '已忽略', color: 'default' }
  };

  // 加载告警记录
  const loadRecords = useCallback(async () => {
    setLoading(true);
    try {
      const params = {
        page: pagination.current - 1,
        size: pagination.pageSize
      };

      let response;
      if (filters.level) {
        response = await alertRecordApi.getByLevel(filters.level, params);
      } else if (filters.status) {
        response = await alertRecordApi.list({ ...params, status: filters.status });
      } else {
        response = await alertRecordApi.list(params);
      }

      const data = response.data;
      setRecords(data.content || []);
      setPagination({
        ...pagination,
        total: data.totalElements || 0
      });
    } catch (error) {
      message.error('加载告警记录失败: ' + (error.message || '未知错误'));
    } finally {
      setLoading(false);
    }
  }, [pagination.current, pagination.pageSize, filters]);

  // 加载统计数据
  const loadStatistics = useCallback(async () => {
    try {
      const response = await alertRecordApi.getStatistics();
      setStatistics(response.data);
    } catch (error) {
      console.error('加载统计数据失败:', error);
    }
  }, []);

  useEffect(() => {
    loadRecords();
    loadStatistics();
  }, [loadRecords, loadStatistics]);

  // 表格分页变化
  const handleTableChange = (newPagination) => {
    setPagination(newPagination);
  };

  // 筛选变化
  const handleFilterChange = (key, value) => {
    setFilters({ ...filters, [key]: value });
    setPagination({ ...pagination, current: 1 });
  };

  // 查看详情
  const handleViewDetail = (record) => {
    setSelectedRecord(record);
    setDetailModalVisible(true);
  };

  // 关闭详情弹窗
  const handleCloseDetail = () => {
    setDetailModalVisible(false);
    setSelectedRecord(null);
  };

  // 确认告警
  const handleAcknowledge = async (id) => {
    try {
      await alertRecordApi.acknowledge(id);
      message.success('告警已确认');
      loadRecords();
      loadStatistics();
    } catch (error) {
      message.error('操作失败: ' + (error.message || '未知错误'));
    }
  };

  // 解决告警
  const handleResolve = async (id) => {
    try {
      await alertRecordApi.resolve(id, { resolutionNote: '' });
      message.success('告警已解决');
      loadRecords();
      loadStatistics();
    } catch (error) {
      message.error('操作失败: ' + (error.message || '未知错误'));
    }
  };

  // 批量处理
  const handleBatchResolve = async (selectedRowKeys) => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要处理的告警');
      return;
    }

    try {
      await alertRecordApi.batchResolve(selectedRowKeys, { resolutionNote: '批量处理' });
      message.success(`已处理 ${selectedRowKeys.length} 条告警`);
      loadRecords();
      loadStatistics();
    } catch (error) {
      message.error('批量处理失败: ' + (error.message || '未知错误'));
    }
  };

  // 刷新数据
  const handleRefresh = () => {
    loadRecords();
    loadStatistics();
  };

  // 表格列定义
  const columns = [
    {
      title: '级别',
      dataIndex: 'level',
      key: 'level',
      width: 100,
      render: (level) => {
        const config = levelMap[level] || { label: level, color: 'default' };
        return <Badge status={config.color} text={config.label} />;
      },
      filters: [
        { text: '信息', value: 'INFO' },
        { text: '警告', value: 'WARNING' },
        { text: '错误', value: 'ERROR' },
        { text: '严重', value: 'CRITICAL' }
      ],
      onFilter: (value, record) => record.level === value
    },
    {
      title: '告警标题',
      dataIndex: 'title',
      key: 'title',
      ellipsis: true,
      render: (text, record) => (
        <Space>
          <span style={{ fontWeight: record.status === 'UNRESOLVED' ? 'bold' : 'normal' }}>
            {text}
          </span>
          {record.status === 'UNRESOLVED' && (
            <Badge status="error" />
          )}
        </Space>
      )
    },
    {
      title: '告警规则',
      dataIndex: 'ruleName',
      key: 'ruleName',
      ellipsis: true
    },
    {
      title: '告警类型',
      dataIndex: 'alertType',
      key: 'alertType',
      width: 120,
      render: (type) => {
        const typeMap = {
          'CPU_USAGE': 'CPU使用率',
          'MEMORY_USAGE': '内存使用率',
          'DISK_USAGE': '磁盘使用率',
          'CAMERA_OFFLINE': '摄像头离线',
          'EDGE_NODE_OFFLINE': '边缘节点离线',
          'STREAM_INTERRUPTED': '流中断',
          'MOTION_DETECTED': '移动侦测',
          'CUSTOM': '自定义'
        };
        return typeMap[type] || type;
      }
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => {
        const config = statusMap[status] || { label: status, color: 'default' };
        return <Tag color={config.color}>{config.label}</Tag>;
      }
    },
    {
      title: '发生时间',
      dataIndex: 'alertTime',
      key: 'alertTime',
      width: 180,
      render: (time) => time ? moment(time).format('YYYY-MM-DD HH:mm:ss') : '-'
    },
    {
      title: '目标',
      dataIndex: 'targetName',
      key: 'targetName',
      ellipsis: true,
      render: (text, record) => text || (record.cameraName ? `摄像头: ${record.cameraName}` : '-')
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="查看详情">
            <Button 
              type="text" 
              size="small"
              icon={<EyeOutlined />} 
              onClick={() => handleViewDetail(record)}
            />
          </Tooltip>
          {record.status === 'UNRESOLVED' && (
            <>
              <Tooltip title="确认">
                <Button 
                  type="text" 
                  size="small"
                  icon={<CheckOutlined />} 
                  onClick={() => handleAcknowledge(record.id)}
                />
              </Tooltip>
              <Tooltip title="解决">
                <Button 
                  type="text" 
                  size="small"
                  icon={<CheckOutlined />} 
                  onClick={() => handleResolve(record.id)}
                />
              </Tooltip>
            </>
          )}
        </Space>
      )
    }
  ];

  const rowSelection = {
    onChange: (selectedRowKeys) => {
      // 可以保存选中项用于批量操作
    }
  };

  return (
    <div className="alert-list">
      {/* 统计卡片 */}
      <Row gutter={[16, 16]} className="mb-4">
        <Col span={6}>
          <Card>
            <Statistic 
              title="今日告警" 
              value={statistics?.todayCount || 0}
              prefix={<BellOutlined />}
              valueStyle={{ color: '#cf1322' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic 
              title="未处理" 
              value={statistics?.unresolvedCount || 0}
              prefix={<ExclamationCircleOutlined />}
              valueStyle={{ color: statistics?.unresolvedCount > 0 ? '#cf1322' : '#3f8600' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic 
              title="已确认" 
              value={statistics?.acknowledgedCount || 0}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic 
              title="本周告警" 
              value={statistics?.totalCount || 0}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 告警记录表格 */}
      <Card
        title={
          <Space>
            <ExclamationCircleOutlined />
            <span>告警记录</span>
          </Space>
        }
        extra={
          <Space>
            <Select
              placeholder="筛选级别"
              allowClear
              style={{ width: 120 }}
              onChange={(value) => handleFilterChange('level', value)}
            >
              <Option value="INFO">信息</Option>
              <Option value="WARNING">警告</Option>
              <Option value="ERROR">错误</Option>
              <Option value="CRITICAL">严重</Option>
            </Select>
            <Select
              placeholder="筛选状态"
              allowClear
              style={{ width: 120 }}
              onChange={(value) => handleFilterChange('status', value)}
            >
              <Option value="UNRESOLVED">未处理</Option>
              <Option value="ACKNOWLEDGED">已确认</Option>
              <Option value="RESOLVED">已解决</Option>
            </Select>
            <Button icon={<ReloadOutlined />} onClick={handleRefresh}>
              刷新
            </Button>
          </Space>
        }
      >
        <Table
          columns={columns}
          dataSource={records}
          rowKey="id"
          loading={loading}
          pagination={pagination}
          onChange={handleTableChange}
          rowSelection={rowSelection}
          size="middle"
        />
      </Card>

      {/* 详情弹窗 */}
      <Modal
        title="告警详情"
        open={detailModalVisible}
        onCancel={handleCloseDetail}
        footer={[
          <Button key="close" onClick={handleCloseDetail}>
            关闭
          </Button>
        ]}
        width={600}
      >
        {selectedRecord && (
          <div>
            <Row gutter={[16, 16]}>
              <Col span={12}>
                <b>告警标题：</b>{selectedRecord.title}
              </Col>
              <Col span={12}>
                <b>告警级别：</b>
                <Tag color={levelMap[selectedRecord.level]?.color}>
                  {levelMap[selectedRecord.level]?.label}
                </Tag>
              </Col>
              <Col span={12}>
                <b>告警状态：</b>
                <Tag color={statusMap[selectedRecord.status]?.color}>
                  {statusMap[selectedRecord.status]?.label}
                </Tag>
              </Col>
              <Col span={12}>
                <b>告警类型：</b>{selectedRecord.alertType}
              </Col>
              <Col span={12}>
                <b>告警规则：</b>{selectedRecord.ruleName}
              </Col>
              <Col span={12}>
                <b>发生时间：</b>
                {selectedRecord.alertTime ? moment(selectedRecord.alertTime).format('YYYY-MM-DD HH:mm:ss') : '-'}
              </Col>
              {selectedRecord.targetName && (
                <Col span={12}>
                  <b>监控目标：</b>{selectedRecord.targetName}
                </Col>
              )}
              {selectedRecord.actualValue && (
                <Col span={12}>
                  <b>实际值：</b>{selectedRecord.actualValue}
                </Col>
              )}
              {selectedRecord.thresholdValue && (
                <Col span={12}>
                  <b>阈值：</b>{selectedRecord.thresholdValue}
                </Col>
              )}
            </Row>
            
            {selectedRecord.message && (
              <div className="mt-4">
                <b>告警详情：</b>
                <p className="mt-2 p-2" style={{ background: '#f5f5f5', borderRadius: 4 }}>
                  {selectedRecord.message}
                </p>
              </div>
            )}

            {(selectedRecord.resolvedByUsername || selectedRecord.resolutionNote) && (
              <div className="mt-4">
                <b>处理信息：</b>
                <p className="mt-2">
                  {selectedRecord.resolvedByUsername && (
                    <span>处理人：{selectedRecord.resolvedByUsername} </span>
                  )}
                  {selectedRecord.resolvedAt && (
                    <span>处理时间：{moment(selectedRecord.resolvedAt).format('YYYY-MM-DD HH:mm:ss')}</span>
                  )}
                </p>
                {selectedRecord.resolutionNote && (
                  <p className="mt-1 p-2" style={{ background: '#f5f5f5', borderRadius: 4 }}>
                    {selectedRecord.resolutionNote}
                  </p>
                )}
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
};

export default AlertList;
