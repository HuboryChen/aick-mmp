import React, { useEffect, useState, useCallback } from 'react';
import { Card, Row, Col, Statistic, Select, DatePicker, Table } from 'antd';
import { UserOutlined, ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons';
import { aiApi, PassengerStats } from '../services/aiApi';
import PageHeader from '../components/ui/PageHeader';
import dayjs from 'dayjs';

const AiPassengerDashboard: React.FC = () => {
  const [cameraId, setCameraId] = useState<number>(1);
  const [realtime, setRealtime] = useState<string>('0');
  const [stats, setStats] = useState<PassengerStats[]>([]);
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs]>([
    dayjs().startOf('day'), dayjs(),
  ]);

  const fetchRealtime = useCallback(async () => {
    try {
      const res = await aiApi.getRealtimePassenger(cameraId);
      setRealtime(res.data);
    } catch { /* ignore polling errors */ }
  }, [cameraId]);

  const fetchStats = useCallback(async () => {
    try {
      const [from, to] = dateRange;
      const res = await aiApi.getPassengerStats(
        cameraId, from.toISOString(), to.toISOString(),
      );
      setStats(res.data);
    } catch { /* ignore */ }
  }, [cameraId, dateRange]);

  useEffect(() => {
    fetchStats();
    const interval = setInterval(fetchRealtime, 5000);
    return () => clearInterval(interval);
  }, [fetchStats, fetchRealtime]);

  const totalEnter = stats.reduce((s, r) => s + r.enterCount, 0);
  const totalExit = stats.reduce((s, r) => s + r.exitCount, 0);

  const columns = [
    { title: '时间', dataIndex: 'startTime', key: 'time', render: (v: string) => dayjs(v).format('HH:mm') },
    { title: '进入', dataIndex: 'enterCount', key: 'enter' },
    { title: '离开', dataIndex: 'exitCount', key: 'exit' },
    { title: '在店', dataIndex: 'insideCount', key: 'inside' },
  ];

  return (
    <div>
      <PageHeader title="客流实时大屏" icon={<UserOutlined />} />
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Select value={cameraId} onChange={setCameraId} style={{ width: '100%' }}>
            <Select.Option value={1}>Camera 1</Select.Option>
            <Select.Option value={2}>Camera 2</Select.Option>
          </Select>
        </Col>
        <Col span={10}>
          <DatePicker.RangePicker
            value={dateRange}
            onChange={(dates) => dates && setDateRange(dates as [dayjs.Dayjs, dayjs.Dayjs])}
          />
        </Col>
      </Row>

      <Row gutter={16}>
        <Col span={8}>
          <Card>
            <Statistic title="实时在店" value={realtime} prefix={<UserOutlined />} suffix="人" />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic title="累计进入" value={totalEnter} prefix={<ArrowUpOutlined />} suffix="人" />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic title="累计离开" value={totalExit} prefix={<ArrowDownOutlined />} suffix="人" />
          </Card>
        </Col>
      </Row>

      <Card title="客流历史数据" style={{ marginTop: 16 }}>
        <Table dataSource={stats} columns={columns} rowKey="id" pagination={{ pageSize: 10 }} />
      </Card>
    </div>
  );
};

export default AiPassengerDashboard;
