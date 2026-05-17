import React, { useEffect, useState, useCallback } from 'react';
import { Card, Table, Switch, Button, Modal, Form, InputNumber, Select, message, Tag, Space } from 'antd';
import { SettingOutlined } from '@ant-design/icons';
import { aiApi, AiAnalysisConfig } from '../services/aiApi';
import PageHeader from '../components/ui/PageHeader';

const defaultConfig: Partial<AiAnalysisConfig> = {
  cameraId: 0,
  enablePassenger: true,
  enableBehavior: true,
  enablePlate: true,
  passengerFrameRate: 1,
  behaviorFrameRate: 2,
  plateFrameRate: 5,
  loiteringThresholdSeconds: 30,
  gatheringMinPeople: 5,
  enabled: true,
};

const AiConfigManagement: React.FC = () => {
  const [configs, setConfigs] = useState<AiAnalysisConfig[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AiAnalysisConfig | null>(null);
  const [form] = Form.useForm();

  const fetchConfigs = useCallback(async () => {
    try {
      const res = await aiApi.getConfigs();
      setConfigs(res.data);
    } catch { /* ignore */ }
  }, []);

  useEffect(() => { fetchConfigs(); }, [fetchConfigs]);

  const handleSave = async () => {
    const values = await form.validateFields();
    const config: Partial<AiAnalysisConfig> = editing
      ? { ...editing, ...values }
      : { ...defaultConfig, ...values };
    try {
      await aiApi.saveConfig(config.cameraId!, config as AiAnalysisConfig);
      message.success(editing ? '配置已更新' : '配置已创建');
      setModalOpen(false);
      setEditing(null);
      form.resetFields();
      fetchConfigs();
    } catch {
      message.error('保存失败');
    }
  };

  const handleDelete = async (cameraId: number) => {
    await aiApi.deleteConfig(cameraId);
    message.success('配置已删除');
    fetchConfigs();
  };

  const columns = [
    { title: '摄像头ID', dataIndex: 'cameraId', key: 'cameraId' },
    { title: '客流分析', dataIndex: 'enablePassenger', key: 'enablePassenger',
      render: (v: boolean) => v ? <Tag color="green">开启</Tag> : <Tag>关闭</Tag> },
    { title: '行为分析', dataIndex: 'enableBehavior', key: 'enableBehavior',
      render: (v: boolean) => v ? <Tag color="green">开启</Tag> : <Tag>关闭</Tag> },
    { title: '车牌识别', dataIndex: 'enablePlate', key: 'enablePlate',
      render: (v: boolean) => v ? <Tag color="green">开启</Tag> : <Tag>关闭</Tag> },
    { title: '客流帧率', dataIndex: 'passengerFrameRate', key: 'pf', render: (v: number) => `${v}fps` },
    { title: '行为帧率', dataIndex: 'behaviorFrameRate', key: 'bf', render: (v: number) => `${v}fps` },
    { title: '车牌帧率', dataIndex: 'plateFrameRate', key: 'plf', render: (v: number) => `${v}fps` },
    { title: '启用', dataIndex: 'enabled', key: 'enabled',
      render: (v: boolean) => <Switch checked={v} disabled /> },
    { title: '操作', key: 'action',
      render: (_: unknown, record: AiAnalysisConfig) => (
        <Space>
          <Button size="small" onClick={() => {
            setEditing(record);
            form.setFieldsValue(record);
            setModalOpen(true);
          }}>编辑</Button>
          <Button size="small" danger onClick={() => handleDelete(record.cameraId)}>删除</Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <PageHeader title="AI分析配置" icon={<SettingOutlined />} />
      <Card extra={<Button type="primary" onClick={() => {
        setEditing(null); form.resetFields(); setModalOpen(true);
      }}>新增配置</Button>}>
        <Table dataSource={configs} columns={columns} rowKey="id" pagination={false} />
      </Card>

      <Modal title={editing ? '编辑AI配置' : '新增AI配置'} open={modalOpen}
        width={600} onOk={handleSave}
        onCancel={() => { setModalOpen(false); setEditing(null); }}>
        <Form form={form} layout="vertical">
          <Form.Item name="cameraId" label="摄像头ID" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={1} disabled={!!editing} />
          </Form.Item>
          <Space style={{ width: '100%' }} size="middle">
            <Form.Item name="enablePassenger" label="客流分析" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="enableBehavior" label="行为分析" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="enablePlate" label="车牌识别" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="enabled" label="启用" valuePropName="checked">
              <Switch defaultChecked />
            </Form.Item>
          </Space>
          <Space style={{ width: '100%' }} size="middle">
            <Form.Item name="passengerFrameRate" label="客流帧率(fps)">
              <InputNumber min={1} max={30} />
            </Form.Item>
            <Form.Item name="behaviorFrameRate" label="行为帧率(fps)">
              <InputNumber min={1} max={30} />
            </Form.Item>
            <Form.Item name="plateFrameRate" label="车牌帧率(fps)">
              <InputNumber min={1} max={30} />
            </Form.Item>
          </Space>
          <Space style={{ width: '100%' }} size="middle">
            <Form.Item name="loiteringThresholdSeconds" label="滞留阈值(秒)">
              <InputNumber min={5} max={300} />
            </Form.Item>
            <Form.Item name="gatheringMinPeople" label="聚集人数">
              <InputNumber min={2} max={50} />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </div>
  );
};

export default AiConfigManagement;
