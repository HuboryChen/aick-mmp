import React, { useEffect, useState, useCallback } from 'react';
import { Card, Row, Col, Select, DatePicker, Statistic, Table, Spin, Space } from 'antd';
import { HeatMapOutlined, UserOutlined, ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons';
import { aiApi } from '../services/aiApi';
import PageHeader from '../components/ui/PageHeader';

const { RangePicker } = DatePicker;

const AiHeatmap: React.FC = () => {
  const [data, setData] = useState<Array<{timeSlot: string; avgInside: number; totalEnter: number; totalExit: number}>>([]);
  const [cameraId, setCameraId] = useState<number>(1);
  const [loading, setLoading] = useState(false);

  const fetchData = useCallback(async (start?: string, end?: string) => {
    setLoading(true);
    try {
      const endTime = end || new Date().toISOString();
      const startTime = start || new Date(Date.now() - 24 * 3600000).toISOString();
      const res = await aiApi.getHeatmapData(cameraId, startTime, endTime);
      setData(res.data);
    } catch { /* ignore */ }
    setLoading(false);
  }, [cameraId]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const totalEnter = data.reduce((s, d) => s + d.totalEnter, 0);
  const totalExit = data.reduce((s, d) => s + d.totalExit, 0);
  const maxInside = Math.max(...data.map(d => d.avgInside), 0);
  const maxVal = Math.max(...data.map(d => d.avgInside), 1);
  const maxHeat = Math.max(...data.map(d => d.totalEnter), 1);

  const columns = [
    { title: '时段', dataIndex: 'timeSlot', key: 'time' },
    { title: '平均滞留', dataIndex: 'avgInside', key: 'inside', render: (v: number) => v.toFixed(1) },
    { title: '进入总数', dataIndex: 'totalEnter', key: 'enter' },
    { title: '离开总数', dataIndex: 'totalExit', key: 'exit' },
    {
      title: '热力强度', key: 'heat',
      render: (_: unknown, record: typeof data[0]) => {
        const intensity = Math.min(record.totalEnter / maxHeat, 1);
        return <div style={{ width: 60, height: 16, background: `rgba(255, 0, 0, ${intensity * 0.8})`, borderRadius: 2 }} />;
      },
    },
  ];

  return (
    <div>
      <PageHeader title="区域热力图" icon={<HeatMapOutlined />} />
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={8}><Card><Statistic title="总进入" value={totalEnter} prefix={<ArrowUpOutlined />} /></Card></Col>
        <Col span={8}><Card><Statistic title="总离开" value={totalExit} prefix={<ArrowDownOutlined />} /></Card></Col>
        <Col span={8}><Card><Statistic title="峰值滞留" value={maxInside.toFixed(1)} prefix={<UserOutlined />} /></Card></Col>
      </Row>
      <Card title="时段热力分布" extra={
        <Space>
          <Select defaultValue={1} style={{ width: 150 }} onChange={v => setCameraId(Number(v))}>
            <Select.Option value={1}>Camera 1</Select.Option>
            <Select.Option value={2}>Camera 2</Select.Option>
          </Select>
          <RangePicker showTime onChange={(_, strs) => {
            if (strs[0] && strs[1]) fetchData(strs[0], strs[1]);
          }} />
        </Space>
      }>
        {loading ? <Spin /> : (
          <>
            <div style={{ display: 'flex', gap: 2, marginBottom: 16, height: 100, alignItems: 'flex-end' }}>
              {data.map((d, i) => {
                const h = (d.avgInside / maxVal) * 100;
                const r = Math.round(255 * (1 - d.avgInside / maxVal));
                const g = Math.round(255 * d.avgInside / maxVal);
                return (
                  <div key={i} style={{
                    flex: 1, height: `${Math.max(h, 2)}%`, background: `rgb(${r}, ${g}, 50)`,
                    minWidth: 8, borderRadius: '2px 2px 0 0',
                  }} title={`${d.timeSlot}: ${d.avgInside.toFixed(1)}`} />
                );
              })}
            </div>
            <Table dataSource={data} columns={columns} rowKey="timeSlot" pagination={false} size="small" />
          </>
        )}
      </Card>
    </div>
  );
};

export default AiHeatmap;
