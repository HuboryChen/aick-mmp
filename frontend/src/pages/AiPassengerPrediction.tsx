import React, { useEffect, useState, useCallback } from 'react';
import { Card, Row, Col, Select, Statistic, Table, Tag, Spin, Space, Slider } from 'antd';
import { LineChartOutlined, ArrowUpOutlined, ArrowDownOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { aiApi } from '../services/aiApi';
import PageHeader from '../components/ui/PageHeader';

const confidenceColors: Record<string, string> = { high: 'green', medium: 'orange', low: 'red' };

const AiPassengerPrediction: React.FC = () => {
  const [data, setData] = useState<Array<{timeSlot: string; predictedEnter: number; predictedExit: number; confidence: string}>>([]);
  const [cameraId, setCameraId] = useState<number>(1);
  const [historyHours, setHistoryHours] = useState(24);
  const [loading, setLoading] = useState(false);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await aiApi.getPassengerPrediction(cameraId, historyHours, 8);
      setData(res.data);
    } catch { /* ignore */ }
    setLoading(false);
  }, [cameraId, historyHours]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const avgEnter = data.length ? Math.round(data.reduce((s, d) => s + d.predictedEnter, 0) / data.length) : 0;
  const avgExit = data.length ? Math.round(data.reduce((s, d) => s + d.predictedExit, 0) / data.length) : 0;
  const maxVal = Math.max(...data.map(d => Math.max(d.predictedEnter, d.predictedExit)), 1);

  const columns = [
    { title: '时段', dataIndex: 'timeSlot', key: 'time' },
    { title: '预测进入', dataIndex: 'predictedEnter', key: 'enter',
      render: (v: number) => <span style={{ color: '#3f8600' }}>{v}</span> },
    { title: '预测离开', dataIndex: 'predictedExit', key: 'exit',
      render: (v: number) => <span style={{ color: '#cf1322' }}>{v}</span> },
    { title: '置信度', dataIndex: 'confidence', key: 'conf',
      render: (v: string) => <Tag color={confidenceColors[v]}>{v === 'high' ? '高' : v === 'medium' ? '中' : '低'}</Tag> },
  ];

  return (
    <div>
      <PageHeader title="客流预测" icon={<LineChartOutlined />} />
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={8}><Card><Statistic title="平均预测进入" value={avgEnter} prefix={<ArrowUpOutlined />} valueStyle={{ color: '#3f8600' }} /></Card></Col>
        <Col span={8}><Card><Statistic title="平均预测离开" value={avgExit} prefix={<ArrowDownOutlined />} valueStyle={{ color: '#cf1322' }} /></Card></Col>
        <Col span={8}><Card><Statistic title="预测时段数" value={data.length} prefix={<MinusCircleOutlined />} /></Card></Col>
      </Row>
      <Card title="客流预测趋势" extra={
        <Space>
          <Select defaultValue={1} style={{ width: 150 }} onChange={v => setCameraId(Number(v))}>
            <Select.Option value={1}>Camera 1</Select.Option>
            <Select.Option value={2}>Camera 2</Select.Option>
          </Select>
          <span>历史数据：{historyHours}小时</span>
          <Slider style={{ width: 120 }} min={6} max={72} value={historyHours} onChange={v => setHistoryHours(v)} />
        </Space>
      }>
        {loading ? <Spin /> : (
          <>
            <div style={{ display: 'flex', gap: 2, marginBottom: 16, height: 120, alignItems: 'flex-end' }}>
              {data.map((d, i) => {
                const hEnter = (d.predictedEnter / maxVal) * 100;
                const hExit = (d.predictedExit / maxVal) * 100;
                return (
                  <div key={i} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
                    <div style={{ width: '60%', height: `${Math.max(hEnter, 2)}%`, background: '#3f8600', borderRadius: '2px 2px 0 0', minWidth: 6 }} title={`进入: ${d.predictedEnter}`} />
                    <div style={{ width: '60%', height: `${Math.max(hExit, 2)}%`, background: '#cf1322', borderRadius: '2px 2px 0 0', minWidth: 6 }} title={`离开: ${d.predictedExit}`} />
                  </div>
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

export default AiPassengerPrediction;
