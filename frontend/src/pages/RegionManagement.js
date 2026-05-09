import React, { useState, useEffect, useCallback } from 'react';
import { Table, Button, Space, Modal, Form, Input, Select, message, Popconfirm, Card, Row, Col, Statistic, Tree, Divider, Tabs, Tag, Empty, Tooltip } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, EnvironmentOutlined, ApartmentOutlined, VideoCameraOutlined, ReloadOutlined, SwapOutlined, SearchOutlined, FolderOutlined, TeamOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { regionApi, cameraApi } from '../utils/api';
import PageContainer from '../components/PageContainer';
import '../styles/RegionManagement.css';

const { Option } = Select;
const { DirectoryTree } = Tree;
const { confirm } = Modal;
const { TabPane } = Tabs;

const RegionManagement = () => {
  const [regionTree, setRegionTree] = useState([]);
  const [regionFlat, setRegionFlat] = useState([]);
  const [selectedRegion, setSelectedRegion] = useState(null);
  const [regionStats, setRegionStats] = useState(null);
  const [regionCameras, setRegionCameras] = useState([]);
  const [loading, setLoading] = useState(false);
  const [statsLoading, setStatsLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [moveModalVisible, setMoveModalVisible] = useState(false);
  const [editingRegion, setEditingRegion] = useState(null);
  const [form] = Form.useForm();
  const [moveForm] = Form.useForm();
  const [expandedKeys, setExpandedKeys] = useState([]);
  const [activeTab, setActiveTab] = useState('tree');

  const fetchRegionTree = useCallback(async () => {
    setLoading(true);
    try {
      const response = await regionApi.getRegionTree();
      setRegionTree(response.data || []);
    } catch (error) {
      console.error('获取区域树失败:', error);
      message.error('获取区域树失败');
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchRegionFlat = useCallback(async () => {
    try {
      const response = await regionApi.getRegionsFlat();
      setRegionFlat(response.data || []);
    } catch (error) {
      console.error('获取区域列表失败:', error);
    }
  }, []);

  const fetchRegionStats = useCallback(async (regionId) => {
    if (!regionId) return;
    setStatsLoading(true);
    try {
      const response = await regionApi.getRegionStats(regionId);
      setRegionStats(response.data);
    } catch (error) {
      console.error('获取区域统计失败:', error);
      setRegionStats(null);
    } finally {
      setStatsLoading(false);
    }
  }, []);

  const fetchRegionCameras = useCallback(async (regionId, recursive = false) => {
    if (!regionId) return;
    try {
      const response = await regionApi.getRegionCameras(regionId, recursive);
      setRegionCameras(response.data || []);
    } catch (error) {
      console.error('获取区域摄像头失败:', error);
      setRegionCameras([]);
    }
  }, []);

  useEffect(() => {
    fetchRegionTree();
    fetchRegionFlat();
  }, [fetchRegionTree, fetchRegionFlat]);

  useEffect(() => {
    if (selectedRegion) {
      fetchRegionStats(selectedRegion.key);
      fetchRegionCameras(selectedRegion.key, false);
    }
  }, [selectedRegion, fetchRegionStats, fetchRegionCameras]);

  const handleSelectRegion = (selectedKeys, info) => {
    if (selectedKeys.length > 0) {
      const region = findRegionByKey(regionTree, selectedKeys[0]);
      setSelectedRegion({ key: selectedKeys[0], ...region });
    } else {
      setSelectedRegion(null);
    }
  };

  const findRegionByKey = (regions, key) => {
    for (const region of regions) {
      if (region.id.toString() === key) return region;
      if (region.children) {
        const found = findRegionByKey(region.children, key);
        if (found) return found;
      }
    }
    return null;
  };

  const handleExpand = (keys) => {
    setExpandedKeys(keys);
  };

  const handleAdd = () => {
    setEditingRegion(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = () => {
    if (!selectedRegion) {
      message.warning('请先选择一个区域');
      return;
    }
    setEditingRegion(selectedRegion);
    form.setFieldsValue({
      code: selectedRegion.code,
      name: selectedRegion.name,
      description: selectedRegion.description,
      parentId: selectedRegion.parentId || undefined,
      sortOrder: selectedRegion.sortOrder || 0,
    });
    setModalVisible(true);
  };

  const handleDelete = async () => {
    if (!selectedRegion) {
      message.warning('请先选择一个区域');
      return;
    }
    
    confirm({
      title: '确认删除区域',
      icon: <ExclamationCircleOutlined />,
      content: (
        <div>
          <p>确定要删除区域 "{selectedRegion.name}" 吗？</p>
          {(regionStats?.childRegions > 0 || regionStats?.totalCameras > 0) && (
            <div style={{ marginTop: 16, padding: 12, background: '#fffbe6', borderRadius: 4 }}>
              <p style={{ color: '#faad14', margin: 0 }}>
                <ExclamationCircleOutlined /> 该区域包含：
              </p>
              <ul style={{ margin: '8px 0 0 0', paddingLeft: 20 }}>
                {regionStats?.childRegions > 0 && <li>{regionStats.childRegions} 个子区域</li>}
                {regionStats?.totalCameras > 0 && <li>{regionStats.totalCameras} 个摄像头</li>}
              </ul>
              <p style={{ margin: '8px 0 0 0' }}>带 force 参数删除将同时删除子区域和重新分配摄像头。</p>
            </div>
          )}
        </div>
      ),
      okText: '删除（不包含子区域/摄像头）',
      okType: 'danger',
      cancelText: regionStats?.childRegions > 0 || regionStats?.totalCameras > 0 ? '强制删除' : '取消',
      onOk: async () => {
        try {
          await regionApi.deleteRegion(selectedRegion.key, false);
          message.success('区域删除成功');
          setSelectedRegion(null);
          fetchRegionTree();
          fetchRegionFlat();
        } catch (error) {
          message.error('删除失败: ' + (error.response?.data?.message || error.message));
        }
      },
      onCancel: () => {
        confirm({
          title: '确认强制删除',
          icon: <ExclamationCircleOutlined />,
          content: `确定要强制删除区域 "${selectedRegion.name}" 吗？所有子区域将被删除，摄像头将被重新分配。`,
          okText: '确认强制删除',
          okType: 'danger',
          onOk: async () => {
            try {
              await regionApi.deleteRegion(selectedRegion.key, true);
              message.success('区域强制删除成功');
              setSelectedRegion(null);
              fetchRegionTree();
              fetchRegionFlat();
            } catch (error) {
              message.error('强制删除失败: ' + (error.response?.data?.message || error.message));
            }
          },
        });
      },
    });
  };

  const handleSubmit = async (values) => {
    try {
      if (editingRegion) {
        await regionApi.updateRegion(editingRegion.key, values);
        message.success('区域信息更新成功');
      } else {
        await regionApi.createRegion(values);
        message.success('区域添加成功');
      }
      setModalVisible(false);
      fetchRegionTree();
      fetchRegionFlat();
    } catch (error) {
      message.error('保存失败: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleMove = () => {
    if (!selectedRegion) {
      message.warning('请先选择一个区域');
      return;
    }
    moveForm.resetFields();
    moveForm.setFieldsValue({
      newParentId: selectedRegion.parentId || undefined,
    });
    setMoveModalVisible(true);
  };

  const handleMoveSubmit = async (values) => {
    try {
      await regionApi.moveRegion(selectedRegion.key, { newParentId: values.newParentId || null });
      message.success('区域移动成功');
      setMoveModalVisible(false);
      fetchRegionTree();
      fetchRegionFlat();
    } catch (error) {
      message.error('移动失败: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleRefresh = () => {
    fetchRegionTree();
    fetchRegionFlat();
    if (selectedRegion) {
      fetchRegionStats(selectedRegion.key);
      fetchRegionCameras(selectedRegion.key, false);
    }
  };

  const getTreeData = () => {
    const convertToTreeData = (regions) => {
      return regions.map(region => ({
        key: region.id.toString(),
        title: (
          <span className="region-tree-title">
            <FolderOutlined style={{ marginRight: 8, color: '#1890ff' }} />
            {region.name}
            <span className="region-camera-count">
              <VideoCameraOutlined /> {region.cameraCount || 0}
            </span>
          </span>
        ),
        ...region,
        children: region.children ? convertToTreeData(region.children) : undefined,
      }));
    };
    return convertToTreeData(regionTree);
  };

  const cameraColumns = [
    {
      title: '摄像头名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '位置',
      dataIndex: 'location',
      key: 'location',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => (
        <Tag color={status === 'ONLINE' ? 'green' : status === 'OFFLINE' ? 'red' : 'orange'}>
          {status === 'ONLINE' ? '在线' : status === 'OFFLINE' ? '离线' : status || '未知'}
        </Tag>
      ),
    },
    {
      title: '边缘节点',
      dataIndex: 'edgeNodeName',
      key: 'edgeNodeName',
      render: (text) => text || '未分配',
    },
  ];

  const flatColumns = [
    {
      title: '区域编码',
      dataIndex: 'code',
      key: 'code',
    },
    {
      title: '区域名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '层级',
      dataIndex: 'level',
      key: 'level',
      render: (level) => `第 ${level} 级`,
    },
    {
      title: '摄像头数',
      dataIndex: 'cameraCount',
      key: 'cameraCount',
    },
    {
      title: '子区域数',
      dataIndex: 'childRegionCount',
      key: 'childRegionCount',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" onClick={() => {
            setSelectedRegion({ key: record.id.toString(), ...record });
          }}>
            查看
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <PageContainer
      title="区域管理"
      icon={<EnvironmentOutlined />}
      actions={
        <Space>
          <Button icon={<ReloadOutlined />} onClick={handleRefresh}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            添加区域
          </Button>
        </Space>
      }
    >
      <Row gutter={16}>
        {/* 左侧：区域树 */}
        <Col span={selectedRegion ? 10 : 24}>
          <Card 
            title="区域结构" 
            extra={
              <Space>
                <Input
                  placeholder="搜索区域"
                  prefix={<SearchOutlined />}
                  style={{ width: 200 }}
                  onChange={(e) => {
                    const keyword = e.target.value;
                    if (keyword) {
                      regionApi.searchRegions(keyword).then(res => {
                        const keys = res.data?.map(r => r.id.toString()) || [];
                        setExpandedKeys(keys);
                      });
                    }
                  }}
                  allowClear
                />
              </Space>
            }
          >
            <Tabs activeKey={activeTab} onChange={setActiveTab}>
              <TabPane tab="树形视图" key="tree">
                {regionTree.length > 0 ? (
                  <DirectoryTree
                    treeData={getTreeData()}
                    expandedKeys={expandedKeys}
                    onExpand={handleExpand}
                    onSelect={handleSelectRegion}
                    selectedKeys={selectedRegion ? [selectedRegion.key] : []}
                    loading={loading}
                    showIcon
                    blockNode
                  />
                ) : (
                  <Empty description="暂无区域数据，点击「添加区域」创建第一个区域" />
                )}
              </TabPane>
              <TabPane tab="列表视图" key="list">
                <Table
                  columns={flatColumns}
                  dataSource={regionFlat}
                  rowKey="id"
                  loading={loading}
                  size="small"
                  pagination={{ pageSize: 10 }}
                  onRow={(record) => ({
                    onClick: () => setSelectedRegion({ key: record.id.toString(), ...record }),
                    style: { cursor: 'pointer' },
                  })}
                />
              </TabPane>
            </Tabs>
          </Card>
        </Col>

        {/* 右侧：区域详情 */}
        {selectedRegion && (
          <Col span={14}>
            <Card 
              title={
                <span>
                  <ApartmentOutlined /> {selectedRegion.name}
                </span>
              }
              extra={
                <Space>
                  <Button icon={<SwapOutlined />} onClick={handleMove} size="small">
                    移动
                  </Button>
                  <Button icon={<EditOutlined />} onClick={handleEdit} size="small">
                    编辑
                  </Button>
                  <Popconfirm
                    title="确认删除"
                    description="确定要删除这个区域吗？"
                    onConfirm={handleDelete}
                    okText="确认"
                    cancelText="取消"
                    okButtonProps={{ danger: true }}
                  >
                    <Button icon={<DeleteOutlined />} danger size="small">
                      删除
                    </Button>
                  </Popconfirm>
                </Space>
              }
            >
              <Tabs defaultActiveKey="stats">
                <TabPane tab="区域统计" key="stats">
                  <Row gutter={16}>
                    <Col span={6}>
                      <Statistic
                        title="总摄像头"
                        value={regionStats?.totalCameras || 0}
                        prefix={<VideoCameraOutlined />}
                        loading={statsLoading}
                      />
                    </Col>
                    <Col span={6}>
                      <Statistic
                        title="在线摄像头"
                        value={regionStats?.onlineCameras || 0}
                        valueStyle={{ color: '#3f8600' }}
                        prefix={<TeamOutlined />}
                        loading={statsLoading}
                      />
                    </Col>
                    <Col span={6}>
                      <Statistic
                        title="离线摄像头"
                        value={regionStats?.offlineCameras || 0}
                        valueStyle={{ color: '#cf1322' }}
                        loading={statsLoading}
                      />
                    </Col>
                    <Col span={6}>
                      <Statistic
                        title="子区域数"
                        value={regionStats?.childRegions || 0}
                        loading={statsLoading}
                      />
                    </Col>
                  </Row>
                  <Divider />
                  <Row gutter={16}>
                    <Col span={12}>
                      <div className="stat-item">
                        <span className="stat-label">区域编码：</span>
                        <span className="stat-value">{selectedRegion.code}</span>
                      </div>
                    </Col>
                    <Col span={12}>
                      <div className="stat-item">
                        <span className="stat-label">区域层级：</span>
                        <span className="stat-value">第 {selectedRegion.level || 1} 级</span>
                      </div>
                    </Col>
                    <Col span={24}>
                      <div className="stat-item">
                        <span className="stat-label">区域路径：</span>
                        <span className="stat-value">{selectedRegion.path || '/'}</span>
                      </div>
                    </Col>
                    {selectedRegion.description && (
                      <Col span={24}>
                        <div className="stat-item">
                          <span className="stat-label">区域描述：</span>
                          <span className="stat-value">{selectedRegion.description}</span>
                        </div>
                      </Col>
                    )}
                  </Row>
                </TabPane>
                <TabPane tab={`摄像头列表 (${regionCameras.length})`} key="cameras">
                  <div style={{ marginBottom: 8 }}>
                    <Button 
                      size="small" 
                      onClick={() => fetchRegionCameras(selectedRegion.key, false)}
                    >
                      直接子区域
                    </Button>
                    <Button 
                      size="small" 
                      style={{ marginLeft: 8 }}
                      onClick={() => fetchRegionCameras(selectedRegion.key, true)}
                    >
                      包含所有子区域
                    </Button>
                  </div>
                  <Table
                    columns={cameraColumns}
                    dataSource={regionCameras}
                    rowKey="id"
                    size="small"
                    pagination={{ pageSize: 5 }}
                    locale={{
                      emptyText: <Empty description="该区域暂无摄像头" />
                    }}
                  />
                </TabPane>
              </Tabs>
            </Card>
          </Col>
        )}
      </Row>

      {/* 添加/编辑区域弹窗 */}
      <Modal
        title={editingRegion ? '编辑区域' : '添加区域'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={() => form.submit()}
        width={500}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
        >
          <Form.Item
            name="code"
            label="区域编码"
            rules={[{ required: true, message: '请输入区域编码' }]}
          >
            <Input placeholder="例如: CN-NORTH" />
          </Form.Item>

          <Form.Item
            name="name"
            label="区域名称"
            rules={[{ required: true, message: '请输入区域名称' }]}
          >
            <Input placeholder="例如: 华北区域" />
          </Form.Item>

          <Form.Item
            name="description"
            label="区域描述"
          >
            <Input.TextArea placeholder="请输入区域描述" rows={3} />
          </Form.Item>

          <Form.Item
            name="parentId"
            label="父级区域"
            tooltip="不选择则为根级区域"
          >
            <Select placeholder="请选择父级区域（可选）" allowClear>
              {regionFlat
                .filter(region => !editingRegion || region.id !== parseInt(editingRegion.key))
                .map(region => (
                  <Option key={region.id} value={region.id}>
                    {'　'.repeat((region.level - 1) || 0)}{region.name} (第{region.level}级)
                  </Option>
                ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="sortOrder"
            label="排序顺序"
            tooltip="数字越小排序越靠前"
          >
            <Input type="number" placeholder="0" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 移动区域弹窗 */}
      <Modal
        title="移动区域"
        open={moveModalVisible}
        onCancel={() => setMoveModalVisible(false)}
        onOk={() => moveForm.submit()}
        width={500}
      >
        <Form
          form={moveForm}
          layout="vertical"
          onFinish={handleMoveSubmit}
        >
          <Form.Item
            label="当前区域"
          >
            <Input value={selectedRegion?.name} disabled />
          </Form.Item>

          <Form.Item
            name="newParentId"
            label="移动到"
            tooltip="不选择则为移动到根级"
          >
            <Select placeholder="请选择新的父级区域" allowClear>
              <Option value={null}>根级（无父级）</Option>
              {regionFlat
                .filter(region => region.id !== parseInt(selectedRegion?.key))
                .map(region => (
                  <Option key={region.id} value={region.id}>
                    {'　'.repeat((region.level - 1) || 0)}{region.name} (第{region.level}级)
                  </Option>
                ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default RegionManagement;
