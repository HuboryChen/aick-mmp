import React, { useState, useEffect, useCallback } from 'react';
import { 
  Table, Button, Space, Modal, Form, Select, InputNumber, 
  Tag, Popconfirm, message, Card, Row, Col, Switch, Checkbox, 
  TimePicker, Input, Slider, Divider, Empty, Tooltip, Alert
} from 'antd';
import { 
  PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined, 
  ClockCircleOutlined, VideoCameraOutlined, CalendarOutlined,
  QuestionCircleOutlined, InfoCircleOutlined
} from '@ant-design/icons';
import { recordingScheduleApi, cameraApi } from '../utils/api';
import dayjs from 'dayjs';

// 录像类型选项
const scheduleTypeOptions = [
  { value: 'CONTINUOUS', label: '持续录像', color: 'blue', description: '24小时不间断录像' },
  { value: 'TIMED', label: '定时录像', color: 'green', description: '按设定时间段录像' },
  { value: 'MOTION', label: '移动侦测', color: 'orange', description: '检测到移动时录像' },
  { value: 'EVENT', label: '事件录像', color: 'purple', description: '特定事件触发录像' },
  { value: 'SMART', label: '智能录像', color: 'cyan', description: 'AI智能分析录像' }
];

// 星期选项
const weekDaysOptions = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 7, label: '周日' }
];

// 获取录像类型显示信息
const getScheduleTypeInfo = (type) => {
  return scheduleTypeOptions.find(opt => opt.value === type) || { label: type, color: 'default' };
};

// 获取星期显示文本
const getWeekDaysText = (days) => {
  if (!days || days.length === 0) return '未设置';
  if (days.length === 7) return '每天';
  const dayLabels = days.map(d => {
    const opt = weekDaysOptions.find(w => w.value === d);
    return opt ? opt.label : '';
  }).filter(Boolean);
  return dayLabels.join('、');
};

// 时间段格式化显示
const formatTimeSlots = (timeSlots) => {
  if (!timeSlots || timeSlots.length === 0) return '-';
  return timeSlots.map(slot => {
    const start = slot.startTime || '00:00';
    const end = slot.endTime || '23:59';
    return `${start} - ${end}`;
  }).join(', ');
};

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes) return '-';
  const gb = bytes / (1024 * 1024 * 1024);
  if (gb >= 1) return `${gb.toFixed(2)} GB`;
  const mb = bytes / (1024 * 1024);
  return `${mb.toFixed(2)} MB`;
};

const RecordingSchedulePage = () => {
  const [schedules, setSchedules] = useState([]);
  const [cameras, setCameras] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingSchedule, setEditingSchedule] = useState(null);
  const [form] = Form.useForm();
  const [timeSlots, setTimeSlots] = useState([{ startTime: '00:00', endTime: '23:59' }]);
  const [selectedWeekDays, setSelectedWeekDays] = useState([1, 2, 3, 4, 5, 6, 7]);
  const [selectedScheduleType, setSelectedScheduleType] = useState('TIMED');
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailData, setDetailData] = useState(null);

  // 加载录像计划列表
  const loadSchedules = useCallback(async () => {
    setLoading(true);
    try {
      const response = await recordingScheduleApi.getSchedules();
      setSchedules(response.data || []);
    } catch (error) {
      console.error('加载录像计划失败:', error);
      message.error('加载录像计划列表失败');
    } finally {
      setLoading(false);
    }
  }, []);

  // 加载摄像头列表
  const loadCameras = useCallback(async () => {
    try {
      const response = await cameraApi.search({ page: 0, size: 1000 });
      setCameras(response.data?.content || response.data || []);
    } catch (error) {
      console.error('加载摄像头列表失败:', error);
    }
  }, []);

  useEffect(() => {
    loadSchedules();
    loadCameras();
  }, [loadSchedules, loadCameras]);

  // 打开创建/编辑弹窗
  const handleOpenModal = (schedule = null) => {
    setEditingSchedule(schedule);
    if (schedule) {
      form.setFieldsValue({
        name: schedule.name,
        cameraId: schedule.cameraId,
        scheduleType: schedule.scheduleType,
        enabled: schedule.enabled,
        retentionDays: schedule.retentionDays,
        motionSensitivity: schedule.motionSensitivity || 50,
        description: schedule.description,
      });
      setSelectedScheduleType(schedule.scheduleType);
      setSelectedWeekDays(schedule.recordingDays || [1, 2, 3, 4, 5, 6, 7]);
      if (schedule.timeSlots && schedule.timeSlots.length > 0) {
        setTimeSlots(schedule.timeSlots.map(slot => ({
          startTime: slot.startTime,
          endTime: slot.endTime,
        })));
      } else {
        setTimeSlots([{ startTime: '00:00', endTime: '23:59' }]);
      }
    } else {
      form.resetFields();
      setSelectedScheduleType('TIMED');
      setSelectedWeekDays([1, 2, 3, 4, 5, 6, 7]);
      setTimeSlots([{ startTime: '00:00', endTime: '23:59' }]);
    }
    setModalVisible(true);
  };

  // 关闭弹窗
  const handleCloseModal = () => {
    setModalVisible(false);
    setEditingSchedule(null);
    form.resetFields();
    setTimeSlots([{ startTime: '00:00', endTime: '23:59' }]);
    setSelectedWeekDays([1, 2, 3, 4, 5, 6, 7]);
  };

  // 提交表单
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      
      const scheduleData = {
        name: values.name,
        cameraId: values.cameraId,
        scheduleType: values.scheduleType,
        enabled: values.enabled !== false,
        retentionDays: values.retentionDays || 30,
        motionSensitivity: values.motionSensitivity || 50,
        description: values.description,
        recordingDays: selectedWeekDays,
        timeSlots: timeSlots.map(slot => ({
          startTime: slot.startTime,
          endTime: slot.endTime,
        })),
      };

      if (editingSchedule) {
        await recordingScheduleApi.updateSchedule(editingSchedule.id, scheduleData);
        message.success('录像计划更新成功');
      } else {
        await recordingScheduleApi.createSchedule(scheduleData);
        message.success('录像计划创建成功');
      }
      
      handleCloseModal();
      loadSchedules();
    } catch (error) {
      console.error('提交失败:', error);
      if (error.errorFields) {
        return; // 表单验证错误
      }
      message.error(editingSchedule ? '更新失败' : '创建失败');
    }
  };

  // 删除录像计划
  const handleDelete = async (id) => {
    try {
      await recordingScheduleApi.deleteSchedule(id);
      message.success('删除成功');
      loadSchedules();
    } catch (error) {
      console.error('删除失败:', error);
      message.error('删除失败');
    }
  };

  // 启用/禁用录像计划
  const handleToggleEnabled = async (id, enabled) => {
    try {
      await recordingScheduleApi.enableSchedule(id, enabled);
      message.success(enabled ? '已启用' : '已禁用');
      loadSchedules();
    } catch (error) {
      console.error('切换状态失败:', error);
      message.error('操作失败');
    }
  };

  // 查看详情
  const handleViewDetail = async (schedule) => {
    try {
      const response = await recordingScheduleApi.getSchedule(schedule.id);
      setDetailData(response.data);
      setDetailVisible(true);
    } catch (error) {
      console.error('加载详情失败:', error);
      message.error('加载详情失败');
    }
  };

  // 添加时间段
  const handleAddTimeSlot = () => {
    setTimeSlots([...timeSlots, { startTime: '00:00', endTime: '23:59' }]);
  };

  // 移除时间段
  const handleRemoveTimeSlot = (index) => {
    if (timeSlots.length <= 1) {
      message.warning('至少需要保留一个时间段');
      return;
    }
    const newTimeSlots = timeSlots.filter((_, i) => i !== index);
    setTimeSlots(newTimeSlots);
  };

  // 更新时间段
  const handleTimeSlotChange = (index, field, value) => {
    const newTimeSlots = [...timeSlots];
    newTimeSlots[index] = {
      ...newTimeSlots[index],
      [field]: value,
    };
    setTimeSlots(newTimeSlots);
  };

  // 获取摄像头名称
  const getCameraName = (cameraId) => {
    const camera = cameras.find(c => c.id === cameraId);
    return camera ? camera.name : `摄像头 #${cameraId}`;
  };

  // 表格列定义
  const columns = [
    {
      title: '计划名称',
      dataIndex: 'name',
      key: 'name',
      width: 150,
      ellipsis: true,
    },
    {
      title: '关联摄像头',
      dataIndex: 'cameraId',
      key: 'cameraId',
      width: 120,
      render: (cameraId) => getCameraName(cameraId),
    },
    {
      title: '录像类型',
      dataIndex: 'scheduleType',
      key: 'scheduleType',
      width: 100,
      render: (type) => {
        const info = getScheduleTypeInfo(type);
        return <Tag color={info.color}>{info.label}</Tag>;
      },
    },
    {
      title: '录像日期',
      dataIndex: 'recordingDays',
      key: 'recordingDays',
      width: 120,
      render: (days) => getWeekDaysText(days),
    },
    {
      title: '时间段',
      dataIndex: 'timeSlots',
      key: 'timeSlots',
      width: 180,
      ellipsis: true,
      render: (timeSlots) => formatTimeSlots(timeSlots),
    },
    {
      title: '保留天数',
      dataIndex: 'retentionDays',
      key: 'retentionDays',
      width: 90,
      render: (days) => `${days || 30} 天`,
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 80,
      render: (enabled, record) => (
        <Switch
          checked={enabled}
          onChange={(checked) => handleToggleEnabled(record.id, checked)}
          checkedChildren="启用"
          unCheckedChildren="禁用"
          size="small"
        />
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="查看详情">
            <Button 
              type="text" 
              size="small" 
              icon={<InfoCircleOutlined />}
              onClick={() => handleViewDetail(record)}
            />
          </Tooltip>
          <Tooltip title="编辑">
            <Button 
              type="text" 
              size="small" 
              icon={<EditOutlined />}
              onClick={() => handleOpenModal(record)}
            />
          </Tooltip>
          <Popconfirm
            title="确定删除该录像计划？"
            description="删除后不可恢复"
            onConfirm={() => handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
            okButtonProps={{ danger: true }}
          >
            <Tooltip title="删除">
              <Button 
                type="text" 
                size="small" 
                danger
                icon={<DeleteOutlined />}
              />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Card
        title={
          <Space>
            <VideoCameraOutlined />
            <span>录像计划管理</span>
          </Space>
        }
        extra={
          <Space>
            <Button 
              icon={<ReloadOutlined />} 
              onClick={loadSchedules}
              loading={loading}
            >
              刷新
            </Button>
            <Button 
              type="primary" 
              icon={<PlusOutlined />}
              onClick={() => handleOpenModal()}
            >
              创建录像计划
            </Button>
          </Space>
        }
        style={{ minHeight: 'calc(100vh - 180px)' }}
      >
        {schedules.length === 0 && !loading ? (
          <Empty 
            description="暂无录像计划" 
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          >
            <Button type="primary" onClick={() => handleOpenModal()}>
              创建第一个录像计划
            </Button>
          </Empty>
        ) : (
          <Table
            columns={columns}
            dataSource={schedules}
            rowKey="id"
            loading={loading}
            pagination={{
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total) => `共 ${total} 条记录`,
            }}
            scroll={{ x: 1100 }}
          />
        )}
      </Card>

      {/* 创建/编辑弹窗 */}
      <Modal
        title={editingSchedule ? '编辑录像计划' : '创建录像计划'}
        open={modalVisible}
        onCancel={handleCloseModal}
        onOk={handleSubmit}
        width={700}
        okText={editingSchedule ? '保存' : '创建'}
        cancelText="取消"
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            enabled: true,
            retentionDays: 30,
            motionSensitivity: 50,
          }}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="name"
                label="计划名称"
                rules={[{ required: true, message: '请输入计划名称' }]}
              >
                <Input placeholder="例如：白天定时录像" maxLength={50} showCount />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="cameraId"
                label="关联摄像头"
                rules={[{ required: true, message: '请选择摄像头' }]}
              >
                <Select placeholder="请选择摄像头" showSearch filterOption={(input, option) =>
                  option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }>
                  {cameras.map(camera => (
                    <Option key={camera.id} value={camera.id}>{camera.name}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="scheduleType"
                label="录像类型"
                rules={[{ required: true, message: '请选择录像类型' }]}
              >
                <Select 
                  placeholder="请选择录像类型"
                  onChange={(value) => setSelectedScheduleType(value)}
                >
                  {scheduleTypeOptions.map(opt => (
                    <Option key={opt.value} value={opt.value}>
                      <Space>
                        <Tag color={opt.color}>{opt.label}</Tag>
                        <span style={{ color: '#888', fontSize: 12 }}>{opt.description}</span>
                      </Space>
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="enabled"
                label="启用状态"
                valuePropName="checked"
              >
                <Switch checkedChildren="启用" unCheckedChildren="禁用" />
              </Form.Item>
            </Col>
          </Row>

          {/* 移动侦测灵敏度 - 仅MOTION类型显示 */}
          {selectedScheduleType === 'MOTION' && (
            <Form.Item
              name="motionSensitivity"
              label={
                <Space>
                  移动侦测灵敏度
                  <Tooltip title="灵敏度越高，越容易触发录像，但可能产生更多误报">
                    <QuestionCircleOutlined style={{ color: '#888' }} />
                  </Tooltip>
                </Space>
              }
            >
              <Slider
                min={0}
                max={100}
                marks={{
                  0: '低',
                  50: '中',
                  100: '高',
                }}
              />
            </Form.Item>
          )}

          {/* 时间段配置 */}
          <Divider orientation="left">
            <Space>
              <ClockCircleOutlined />
              时间段配置
            </Space>
          </Divider>
          
          <div style={{ marginBottom: 16, color: '#888' }}>
            {selectedScheduleType === 'CONTINUOUS' 
              ? '持续录像将24小时不间断录像，以下时间段配置将被忽略'
              : '设置录像的具体时间段，支持添加多个时间段'}
          </div>

          {timeSlots.map((slot, index) => (
            <Row key={index} gutter={16} style={{ marginBottom: 12, alignItems: 'center' }}>
              <Col span={8}>
                <TimePicker
                  value={slot.startTime ? dayjs(slot.startTime, 'HH:mm') : null}
                  format="HH:mm"
                  placeholder="开始时间"
                  style={{ width: '100%' }}
                  onChange={(time, timeString) => handleTimeSlotChange(index, 'startTime', timeString)}
                />
              </Col>
              <Col span={2} style={{ textAlign: 'center' }}>
                至
              </Col>
              <Col span={8}>
                <TimePicker
                  value={slot.endTime ? dayjs(slot.endTime, 'HH:mm') : null}
                  format="HH:mm"
                  placeholder="结束时间"
                  style={{ width: '100%' }}
                  onChange={(time, timeString) => handleTimeSlotChange(index, 'endTime', timeString)}
                />
              </Col>
              <Col span={6}>
                {timeSlots.length > 1 && (
                  <Button 
                    type="text" 
                    danger
                    onClick={() => handleRemoveTimeSlot(index)}
                  >
                    删除
                  </Button>
                )}
              </Col>
            </Row>
          ))}

          {selectedScheduleType !== 'CONTINUOUS' && (
            <Button 
              type="dashed" 
              onClick={handleAddTimeSlot}
              icon={<PlusOutlined />}
              style={{ width: '100%', marginBottom: 16 }}
            >
              添加时间段
            </Button>
          )}

          {/* 录像日期选择 */}
          <Divider orientation="left">
            <Space>
              <CalendarOutlined />
              录像日期
            </Space>
          </Divider>
          
          <Checkbox.Group
            value={selectedWeekDays}
            onChange={(values) => setSelectedWeekDays(values)}
            style={{ width: '100%' }}
          >
            <Row gutter={[16, 8]}>
              {weekDaysOptions.map(day => (
                <Col key={day.value} span={3}>
                  <Checkbox value={day.value}>{day.label}</Checkbox>
                </Col>
              ))}
              <Col span={3}>
                <Checkbox 
                  onChange={(e) => {
                    if (e.target.checked) {
                      setSelectedWeekDays([1, 2, 3, 4, 5, 6, 7]);
                    } else {
                      setSelectedWeekDays([]);
                    }
                  }}
                  checked={selectedWeekDays.length === 7}
                  indeterminate={selectedWeekDays.length > 0 && selectedWeekDays.length < 7}
                >
                  每天
                </Checkbox>
              </Col>
            </Row>
          </Checkbox.Group>

          <Row gutter={16} style={{ marginTop: 16 }}>
            <Col span={12}>
              <Form.Item
                name="retentionDays"
                label="录像保留天数"
                tooltip="超过此天数的录像将被自动清理"
              >
                <InputNumber
                  min={1}
                  max={365}
                  style={{ width: '100%' }}
                  addonAfter="天"
                />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="description"
            label="备注说明"
          >
            <TextArea 
              rows={3} 
              placeholder="可选：添加计划说明或备注信息"
              maxLength={500}
              showCount
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 详情弹窗 */}
      <Modal
        title="录像计划详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailVisible(false)}>
            关闭
          </Button>,
          <Button 
            key="edit" 
            type="primary"
            onClick={() => {
              setDetailVisible(false);
              handleOpenModal(detailData);
            }}
          >
            编辑
          </Button>
        ]}
      >
        {detailData && (
          <Row gutter={[16, 16]}>
            <Col span={12}>
              <b>计划名称：</b>{detailData.name}
            </Col>
            <Col span={12}>
              <b>关联摄像头：</b>{getCameraName(detailData.cameraId)}
            </Col>
            <Col span={12}>
              <b>录像类型：</b>
              <Tag color={getScheduleTypeInfo(detailData.scheduleType).color}>
                {getScheduleTypeInfo(detailData.scheduleType).label}
              </Tag>
            </Col>
            <Col span={12}>
              <b>状态：</b>
              <Tag color={detailData.enabled ? 'green' : 'red'}>
                {detailData.enabled ? '已启用' : '已禁用'}
              </Tag>
            </Col>
            <Col span={12}>
              <b>录像日期：</b>{getWeekDaysText(detailData.recordingDays)}
            </Col>
            <Col span={12}>
              <b>录像保留天数：</b>{detailData.retentionDays || 30} 天
            </Col>
            <Col span={24}>
              <b>时间段：</b>
              {detailData.timeSlots && detailData.timeSlots.length > 0 ? (
                <ul style={{ margin: '8px 0', paddingLeft: 20 }}>
                  {detailData.timeSlots.map((slot, index) => (
                    <li key={index}>
                      {slot.startTime} - {slot.endTime}
                    </li>
                  ))}
                </ul>
              ) : '-'}
            </Col>
            {detailData.scheduleType === 'MOTION' && (
              <Col span={12}>
                <b>移动侦测灵敏度：</b>{detailData.motionSensitivity || 50}
              </Col>
            )}
            {detailData.description && (
              <Col span={24}>
                <b>备注：</b>{detailData.description}
              </Col>
            )}
            <Col span={12}>
              <b>创建时间：</b>
              {detailData.createdAt ? dayjs(detailData.createdAt).format('YYYY-MM-DD HH:mm:ss') : '-'}
            </Col>
            <Col span={12}>
              <b>更新时间：</b>
              {detailData.updatedAt ? dayjs(detailData.updatedAt).format('YYYY-MM-DD HH:mm:ss') : '-'}
            </Col>
          </Row>
        )}
      </Modal>
    </div>
  );
};

export default RecordingSchedulePage;
