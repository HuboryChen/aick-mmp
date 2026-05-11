import React, { useEffect, useState, useCallback } from 'react';
import { Card, Table, Tag, Button, Select, Badge, message, Space } from 'antd';
import { WarningOutlined } from '@ant-design/icons';
import { aiApi, BehaviorEvent } from '../services/aiApi';
import PageHeader from '../components/ui/PageHeader';

const levelColors: Record<string, string> = {
  INFO: 'blue', WARNING: 'orange', CRITICAL: 'red',
};
const statusColors: Record<string, string> = {
  UNRESOLVED: 'error', ACKNOWLEDGED: 'warning', RESOLVED: 'success',
};

const AiBehaviorAlertCenter: React.FC = () => {
  const [events, setEvents] = useState<BehaviorEvent[]>([]);
  const [filterType, setFilterType] = useState<string | undefined>();
  const [filterStatus, setFilterStatus] = useState<string | undefined>();

  const fetchEvents = useCallback(async () => {
    try {
      const res = await aiApi.getBehaviorEvents(0, filterType, filterStatus);
      setEvents(res.data);
    } catch { /* ignore */ }
  }, [filterType, filterStatus]);

  useEffect(() => { fetchEvents(); }, [fetchEvents]);

  const handleResolve = async (id: number) => {
    await aiApi.updateBehaviorStatus(id, 'RESOLVED');
    message.success('告警已处理');
    fetchEvents();
  };

  const columns = [
    { title: '类型', dataIndex: 'eventType', key: 'type',
      render: (t: string) => <Tag color={t === 'FALL' ? 'red' : 'blue'}>{t}</Tag> },
    { title: '级别', dataIndex: 'level', key: 'level',
      render: (l: string) => <Badge color={levelColors[l]} text={l} /> },
    { title: '描述', dataIndex: 'description', key: 'desc' },
    { title: '时间', dataIndex: 'eventTime', key: 'time' },
    { title: '状态', dataIndex: 'status', key: 'status',
      render: (s: string) => <Tag color={statusColors[s]}>{s}</Tag> },
    { title: '操作', key: 'action',
      render: (_: unknown, record: BehaviorEvent) => (
        record.status === 'UNRESOLVED' ? (
          <Button size="small" onClick={() => handleResolve(record.id)}>处理</Button>
        ) : null
      ),
    },
  ];

  return (
    <div>
      <PageHeader title="行为告警中心" icon={<WarningOutlined />} />
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Select placeholder="告警类型" allowClear style={{ width: 150 }} onChange={setFilterType}>
            <Select.Option value="LOITERING">滞留</Select.Option>
            <Select.Option value="INTRUSION">闯入</Select.Option>
            <Select.Option value="GATHERING">聚集</Select.Option>
            <Select.Option value="FALL">跌倒</Select.Option>
          </Select>
          <Select placeholder="状态" allowClear style={{ width: 150 }} onChange={setFilterStatus}>
            <Select.Option value="UNRESOLVED">未处理</Select.Option>
            <Select.Option value="ACKNOWLEDGED">确认中</Select.Option>
            <Select.Option value="RESOLVED">已处理</Select.Option>
          </Select>
          <Button type="primary" onClick={fetchEvents}>刷新</Button>
        </Space>
        <Table dataSource={events} columns={columns} rowKey="id" pagination={{ pageSize: 20 }} />
      </Card>
    </div>
  );
};

export default AiBehaviorAlertCenter;
