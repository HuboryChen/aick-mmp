import React, { useState, useEffect } from 'react';
import { Table, Button, Space, Modal, Form, Input, Select, message, Card, Typography, Popconfirm } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { regionApi } from '../utils/api';

const { Title } = Typography;
const { Option } = Select;

const RegionManagement = () => {
  const [regions, setRegions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRegion, setEditingRegion] = useState(null);
  const [form] = Form.useForm();

  useEffect(() => {
    fetchRegions();
  }, []);

  const fetchRegions = async () => {
    setLoading(true);
    try {
      const response = await regionApi.getAllRegions();
      setRegions(response.data);
    } catch (error) {
      console.error('获取地区列表失败:', error);
      message.error('获取地区列表失败');
    } finally {
      setLoading(false);
    }
  };

  const handleAdd = () => {
    setEditingRegion(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (region) => {
    setEditingRegion(region);
    form.setFieldsValue(region);
    setModalVisible(true);
  };

  const handleDelete = async (id) => {
    try {
      await regionApi.deleteRegion(id);
      message.success('删除成功');
      fetchRegions();
    } catch (error) {
      console.error('删除地区失败:', error);
      message.error('删除地区失败');
    }
  };

  const handleSubmit = async (values) => {
    try {
      if (editingRegion) {
        // 编辑
        await regionApi.updateRegion(editingRegion.id, values);
        message.success('地区信息更新成功');
      } else {
        // 新增
        await regionApi.createRegion(values);
        message.success('地区添加成功');
      }
      setModalVisible(false);
      fetchRegions();
    } catch (error) {
      console.error('保存地区失败:', error);
      message.error('保存地区失败');
    }
  };

  const columns = [
    {
      title: '地区编码',
      dataIndex: 'code',
      key: 'code',
    },
    {
      title: '地区名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
    },
    {
      title: '父级地区',
      dataIndex: 'parentId',
      key: 'parentId',
      render: (parentId) => {
        if (!parentId) return '无';
        const parent = regions.find(r => r.id === parentId);
        return parent ? parent.name : '未知';
      }
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="middle">
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确认删除"
            description="确定要删除这个地区吗？"
            onConfirm={() => handleDelete(record.id)}
            okText="确认"
            cancelText="取消"
          >
            <Button
              type="link"
              danger
              icon={<DeleteOutlined />}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card>
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Title level={3} style={{ margin: 0 }}>地区管理</Title>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            添加地区
          </Button>
        </div>
        
        <Table
          columns={columns}
          dataSource={regions}
          loading={loading}
          rowKey="id"
        />
      </Card>

      <Modal
        title={editingRegion ? '编辑地区' : '添加地区'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={() => form.submit()}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
        >
          <Form.Item
            name="code"
            label="地区编码"
            rules={[{ required: true, message: '请输入地区编码' }]}
          >
            <Input placeholder="例如: REGION-A" />
          </Form.Item>

          <Form.Item
            name="name"
            label="地区名称"
            rules={[{ required: true, message: '请输入地区名称' }]}
          >
            <Input placeholder="例如: 华北地区" />
          </Form.Item>

          <Form.Item
            name="description"
            label="描述"
          >
            <Input.TextArea placeholder="请输入地区描述" rows={3} />
          </Form.Item>

          <Form.Item
            name="parentId"
            label="父级地区"
          >
            <Select placeholder="请选择父级地区" allowClear>
              {regions
                .filter(region => !editingRegion || region.id !== editingRegion.id) // 排除自身作为父级
                .map(region => (
                  <Option key={region.id} value={region.id}>{region.name}</Option>
                ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default RegionManagement;