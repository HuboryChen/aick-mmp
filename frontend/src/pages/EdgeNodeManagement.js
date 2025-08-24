import React, { useState, useEffect } from 'react';
import { Table, Button, Space, Tag, Card, Typography, Progress, Tooltip } from 'antd';
import { ReloadOutlined, SettingOutlined, EyeOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;

const EdgeNodeManagement = () => {
  const [nodes, setNodes] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchEdgeNodes();
  }, []);

  const fetchEdgeNodes = async () => {
    setLoading(true);
    try {
      // 模拟数据
      setNodes([
        {
          id: 1,
          name: 'Node-A-01',
          region: '地区A',
          ip: '192.168.1.100',
          status: 'online',
          cpuUsage: 45,
          memoryUsage: 62,
          diskUsage: 38,
          connectedCameras: 8,
          maxCameras: 10,
          bandwidth: '85 Mbps',
          lastHeartbeat: '2024-01-15 14:30:25'
        },
        {
          id: 2,
          name: 'Node-A-02',
          region: '地区A',
          ip: '192.168.1.101',
          status: 'warning',
          cpuUsage: 78,
          memoryUsage: 85,
          diskUsage: 92,
          connectedCameras: 6,
          maxCameras: 8,
          bandwidth: '120 Mbps',
          lastHeartbeat: '2024-01-15 14:29:45'
        },
        {
          id: 3,
          name: 'Node-B-01',
          region: '地区B',
          ip: '192.168.2.100',
          status: 'online',
          cpuUsage: 32,
          memoryUsage: 48,
          diskUsage: 55,
          connectedCameras: 5,
          maxCameras: 12,
          bandwidth: '95 Mbps',
          lastHeartbeat: '2024-01-15 14:30:30'
        },
        {
          id: 4,
          name: 'Node-B-02',
          region: '地区B',
          ip: '192.168.2.101',
          status: 'offline',
          cpuUsage: 0,
          memoryUsage: 0,
          diskUsage: 0,
          connectedCameras: 0,
          maxCameras: 8,
          bandwidth: '0 Mbps',
          lastHeartbeat: '2024-01-15 13:45:12'
        }
      ]);
    } catch (error) {
      console.error('获取边缘节点失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'online': return 'green';
      case 'warning': return 'orange';
      case 'offline': return 'red';
      default: return 'default';
    }
  };

  const getStatusText = (status) => {
    switch (status) {
      case 'online': return '在线';
      case 'warning': return '警告';
      case 'offline': return '离线';
      default: return '未知';
    }
  };

  const getUsageColor = (usage) => {
    if (usage >= 90) return '#ff4d4f';
    if (usage >= 70) return '#faad14';
    return '#52c41a';
  };

  const columns = [
    {
      title: '节点名称',
      dataIndex: 'name',
      key: 'name',
      render: (name, record) => (
        <div>
          <div style={{ fontWeight: 'bold' }}>{name}</div>
          <Text type="secondary" style={{ fontSize: '12px' }}>{record.ip}</Text>
        </div>
      ),
    },
    {
      title: '所属地区',
      dataIndex: 'region',
      key: 'region',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => (
        <Tag color={getStatusColor(status)}>
          {getStatusText(status)}
        </Tag>
      ),
    },
    {
      title: 'CPU使用率',
      dataIndex: 'cpuUsage',
      key: 'cpuUsage',
      render: (usage) => (
        <div style={{ width: '80px' }}>
          <Progress
            percent={usage}
            size="small"
            strokeColor={getUsageColor(usage)}
            format={(percent) => `${percent}%`}
          />
        </div>
      ),
    },
    {
      title: '内存使用率',
      dataIndex: 'memoryUsage',
      key: 'memoryUsage',
      render: (usage) => (
        <div style={{ width: '80px' }}>
          <Progress
            percent={usage}
            size="small"
            strokeColor={getUsageColor(usage)}
            format={(percent) => `${percent}%`}
          />
        </div>
      ),
    },
    {
      title: '磁盘使用率',
      dataIndex: 'diskUsage',
      key: 'diskUsage',
      render: (usage) => (
        <div style={{ width: '80px' }}>
          <Progress
            percent={usage}
            size="small"
            strokeColor={getUsageColor(usage)}
            format={(percent) => `${percent}%`}
          />
        </div>
      ),
    },
    {
      title: '连接摄像头',
      key: 'cameras',
      render: (_, record) => (
        <div>
          <Text>{record.connectedCameras} / {record.maxCameras}</Text>
          <Progress
            percent={Math.round((record.connectedCameras / record.maxCameras) * 100)}
            size="small"
            showInfo={false}
            style={{ marginTop: '4px' }}
          />
        </div>
      ),
    },
    {
      title: '带宽',
      dataIndex: 'bandwidth',
      key: 'bandwidth',
    },
    {
      title: '最后心跳',
      dataIndex: 'lastHeartbeat',
      key: 'lastHeartbeat',
      render: (time) => (
        <Text style={{ fontSize: '12px' }}>{time}</Text>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="middle">
          <Tooltip title="重启节点">
            <Button
              type="link"
              icon={<ReloadOutlined />}
              disabled={record.status === 'offline'}
            />
          </Tooltip>
          <Tooltip title="查看详情">
            <Button
              type="link"
              icon={<EyeOutlined />}
            />
          </Tooltip>
          <Tooltip title="节点配置">
            <Button
              type="link"
              icon={<SettingOutlined />}
            />
          </Tooltip>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card>
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Title level={3} style={{ margin: 0 }}>边缘节点管理</Title>
          <Button icon={<ReloadOutlined />} onClick={fetchEdgeNodes} loading={loading}>
            刷新
          </Button>
        </div>
        
        <Table
          columns={columns}
          dataSource={nodes}
          loading={loading}
          rowKey="id"
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 个节点`,
          }}
          scroll={{ x: 1200 }}
        />
      </Card>
    </div>
  );
};

export default EdgeNodeManagement;