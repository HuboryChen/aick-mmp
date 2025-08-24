import React, { useState, useEffect } from 'react';
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Card, Typography } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, PlayCircleOutlined, StopOutlined } from '@ant-design/icons';

const { Title } = Typography;
const { Option } = Select;

const CameraManagement = () => {
  const [cameras, setCameras] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingCamera, setEditingCamera] = useState(null);
  const [form] = Form.useForm();

  useEffect(() => {
    fetchCameras();
  }, []);

  const fetchCameras = async () => {
    setLoading(true);
    try {
      // 模拟数据
      setCameras([
        {
          id: 1,
          name: 'Camera-A-01',
          region: '地区A',
          url: 'rtsp://192.168.1.101:554/stream',
          status: 'online',
          edgeNode: 'Node-A-01',
          resolution: '1920x1080',
          protocol: 'RTSP'
        },
        {
          id: 2,
          name: 'Camera-A-02',
          region: '地区A',
          url: 'rtsp://192.168.1.102:554/stream',
          status: 'offline',
          edgeNode: 'Node-A-01',
          resolution: '1280x720',
          protocol: 'RTSP'
        },
        {
          id: 3,
          name: 'Camera-B-01',
          region: '地区B',
          url: 'rtsp://192.168.2.101:554/stream',
          status: 'online',
          edgeNode: 'Node-B-01',
          resolution: '1920x1080',
          protocol: 'ONVIF'
        }
      ]);
    } catch (error) {
      message.error('获取摄像头列表失败');
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    {
      title: '摄像头名称',
      dataIndex: 'name',
      key: 'name',
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
        <Tag color={status === 'online' ? 'green' : 'red'}>
          {status === 'online' ? '在线' : '离线'}
        </Tag>
      ),
    },
    {
      title: '边缘节点',
      dataIndex: 'edgeNode',
      key: 'edgeNode',
    },
    {
      title: '分辨率',
      dataIndex: 'resolution',
      key: 'resolution',
    },
    {
      title: '协议',
      dataIndex: 'protocol',
      key: 'protocol',
      render: (protocol) => <Tag>{protocol}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="middle">
          {record.status === 'online' ? (
            <Button
              type="link"
              icon={<StopOutlined />}
              onClick={() => handleStop(record.id)}
            >
              停止
            </Button>
          ) : (
            <Button
              type="link"
              icon={<PlayCircleOutlined />}
              onClick={() => handleStart(record.id)}
            >
              启动
            </Button>
          )}
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record.id)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  const handleAdd = () => {
    setEditingCamera(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (camera) => {
    setEditingCamera(camera);
    form.setFieldsValue(camera);
    setModalVisible(true);
  };

  const handleDelete = (id) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这个摄像头吗？',
      onOk: () => {
        setCameras(cameras.filter(camera => camera.id !== id));
        message.success('删除成功');
      },
    });
  };

  const handleStart = (id) => {
    // 实际应该调用API启动摄像头
    setCameras(cameras.map(camera => 
      camera.id === id ? { ...camera, status: 'online' } : camera
    ));
    message.success('摄像头启动成功');
  };

  const handleStop = (id) => {
    // 实际应该调用API停止摄像头
    setCameras(cameras.map(camera => 
      camera.id === id ? { ...camera, status: 'offline' } : camera
    ));
    message.success('摄像头已停止');
  };

  const handleSubmit = (values) => {
    if (editingCamera) {
      // 编辑
      setCameras(cameras.map(camera => 
        camera.id === editingCamera.id ? { ...camera, ...values } : camera
      ));
      message.success('摄像头信息更新成功');
    } else {
      // 新增
      const newCamera = {
        ...values,
        id: Date.now(),
        status: 'offline'
      };
      setCameras([...cameras, newCamera]);
      message.success('摄像头添加成功');
    }
    setModalVisible(false);
  };

  return (
    <div>
      <Card>
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Title level={3} style={{ margin: 0 }}>摄像头管理</Title>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            添加摄像头
          </Button>
        </div>
        
        <Table
          columns={columns}
          dataSource={cameras}
          loading={loading}
          rowKey="id"
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条记录`,
          }}
        />
      </Card>

      <Modal
        title={editingCamera ? '编辑摄像头' : '添加摄像头'}
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
            name="name"
            label="摄像头名称"
            rules={[{ required: true, message: '请输入摄像头名称' }]}
          >
            <Input placeholder="例如: Camera-A-01" />
          </Form.Item>

          <Form.Item
            name="region"
            label="所属地区"
            rules={[{ required: true, message: '请选择所属地区' }]}
          >
            <Select placeholder="请选择地区">
              <Option value="地区A">地区A</Option>
              <Option value="地区B">地区B</Option>
              <Option value="地区C">地区C</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="url"
            label="视频流地址"
            rules={[{ required: true, message: '请输入视频流地址' }]}
          >
            <Input placeholder="rtsp://192.168.1.101:554/stream" />
          </Form.Item>

          <Form.Item
            name="edgeNode"
            label="边缘节点"
            rules={[{ required: true, message: '请选择边缘节点' }]}
          >
            <Select placeholder="请选择边缘节点">
              <Option value="Node-A-01">Node-A-01</Option>
              <Option value="Node-A-02">Node-A-02</Option>
              <Option value="Node-B-01">Node-B-01</Option>
              <Option value="Node-B-02">Node-B-02</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="resolution"
            label="分辨率"
            rules={[{ required: true, message: '请选择分辨率' }]}
          >
            <Select placeholder="请选择分辨率">
              <Option value="640x480">640x480</Option>
              <Option value="1280x720">1280x720</Option>
              <Option value="1920x1080">1920x1080</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="protocol"
            label="协议类型"
            rules={[{ required: true, message: '请选择协议类型' }]}
          >
            <Select placeholder="请选择协议类型">
              <Option value="RTSP">RTSP</Option>
              <Option value="ONVIF">ONVIF</Option>
              <Option value="GB28181">GB28181</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default CameraManagement;