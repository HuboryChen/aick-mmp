import React, { useEffect, useState, useCallback } from 'react';
import { Card, Table, Input, Button, Modal, Form, Switch, message, Tag, Space } from 'antd';
import { CarOutlined } from '@ant-design/icons';
import { aiApi, VehicleRecord, WhitelistEntry } from '../services/aiApi';
import PageHeader from '../components/ui/PageHeader';

const AiLicensePlateManagement: React.FC = () => {
  const [records, setRecords] = useState<VehicleRecord[]>([]);
  const [whitelist, setWhitelist] = useState<WhitelistEntry[]>([]);
  const [plateFilter, setPlateFilter] = useState<string>();
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form] = Form.useForm();

  const fetchRecords = useCallback(async () => {
    try {
      const res = await aiApi.getVehicleRecords(plateFilter);
      setRecords(res.data);
    } catch { /* ignore */ }
  }, [plateFilter]);

  const fetchWhitelist = useCallback(async () => {
    try {
      const res = await aiApi.getWhitelist();
      setWhitelist(res.data);
    } catch { /* ignore */ }
  }, []);

  useEffect(() => { fetchRecords(); }, [fetchRecords]);
  useEffect(() => { fetchWhitelist(); }, [fetchWhitelist]);

  const handleSave = async () => {
    const values = await form.validateFields();
    if (editingId) {
      await aiApi.updateWhitelist(editingId, values);
      message.success('白名单已更新');
    } else {
      await aiApi.addWhitelist(values);
      message.success('白名单已添加');
    }
    setModalOpen(false);
    setEditingId(null);
    form.resetFields();
    fetchWhitelist();
  };

  const handleDelete = async (id: number) => {
    await aiApi.deleteWhitelist(id);
    message.success('已删除');
    fetchWhitelist();
  };

  const recordColumns = [
    { title: '车牌号', dataIndex: 'plateNumber', key: 'plate' },
    { title: '颜色', dataIndex: 'plateColor', key: 'color' },
    { title: '置信度', dataIndex: 'confidence', key: 'confidence',
      render: (v: number) => v ? `${(v * 100).toFixed(1)}%` : '-' },
    { title: '白名单', dataIndex: 'isWhitelisted', key: 'wl',
      render: (v: boolean) => v ? <Tag color="green">是</Tag> : <Tag>否</Tag> },
    { title: '时间', dataIndex: 'detectTime', key: 'time' },
  ];

  const whitelistColumns = [
    { title: '车牌号', dataIndex: 'plateNumber', key: 'plate' },
    { title: '车主', dataIndex: 'ownerName', key: 'owner' },
    { title: '启用', dataIndex: 'enabled', key: 'enabled',
      render: (v: boolean) => <Switch checked={v} disabled /> },
    { title: '操作', key: 'action',
      render: (_: unknown, record: WhitelistEntry) => (
        <Space>
          <Button size="small" onClick={() => {
            setEditingId(record.id!);
            form.setFieldsValue(record);
            setModalOpen(true);
          }}>编辑</Button>
          <Button size="small" danger onClick={() => handleDelete(record.id!)}>删除</Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <PageHeader title="车牌管理" icon={<CarOutlined />} />
      <Card title="识别记录" style={{ marginBottom: 16 }}>
        <Space style={{ marginBottom: 16 }}>
          <Input.Search
            placeholder="搜索车牌号"
            onSearch={setPlateFilter}
            style={{ width: 250 }}
          />
        </Space>
        <Table dataSource={records} columns={recordColumns} rowKey="id" pagination={{ pageSize: 10 }} />
      </Card>

      <Card title="白名单管理" extra={<Button type="primary" onClick={() => {
        setEditingId(null); form.resetFields(); setModalOpen(true);
      }}>添加</Button>}>
        <Table dataSource={whitelist} columns={whitelistColumns} rowKey="id" pagination={false} />
      </Card>

      <Modal
        title={editingId ? '编辑白名单' : '添加白名单'}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => { setModalOpen(false); setEditingId(null); }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="plateNumber" label="车牌号" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="plateColor" label="颜色">
            <Input />
          </Form.Item>
          <Form.Item name="ownerName" label="车主">
            <Input />
          </Form.Item>
          <Form.Item name="description" label="备注">
            <Input.TextArea />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch defaultChecked />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default AiLicensePlateManagement;
