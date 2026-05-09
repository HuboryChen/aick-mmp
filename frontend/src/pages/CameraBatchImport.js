import React, { useState, useEffect, useRef } from 'react';
import { Card, Table, Button, Upload, Progress, Tag, message, Space, Row, Col, Statistic, Alert, Spin, Modal, Descriptions } from 'antd';
import { InboxOutlined, DownloadOutlined, StopOutlined, ReloadOutlined, FileExcelOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';
import { cameraBatchImportApi } from '../utils/api';

const { Dragger } = Upload;

const CameraBatchImport = () => {
  const [importing, setImporting] = useState(false);
  const [currentTaskId, setCurrentTaskId] = useState(null);
  const [progress, setProgress] = useState({ progress: 0, totalRecords: 0, successCount: 0, failCount: 0, status: '' });
  const [importHistory, setImportHistory] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyPagination, setHistoryPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [completedResult, setCompletedResult] = useState(null);
  const pollingRef = useRef(null);

  useEffect(() => {
    fetchHistory();
    return () => {
      if (pollingRef.current) clearInterval(pollingRef.current);
    };
  }, [historyPagination.current, historyPagination.pageSize]);

  const downloadTemplate = async () => {
    try {
      const response = await cameraBatchImportApi.downloadTemplate();
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'camera-import-template.xlsx');
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      message.success('模板下载成功');
    } catch (err) {
      message.error('模板下载失败');
    }
  };

  const handleImport = async (file) => {
    const ext = file.name.split('.').pop().toLowerCase();
    if (ext !== 'xlsx' && ext !== 'csv') {
      message.error('仅支持 .xlsx 和 .csv 文件');
      return false;
    }

    if (file.size > 10 * 1024 * 1024) {
      message.error('文件大小不能超过 10MB');
      return false;
    }

    setImporting(true);
    setCompletedResult(null);
    setProgress({ progress: 0, totalRecords: 0, successCount: 0, failCount: 0, status: 'VALIDATING' });

    try {
      const response = await cameraBatchImportApi.startImport(file);
      const taskId = response.data?.taskId;
      if (taskId) {
        setCurrentTaskId(taskId);
        startPolling(taskId);
      }
    } catch (err) {
      message.error('导入启动失败');
      setImporting(false);
    }

    return false;
  };

  const startPolling = (taskId) => {
    if (pollingRef.current) clearInterval(pollingRef.current);
    pollingRef.current = setInterval(async () => {
      try {
        const response = await cameraBatchImportApi.getImportProgress(taskId);
        const data = response.data;
        setProgress({
          progress: data.progress || 0,
          totalRecords: data.totalRecords || 0,
          successCount: data.successCount || 0,
          failCount: data.failCount || 0,
          status: data.status || '',
        });

        if (data.status === 'COMPLETED' || data.status === 'FAILED' || data.status === 'CANCELLED') {
          clearInterval(pollingRef.current);
          setImporting(false);
          setCompletedResult({
            status: data.status,
            totalRecords: data.totalRecords,
            successCount: data.successCount,
            failCount: data.failCount,
            taskId: taskId,
          });
          fetchHistory();
        }
      } catch (err) {
        console.warn('Polling failed:', err);
      }
    }, 1500);
  };

  const cancelImport = async () => {
    if (!currentTaskId) return;
    try {
      await cameraBatchImportApi.cancelImport(currentTaskId);
      message.success('导入已取消');
      setImporting(false);
      if (pollingRef.current) clearInterval(pollingRef.current);
      fetchHistory();
    } catch (err) {
      message.error('取消失败');
    }
  };

  const downloadErrorReport = async (taskId) => {
    try {
      const response = await cameraBatchImportApi.downloadErrorReport(taskId);
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `import-error-report-${taskId}.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      message.error('错误报告下载失败');
    }
  };

  const fetchHistory = async () => {
    setHistoryLoading(true);
    try {
      const params = {
        page: historyPagination.current - 1,
        size: historyPagination.pageSize,
      };
      const response = await cameraBatchImportApi.getImportHistory(params);
      setImportHistory(response.data?.content || []);
      setHistoryPagination(prev => ({ ...prev, total: response.data?.totalElements || 0 }));
    } catch (err) {
      console.error('Failed to fetch import history:', err);
    } finally {
      setHistoryLoading(false);
    }
  };

  const statusTag = (status) => {
    const colors = {
      PENDING: 'default', VALIDATING: 'processing', IMPORTING: 'processing',
      COMPLETED: 'success', FAILED: 'error', CANCELLED: 'warning',
    };
    const labels = {
      PENDING: '等待中', VALIDATING: '验证中', IMPORTING: '导入中',
      COMPLETED: '已完成', FAILED: '失败', CANCELLED: '已取消',
    };
    return <Tag color={colors[status] || 'default'}>{labels[status] || status}</Tag>;
  };

  const historyColumns = [
    { title: '文件名', dataIndex: 'fileName', key: 'fileName', ellipsis: true },
    { title: '状态', dataIndex: 'status', key: 'status', width: 90, render: (s) => statusTag(s) },
    { title: '总记录', dataIndex: 'totalRecords', key: 'totalRecords', width: 80 },
    { title: '成功', dataIndex: 'successCount', key: 'successCount', width: 70, render: (v) => <span style={{ color: '#52c41a' }}>{v || 0}</span> },
    { title: '失败', dataIndex: 'failCount', key: 'failCount', width: 70, render: (v) => <span style={{ color: v > 0 ? '#ff4d4f' : undefined }}>{v || 0}</span> },
    {
      title: '错误报告', key: 'errors', width: 90,
      render: (_, record) => record.failCount > 0 ? (
        <Button type="link" size="small" onClick={() => downloadErrorReport(record.id)}>下载</Button>
      ) : '-',
    },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 160, render: (t) => t ? new Date(t).toLocaleString() : '-' },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={12}>
          <Card title="导入模板" variant="outlined" style={{ height: '100%' }}>
            <p style={{ color: '#888', marginBottom: 16 }}>
              下载Excel导入模板，按照模板格式填写摄像头信息后上传导入。
            </p>
            <Button type="primary" icon={<DownloadOutlined />} onClick={downloadTemplate}>
              下载导入模板
            </Button>
          </Card>
        </Col>
        <Col span={12}>
          <Card title="上传文件" variant="outlined" style={{ height: '100%' }}>
            <Dragger
              name="file"
              multiple={false}
              accept=".xlsx,.csv"
              showUploadList={false}
              beforeUpload={handleImport}
              disabled={importing}
            >
              <p className="ant-upload-drag-icon">
                <InboxOutlined />
              </p>
              <p className="ant-upload-text">点击或拖拽文件到此区域上传</p>
              <p className="ant-upload-hint">支持 .xlsx 和 .csv 格式，最大 10MB</p>
            </Dragger>
          </Card>
        </Col>
      </Row>

      {importing && (
        <Card title="导入进度" variant="outlined" style={{ marginBottom: 16 }}>
          <Descriptions column={4} size="small">
            <Descriptions.Item label="状态">{statusTag(progress.status || 'IMPORTING')}</Descriptions.Item>
            <Descriptions.Item label="总记录">{progress.totalRecords}</Descriptions.Item>
            <Descriptions.Item label="成功"><span style={{ color: '#52c41a' }}>{progress.successCount}</span></Descriptions.Item>
            <Descriptions.Item label="失败"><span style={{ color: '#ff4d4f' }}>{progress.failCount}</span></Descriptions.Item>
          </Descriptions>
          <Progress
            percent={progress.progress}
            success={{ percent: progress.totalRecords > 0 ? (progress.successCount / progress.totalRecords) * 100 : 0 }}
            style={{ marginTop: 12 }}
          />
          <div style={{ marginTop: 12, textAlign: 'right' }}>
            <Button icon={<StopOutlined />} onClick={cancelImport} danger>取消导入</Button>
          </div>
        </Card>
      )}

      {completedResult && !importing && (
        <Card
          title="导入结果"
          variant="outlined"
          style={{ marginBottom: 16 }}
          extra={
            completedResult.failCount > 0 ? (
              <Space>
                <Button type="link" onClick={() => downloadErrorReport(completedResult.taskId)}>
                  下载错误报告
                </Button>
                <Button type="link" icon={<ReloadOutlined />} onClick={() => message.info('请修正错误后重新上传文件，已成功的记录会自动跳过')}>
                  重试失败记录
                </Button>
              </Space>
            ) : null
          }
        >
          <Row gutter={24}>
            <Col span={6}>
              <Statistic
                title="状态"
                value={completedResult.status === 'COMPLETED' ? '导入完成' : '导入失败'}
                valueStyle={{ color: completedResult.status === 'COMPLETED' ? '#52c41a' : '#ff4d4f' }}
                prefix={completedResult.status === 'COMPLETED' ? <CheckCircleOutlined /> : <CloseCircleOutlined />}
              />
            </Col>
            <Col span={6}>
              <Statistic title="总记录" value={completedResult.totalRecords} />
            </Col>
            <Col span={6}>
              <Statistic title="成功" value={completedResult.successCount} valueStyle={{ color: '#52c41a' }} />
            </Col>
            <Col span={6}>
              <Statistic title="失败" value={completedResult.failCount} valueStyle={{ color: completedResult.failCount > 0 ? '#ff4d4f' : undefined }} />
            </Col>
          </Row>
        </Card>
      )}

      <Card
        title="导入历史"
        variant="outlined"
        extra={<Button icon={<ReloadOutlined />} onClick={fetchHistory}>刷新</Button>}
      >
        <Table
          columns={historyColumns}
          dataSource={importHistory}
          rowKey="id"
          loading={historyLoading}
          pagination={{
            ...historyPagination,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
          }}
          onChange={(pag) => setHistoryPagination(prev => ({ ...prev, current: pag.current, pageSize: pag.pageSize }))}
          size="small"
        />
      </Card>
    </div>
  );
};

export default CameraBatchImport;
