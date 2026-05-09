import React, { useState, useEffect, useRef } from 'react';
import { Table, Button, Space, Modal, Form, Input, Select, message, Popconfirm, Tag, Tooltip, Row, Col, Card, Upload } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined, ReloadOutlined, DownloadOutlined, UploadOutlined, LinkOutlined } from '@ant-design/icons';
import { cameraConfigTemplateApi } from '../utils/api';

const { Option } = Select;
const { TextArea } = Input;

const ConfigTemplateManagement = () => {
  const [templates, setTemplates] = useState([]);
  const [brands, setBrands] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [urlModalVisible, setUrlModalVisible] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState(null);
  const [generatedUrl, setGeneratedUrl] = useState('');
  const [urlParams, setUrlParams] = useState({ ip: '', port: '', username: 'admin', password: '', channel: '1' });
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [filters, setFilters] = useState({ brand: undefined, protocol: undefined });
  const [form] = Form.useForm();

  useEffect(() => {
    fetchTemplates();
    fetchBrands();
  }, [pagination.current, pagination.pageSize, filters]);

  const fetchTemplates = async () => {
    setLoading(true);
    try {
      const params = {
        page: pagination.current - 1,
        size: pagination.pageSize,
        brand: filters.brand,
        protocol: filters.protocol,
      };
      const response = await cameraConfigTemplateApi.getTemplates(params);
      setTemplates(response.data?.content || []);
      setPagination(prev => ({ ...prev, total: response.data?.totalElements || 0 }));
    } catch (err) {
      message.error('获取模板列表失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchBrands = async () => {
    try {
      const response = await cameraConfigTemplateApi.getBrands();
      setBrands(response.data || []);
    } catch (err) {
      console.error('Failed to fetch brands:', err);
    }
  };

  const handleCreate = () => {
    setEditingTemplate(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (record) => {
    setEditingTemplate(record);
    form.setFieldsValue(record);
    setModalVisible(true);
  };

  const handleDelete = async (id) => {
    try {
      await cameraConfigTemplateApi.deleteTemplate(id);
      message.success('模板已删除');
      fetchTemplates();
    } catch (err) {
      message.error('删除失败');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (editingTemplate) {
        await cameraConfigTemplateApi.updateTemplate(editingTemplate.id, values);
        message.success('模板已更新');
      } else {
        await cameraConfigTemplateApi.createTemplate(values);
        message.success('模板已创建');
      }
      setModalVisible(false);
      fetchTemplates();
    } catch (err) {
      if (err.errorFields) return;
      message.error('操作失败');
    }
  };

  const handleGenerateUrl = async (record) => {
    setEditingTemplate(record);
    setUrlParams({ ip: '', port: String(record.defaultPort || ''), username: 'admin', password: '', channel: '1' });
    setGeneratedUrl('');
    setUrlModalVisible(true);
  };

  const doGenerateUrl = async () => {
    try {
      const params = {};
      if (urlParams.ip) params.ip = urlParams.ip;
      if (urlParams.port) params.port = urlParams.port;
      if (urlParams.username) params.username = urlParams.username;
      if (urlParams.password) params.password = urlParams.password;
      if (urlParams.channel) params.channel = urlParams.channel;

      const response = await cameraConfigTemplateApi.generateUrl(editingTemplate.id, params);
      setGeneratedUrl(response.data);
    } catch (err) {
      message.error('生成URL失败');
    }
  };

  const handleExport = async () => {
    try {
      const response = await cameraConfigTemplateApi.exportTemplates();
      const data = response.data;
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'camera-config-templates.json');
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      message.success('模板导出成功');
    } catch (err) {
      message.error('导出失败');
    }
  };

  const handleImport = async (file) => {
    try {
      const text = await file.text();
      const templates = JSON.parse(text);
      if (!Array.isArray(templates)) {
        message.error('无效的导入文件格式');
        return false;
      }
      await cameraConfigTemplateApi.importTemplates(templates);
      message.success('模板导入成功');
      fetchTemplates();
    } catch (err) {
      message.error('导入失败，请检查文件格式');
    }
    return false;
  };

  const columns = [
    { title: '品牌', dataIndex: 'brand', key: 'brand', width: 100 },
    { title: '型号', dataIndex: 'model', key: 'model', width: 180 },
    { title: '协议', dataIndex: 'protocol', key: 'protocol', width: 80 },
    { title: '默认端口', dataIndex: 'defaultPort', key: 'defaultPort', width: 90 },
    {
      title: 'URL模板', dataIndex: 'urlPathTemplate', key: 'urlPathTemplate', ellipsis: true,
      render: (text) => <Tooltip title={text}><span style={{ fontFamily: 'monospace', fontSize: 12 }}>{text}</span></Tooltip>,
    },
    {
      title: '预置', dataIndex: 'isPreset', key: 'isPreset', width: 60,
      render: (val) => val ? <Tag color="blue">是</Tag> : <Tag>否</Tag>,
    },
    {
      title: '使用次数', dataIndex: 'usageCount', key: 'usageCount', width: 80,
      render: (val) => val || 0,
    },
    {
      title: '操作', key: 'action', width: 180,
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="生成URL">
            <Button type="link" icon={<LinkOutlined />} onClick={() => handleGenerateUrl(record)} />
          </Tooltip>
          <Tooltip title="编辑">
            <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)} disabled={record.isPreset} />
          </Tooltip>
          <Popconfirm title="确定删除此模板？" onConfirm={() => handleDelete(record.id)}>
            <Tooltip title="删除">
              <Button type="link" danger icon={<DeleteOutlined />} disabled={record.isPreset} />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Card title="摄像头配置模板" variant="outlined" style={{ marginBottom: 16 }}>
        <Row gutter={16} align="middle">
          <Col>
            <Select
              placeholder="选择品牌"
              allowClear
              style={{ width: 150 }}
              value={filters.brand}
              onChange={(val) => setFilters(prev => ({ ...prev, brand: val }))}
            >
              {brands.map(b => <Option key={b} value={b}>{b}</Option>)}
            </Select>
          </Col>
          <Col>
            <Select
              placeholder="选择协议"
              allowClear
              style={{ width: 120 }}
              value={filters.protocol}
              onChange={(val) => setFilters(prev => ({ ...prev, protocol: val }))}
            >
              <Option value="RTSP">RTSP</Option>
              <Option value="ONVIF">ONVIF</Option>
              <Option value="HTTP">HTTP</Option>
            </Select>
          </Col>
          <Col flex="auto">
            <Space>
              <Button icon={<SearchOutlined />} onClick={fetchTemplates}>查询</Button>
              <Button icon={<ReloadOutlined />} onClick={() => { setFilters({ brand: undefined, protocol: undefined }); }}>重置</Button>
            </Space>
          </Col>
          <Col>
            <Space>
              <Button icon={<DownloadOutlined />} onClick={handleExport}>导出</Button>
              <Upload accept=".json" showUploadList={false} beforeUpload={handleImport}>
                <Button icon={<UploadOutlined />}>导入</Button>
              </Upload>
              <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>新增模板</Button>
            </Space>
          </Col>
        </Row>
      </Card>

      <Table
        columns={columns}
        dataSource={templates}
        rowKey="id"
        loading={loading}
        pagination={{
          ...pagination,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
        }}
        onChange={(pag) => setPagination(prev => ({ ...prev, current: pag.current, pageSize: pag.pageSize }))}
        size="small"
      />

      <Modal
        title={editingTemplate ? '编辑模板' : '新增模板'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="brand" label="品牌" rules={[{ required: true, message: '请输入品牌' }]}>
                <Input placeholder="如: 海康威视" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="model" label="型号" rules={[{ required: true, message: '请输入型号' }]}>
                <Input placeholder="如: DS-2CD2T45D-I5" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="protocol" label="协议" rules={[{ required: true, message: '请选择协议' }]}>
                <Select placeholder="选择协议">
                  <Option value="RTSP">RTSP</Option>
                  <Option value="ONVIF">ONVIF</Option>
                  <Option value="HTTP">HTTP</Option>
                  <Option value="GB28181">GB28181</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="defaultPort" label="默认端口" rules={[{ required: true, message: '请输入默认端口' }]}>
                <Input type="number" placeholder="如: 554" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="urlPathTemplate" label="URL模板" rules={[{ required: true, message: '请输入URL模板' }]}>
            <TextArea rows={3} placeholder="如: rtsp://{username}:{password}@{ip}:{port}/Streaming/Channels/{channel}01" />
          </Form.Item>
          <div style={{ background: '#f5f5f5', padding: 8, borderRadius: 4, marginBottom: 16, fontSize: 12, color: '#888' }}>
            可用变量: {'{ip}'}, {'{port}'}, {'{username}'}, {'{password}'}, {'{channel}'}
          </div>
          <Form.Item name="presetParameters" label="预设参数 (JSON)">
            <TextArea rows={2} placeholder='如: {"channel":"1"}' />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="生成连接URL"
        open={urlModalVisible}
        onCancel={() => setUrlModalVisible(false)}
        footer={[
          <Button key="generate" type="primary" onClick={doGenerateUrl}>生成URL</Button>,
          <Button key="close" onClick={() => setUrlModalVisible(false)}>关闭</Button>,
        ]}
        width={600}
      >
        <div style={{ marginBottom: 16 }}>
          <div style={{ fontWeight: 'bold', marginBottom: 8 }}>模板: {editingTemplate?.urlPathTemplate}</div>
        </div>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item label="IP地址">
              <Input value={urlParams.ip} onChange={e => setUrlParams(prev => ({ ...prev, ip: e.target.value }))} placeholder="如: 192.168.1.100" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item label="端口">
              <Input value={urlParams.port} onChange={e => setUrlParams(prev => ({ ...prev, port: e.target.value }))} placeholder={`默认: ${editingTemplate?.defaultPort || 554}`} />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item label="用户名">
              <Input value={urlParams.username} onChange={e => setUrlParams(prev => ({ ...prev, username: e.target.value }))} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item label="密码">
              <Input.Password value={urlParams.password} onChange={e => setUrlParams(prev => ({ ...prev, password: e.target.value }))} />
            </Form.Item>
          </Col>
        </Row>
        <Form.Item label="通道">
          <Input value={urlParams.channel} onChange={e => setUrlParams(prev => ({ ...prev, channel: e.target.value }))} placeholder="默认: 1" />
        </Form.Item>
        {generatedUrl && (
          <Card title="生成的URL" size="small" style={{ background: '#f6ffed', borderColor: '#b7eb8f' }}>
            <div style={{ fontFamily: 'monospace', wordBreak: 'break-all', fontSize: 13 }}>{generatedUrl}</div>
          </Card>
        )}
      </Modal>
    </div>
  );
};

export default ConfigTemplateManagement;
