import React, { useState, useEffect } from 'react';
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Popconfirm, Empty, DatePicker, Typography, Divider } from 'antd';
import { PlusOutlined, KeyOutlined, DeleteOutlined, SearchOutlined, ReloadOutlined, CopyOutlined, CheckCircleOutlined, StopOutlined, ClockCircleOutlined } from '@ant-design/icons';
import { apiKeyApi } from '../utils/api';
import PageContainer from '../components/PageContainer';
import dayjs from 'dayjs';

const { Option } = Select;
const { TextArea } = Input;
const { Text } = Typography;
const { RangePicker } = DatePicker;

const ApiKeyManagement = () => {
  const [apiKeys, setApiKeys] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [secretKeyModalVisible, setSecretKeyModalVisible] = useState(false);
  const [createdSecret, setCreatedSecret] = useState(null);
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [form] = Form.useForm();
  const [searchKeyword, setSearchKeyword] = useState('');
  const [filterStatus, setFilterStatus] = useState(undefined);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  useEffect(() => {
    fetchApiKeys();
  }, [pagination.current, pagination.pageSize, filterStatus]);

  const handleOpenModal = () => {
    form.resetFields();
    form.setFieldsValue({
      status: 'ENABLED',
    });
    setModalVisible(true);
  };

  const fetchApiKeys = async () => {
    setLoading(true);
    try {
      let params = {
        page: pagination.current - 1,
        size: pagination.pageSize,
      };

      if (searchKeyword) params.keyword = searchKeyword;
      if (filterStatus) params.status = filterStatus;

      const response = await apiKeyApi.listForUser(params);

      const content = response.data?.content || [];
      const totalElements = response.data?.totalElements || 0;
      const pageNumber = response.data?.number || 0;

      const processedKeys = Array.isArray(content) ? content.map(key => ({
        id: key.id,
        accessKey: key.accessKey,
        name: key.name || '未命名',
        type: key.type || 'USER',
        status: key.status || 'ENABLED',
        lastUsedAt: key.lastUsedAt,
        expiresAt: key.expiresAt,
        createdAt: key.createdAt,
        userId: key.userId,
      })) : [];

      setApiKeys(processedKeys);
      setPagination({
        ...pagination,
        total: totalElements,
        current: pageNumber + 1,
      });
    } catch (error) {
      console.error('获取API Key列表失败:', error);
      message.error('获取API Key列表失败: ' + (error.response?.data?.message || error.message || '未知错误'));
      setApiKeys([]);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    setPagination({ ...pagination, current: 1 });
    fetchApiKeys();
  };

  const handleReset = () => {
    setSearchKeyword('');
    setFilterStatus(undefined);
    setPagination({ ...pagination, current: 1 });
  };

  const handleTableChange = (pager) => {
    setPagination(pager);
    fetchApiKeys();
  };

  const rowSelection = {
    selectedRowKeys,
    onChange: setSelectedRowKeys,
  };

  const handleAdd = () => {
    handleOpenModal();
  };

  const handleDelete = async (id) => {
    try {
      await apiKeyApi.deleteUserKey(id);
      message.success('删除成功');
      fetchApiKeys();
    } catch (error) {
      console.error('删除API Key失败:', error);
      message.error('删除API Key失败: ' + (error.response?.data?.message || error.message || '未知错误'));
    }
  };

  const handleSubmit = async (values) => {
    try {
      const response = await apiKeyApi.createForUser(values);

      // 创建成功后，显示 Secret Key（仅此一次）
      if (response.data?.secretKey) {
        setCreatedSecret({
          accessKey: response.data.accessKey,
          secretKey: response.data.secretKey,
          name: response.data.name,
        });
        setSecretKeyModalVisible(true);
      }

      message.success('API Key 创建成功');
      setModalVisible(false);
      fetchApiKeys();
    } catch (error) {
      console.error('创建API Key失败:', error);
      message.error('创建API Key失败: ' + (error.response?.data?.message || error.message || '未知错误'));
    }
  };

  const handleStatusChange = async (id, status) => {
    try {
      await apiKeyApi.updateUserKeyStatus(id, status);
      message.success('状态更新成功');
      fetchApiKeys();
    } catch (error) {
      console.error('状态更新失败:', error);
      message.error('状态更新失败: ' + (error.response?.data?.message || error.message || '未知错误'));
    }
  };

  const handleCopyKey = (text, type) => {
    navigator.clipboard.writeText(text).then(() => {
      message.success(`${type} 已复制到剪贴板`);
    }).catch(() => {
      message.error('复制失败');
    });
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'ENABLED':
        return <CheckCircleOutlined style={{ color: 'var(--status-online)' }} />;
      case 'DISABLED':
        return <StopOutlined style={{ color: 'var(--status-offline)' }} />;
      case 'EXPIRED':
        return <ClockCircleOutlined style={{ color: 'var(--status-warning)' }} />;
      default:
        return null;
    }
  };

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '密钥名称',
      dataIndex: 'name',
      key: 'name',
      render: (text) => <strong style={{ color: 'var(--color-text-primary)' }}>{text}</strong>,
    },
    {
      title: 'Access Key',
      dataIndex: 'accessKey',
      key: 'accessKey',
      width: 280,
      render: (accessKey) => (
        <Space>
          <code style={{ 
            fontSize: '12px', 
            color: 'var(--color-accent)',
            fontFamily: 'var(--font-mono)',
            background: 'var(--color-bg-secondary)',
            padding: '2px 8px',
            borderRadius: '4px',
          }}>
            {accessKey}
          </code>
          <Button 
            type="link" 
            size="small" 
            icon={<CopyOutlined />}
            onClick={() => handleCopyKey(accessKey, 'Access Key')}
          />
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => {
        const statusMap = {
          ENABLED: { text: '启用', color: 'green' },
          DISABLED: { text: '禁用', color: 'default' },
          EXPIRED: { text: '已过期', color: 'orange' },
        };
        const { text, color } = statusMap[status] || { text: status, color: 'default' };
        return (
          <Tag icon={getStatusIcon(status)} color={color}>
            {text}
          </Tag>
        );
      },
    },
    {
      title: '过期时间',
      dataIndex: 'expiresAt',
      key: 'expiresAt',
      width: 180,
      render: (expiresAt) => {
        if (!expiresAt) return <span style={{ color: 'var(--color-text-muted)' }}>永不过期</span>;
        const isExpired = new Date(expiresAt) < new Date();
        return (
          <span style={{ color: isExpired ? 'var(--status-warning)' : 'var(--color-text-primary)' }}>
            {new Date(expiresAt).toLocaleString('zh-CN')}
          </span>
        );
      },
    },
    {
      title: '最后使用',
      dataIndex: 'lastUsedAt',
      key: 'lastUsedAt',
      width: 160,
      render: (lastUsedAt) => {
        if (!lastUsedAt) return <span style={{ color: 'var(--color-text-muted)' }}>从未使用</span>;
        const time = new Date(lastUsedAt);
        const now = new Date();
        const diffHours = Math.floor((now - time) / (1000 * 60 * 60));
        
        let displayText = time.toLocaleString('zh-CN');
        if (diffHours < 1) {
          displayText = '刚刚';
        } else if (diffHours < 24) {
          displayText = `${diffHours}小时前`;
        }
        
        return <span>{displayText}</span>;
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (time) => time ? new Date(time).toLocaleString('zh-CN') : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_, record) => (
        <Space size="small">
          {record.status === 'ENABLED' ? (
            <Button
              type="link"
              onClick={() => handleStatusChange(record.id, 'DISABLED')}
            >
              禁用
            </Button>
          ) : (
            <Button
              type="link"
              onClick={() => handleStatusChange(record.id, 'ENABLED')}
            >
              启用
            </Button>
          )}
          <Popconfirm
            title="确认删除"
            description="确定要删除这个API Key吗？此操作不可撤销。"
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
    <PageContainer
      title="API Key 管理"
      icon={<KeyOutlined />}
      actions={
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          创建密钥
        </Button>
      }
    >
      <div style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input
            placeholder="搜索密钥名称"
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            onPressEnter={handleSearch}
            style={{ width: 200 }}
            prefix={<SearchOutlined />}
          />
          <Select
            placeholder="状态"
            value={filterStatus}
            onChange={setFilterStatus}
            style={{ width: 120 }}
            allowClear
          >
            <Option value="ENABLED">启用</Option>
            <Option value="DISABLED">禁用</Option>
            <Option value="EXPIRED">已过期</Option>
          </Select>
          <Button type="primary" onClick={handleSearch}>
            搜索
          </Button>
          <Button onClick={handleReset}>
            重置
          </Button>
          <Button icon={<ReloadOutlined />} onClick={fetchApiKeys}>
            刷新
          </Button>
        </Space>
      </div>

      <Table
        columns={columns}
        dataSource={apiKeys}
        loading={loading}
        rowKey="id"
        pagination={pagination}
        onChange={handleTableChange}
        locale={{
          emptyText: (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="暂无用户密钥"
            />
          )
        }}
      />

      {/* 创建 API Key 的 Modal */}
      <Modal
        title="创建 API Key"
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
            label="密钥名称"
            rules={[{ required: true, message: '请输入密钥名称' }]}
          >
            <Input placeholder="例如: 开发环境密钥" />
          </Form.Item>

          <Form.Item
            name="status"
            label="状态"
            rules={[{ required: true, message: '请选择状态' }]}
          >
            <Select placeholder="请选择状态">
              <Option value="ENABLED">启用</Option>
              <Option value="DISABLED">禁用</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="expiresAt"
            label="过期时间"
          >
            <DatePicker 
              showTime 
              placeholder="留空表示永不过期" 
              style={{ width: '100%' }}
              disabledDate={(current) => current && current < dayjs().endOf('day')}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 显示 Secret Key 的 Modal（仅创建时显示一次） */}
      <Modal
        title="API Key 创建成功"
        open={secretKeyModalVisible}
        onCancel={() => setSecretKeyModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setSecretKeyModalVisible(false)}>
            关闭
          </Button>,
        ]}
        width={600}
      >
        <div style={{ marginBottom: 24 }}>
          <Text type="warning" strong>
            ⚠️ 请立即保存 Secret Key！此密钥仅显示一次，关闭后将无法再次查看。
          </Text>
        </div>

        {createdSecret && (
          <div>
            <div style={{ marginBottom: 16 }}>
              <Text strong>密钥名称：</Text>
              <Text>{createdSecret.name}</Text>
            </div>

            <div style={{ marginBottom: 16 }}>
              <Text strong>Access Key：</Text>
              <Space>
                <code style={{ 
                  fontSize: '13px', 
                  color: 'var(--color-accent)',
                  fontFamily: 'var(--font-mono)',
                  background: 'var(--color-bg-secondary)',
                  padding: '4px 8px',
                  borderRadius: '4px',
                }}>
                  {createdSecret.accessKey}
                </code>
                <Button 
                  size="small" 
                  icon={<CopyOutlined />}
                  onClick={() => handleCopyKey(createdSecret.accessKey, 'Access Key')}
                >
                  复制
                </Button>
              </Space>
            </div>

            <div>
              <Text strong>Secret Key：</Text>
              <Space direction="vertical" style={{ width: '100%' }}>
                <code style={{ 
                  fontSize: '13px', 
                  color: 'var(--status-warning)',
                  fontFamily: 'var(--font-mono)',
                  background: 'var(--color-bg-secondary)',
                  padding: '8px',
                  borderRadius: '4px',
                  display: 'block',
                  wordBreak: 'break-all',
                }}>
                  {createdSecret.secretKey}
                </code>
                <Button 
                  type="primary" 
                  icon={<CopyOutlined />}
                  onClick={() => handleCopyKey(createdSecret.secretKey, 'Secret Key')}
                >
                  复制 Secret Key
                </Button>
              </Space>
            </div>
          </div>
        )}
      </Modal>
    </PageContainer>
  );
};

export default ApiKeyManagement;
