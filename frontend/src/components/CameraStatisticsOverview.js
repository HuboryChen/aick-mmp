import React, { useState, useEffect, useCallback } from 'react';
import { Card, Row, Col, Statistic, Tag, Spin, Table, Progress, Space, Button, Tooltip } from 'antd';
import { 
  VideoCameraOutlined, CheckCircleOutlined, CloseCircleOutlined, 
  ExclamationCircleOutlined, ReloadOutlined, HistoryOutlined, ClockCircleOutlined
} from '@ant-design/icons';
import { cameraApi } from '../utils/api';
import dayjs from 'dayjs';

/**
 * 摄像头统计概览组件
 * 显示摄像头状态分布、录像统计等汇总信息
 */
const CameraStatisticsOverview = ({ visible, onClose }) => {
  const [loading, setLoading] = useState(false);
  const [statistics, setStatistics] = useState(null);
  const [recentAlerts, setRecentAlerts] = useState([]);
  const [refreshKey, setRefreshKey] = useState(0);

  const loadStatistics = useCallback(async () => {
    setLoading(true);
    try {
      const response = await cameraApi.getCameras({ page: 0, size: 1000 });
      const cameras = response.data?.content || response.data || [];
      
      // 计算统计数据
      const stats = {
        total: cameras.length,
        online: cameras.filter(c => c.status === 'ONLINE').length,
        offline: cameras.filter(c => c.status === 'OFFLINE').length,
        error: cameras.filter(c => c.status === 'ERROR').length,
        connecting: cameras.filter(c => c.status === 'CONNECTING').length,
        maintenance: cameras.filter(c => c.status === 'MAINTENANCE').length,
        pending: cameras.filter(c => c.status === 'PENDING_ALLOCATION').length,
        
        // 按区域分布
        byRegion: {},
        
        // 按节点分布
        byEdgeNode: {},
      };
      
      // 计算区域分布
      cameras.forEach(c => {
        const regionName = c.regionName || '未分配';
        stats.byRegion[regionName] = (stats.byRegion[regionName] || 0) + 1;
      });
      
      // 计算节点分布
      cameras.forEach(c => {
        const nodeName = c.edgeNodeName || '未分配';
        stats.byEdgeNode[nodeName] = (stats.byEdgeNode[nodeName] || 0) + 1;
      });
      
      setStatistics(stats);
    } catch (error) {
      console.error('加载统计数据失败:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (visible) {
      loadStatistics();
    }
  }, [visible, loadStatistics, refreshKey]);

  const getOnlineRate = () => {
    if (!statistics) return 0;
    return statistics.total > 0 ? (statistics.online / statistics.total * 100).toFixed(1) : 0;
  };

  const statusColumns = [
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status) => {
        const config = {
          'ONLINE': { color: 'green', label: '在线' },
          'OFFLINE': { color: 'red', label: '离线' },
          'ERROR': { color: 'orange', label: '错误' },
          'CONNECTING': { color: 'blue', label: '连接中' },
          'MAINTENANCE': { color: 'purple', label: '维护中' },
          'PENDING_ALLOCATION': { color: 'cyan', label: '待分配' },
        };
        const { color, label } = config[status] || { color: 'default', label: status };
        return <Tag color={color}>{label}</Tag>;
      },
    },
    {
      title: '数量',
      dataIndex: 'count',
      key: 'count',
      width: 100,
    },
    {
      title: '占比',
      dataIndex: 'percentage',
      key: 'percentage',
      render: (_, record) => {
        const total = statistics?.total || 1;
        const percentage = ((record.count / total) * 100).toFixed(1);
        return (
          <Progress 
            percent={parseFloat(percentage)} 
            size="small" 
            showInfo={false}
            strokeColor={record.color}
          />
        );
      },
    },
  ];

  const statusData = statistics ? [
    { status: 'ONLINE', count: statistics.online, color: '#52c41a' },
    { status: 'OFFLINE', count: statistics.offline, color: '#ff4d4f' },
    { status: 'ERROR', count: statistics.error, color: '#fa8c16' },
    { status: 'CONNECTING', count: statistics.connecting, color: '#1890ff' },
    { status: 'MAINTENANCE', count: statistics.maintenance, color: '#722ed1' },
    { status: 'PENDING_ALLOCATION', count: statistics.pending, color: '#13c2c2' },
  ] : [];

  if (!visible) return null;

  return (
    <div style={{ 
      position: 'fixed', 
      top: 0, 
      left: 0, 
      right: 0, 
      bottom: 0, 
      background: 'rgba(0,0,0,0.5)',
      zIndex: 1000,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }}>
      <Card
        title={
          <Space>
            <HistoryOutlined />
            <span>摄像头统计概览</span>
          </Space>
        }
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => setRefreshKey(k => k + 1)}>
              刷新
            </Button>
            <Button onClick={onClose}>关闭</Button>
          </Space>
        }
        style={{ 
          width: 800, 
          maxHeight: '80vh',
          overflow: 'auto'
        }}
      >
        {loading ? (
          <div style={{ textAlign: 'center', padding: 50 }}>
            <Spin size="large" />
          </div>
        ) : statistics ? (
          <>
            {/* 概览统计 */}
            <Row gutter={16} style={{ marginBottom: 24 }}>
              <Col span={6}>
                <Card size="small">
                  <Statistic
                    title="摄像头总数"
                    value={statistics.total}
                    prefix={<VideoCameraOutlined />}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card size="small">
                  <Statistic
                    title="在线率"
                    value={getOnlineRate()}
                    suffix="%"
                    valueStyle={{ color: '#52c41a' }}
                    prefix={<CheckCircleOutlined />}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card size="small">
                  <Statistic
                    title="在线"
                    value={statistics.online}
                    valueStyle={{ color: '#52c41a' }}
                    prefix={<CheckCircleOutlined />}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card size="small">
                  <Statistic
                    title="离线"
                    value={statistics.offline}
                    valueStyle={{ color: '#ff4d4f' }}
                    prefix={<CloseCircleOutlined />}
                  />
                </Card>
              </Col>
            </Row>

            {/* 状态分布 */}
            <Table
              title={() => '状态分布'}
              columns={statusColumns}
              dataSource={statusData}
              rowKey="status"
              pagination={false}
              style={{ marginBottom: 24 }}
            />

            {/* 区域分布 */}
            {Object.keys(statistics.byRegion).length > 0 && (
              <div style={{ marginBottom: 24 }}>
                <h4>区域分布</h4>
                <Row gutter={[8, 8]}>
                  {Object.entries(statistics.byRegion).map(([region, count]) => (
                    <Col span={8} key={region}>
                      <Tag color="blue">
                        {region}: {count}
                      </Tag>
                    </Col>
                  ))}
                </Row>
              </div>
            )}

            {/* 节点分布 */}
            {Object.keys(statistics.byEdgeNode).length > 0 && (
              <div>
                <h4>节点分布</h4>
                <Row gutter={[8, 8]}>
                  {Object.entries(statistics.byEdgeNode).map(([node, count]) => (
                    <Col span={8} key={node}>
                      <Tag color="purple">
                        {node}: {count}
                      </Tag>
                    </Col>
                  ))}
                </Row>
              </div>
            )}
          </>
        ) : null}
      </Card>
    </div>
  );
};

export default CameraStatisticsOverview;
