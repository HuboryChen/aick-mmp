/**
 * 视频墙预设选择器组件
 * 
 * 提供预设列表展示、选择、创建、编辑、删除和拖拽排序功能
 * 使用 @dnd-kit 实现拖拽排序
 */

import React, { useState } from 'react';
import { 
  Card, 
  List, 
  Button, 
  Space, 
  Typography, 
  Tag, 
  Popconfirm,
  Modal,
  Form,
  Input,
  Radio,
  Slider,
  message,
  Alert,
} from 'antd';
import { 
  StarOutlined, 
  StarFilled,
  PlusOutlined, 
  EditOutlined, 
  DeleteOutlined,
  DragOutlined,
  CheckOutlined,
  LayoutOutlined,
  VideoCameraOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';

const { Text, Title } = Typography;
const { TextArea } = Input;

/**
 * 可排序的预设项组件
 */
const SortablePresetItem = ({ 
  preset, 
  isActive,
  canEdit,
  canDelete,
  onSelect,
  onEdit,
  onDelete,
  onSetDefault,
}) => {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: preset.id, disabled: preset.isBuiltIn });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
    cursor: preset.isBuiltIn ? 'pointer' : 'grab',
  };

  // 获取画质标签颜色
  const getQualityColor = (quality) => {
    switch (quality) {
      case '480p': return 'orange';
      case '720p': return 'blue';
      case '1080p': return 'green';
      default: return 'default';
    }
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`preset-item ${isActive ? 'preset-item-active' : ''} ${preset.isBuiltIn ? 'preset-item-builtin' : ''}`}
    >
      {/* 拖拽手柄 - 仅用户预设显示 */}
      {!preset.isBuiltIn && (
        <div 
          className="preset-item-drag-handle"
          {...attributes}
          {...listeners}
          style={{ cursor: 'grab', padding: '0 8px' }}
        >
          <DragOutlined />
        </div>
      )}

      {/* 内置预设图标 */}
      {preset.isBuiltIn && (
        <div style={{ padding: '0 8px', color: '#faad14' }}>
          <StarFilled />
        </div>
      )}

      {/* 预设信息 */}
      <div 
        className="preset-item-content"
        style={{ flex: 1, cursor: 'pointer' }}
        onClick={() => onSelect?.(preset)}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Text strong={isActive}>{preset.presetName || preset.name}</Text>
          {preset.isDefault && (
            <Tag color="gold" style={{ margin: 0 }}>默认</Tag>
          )}
        </div>
        
        <div style={{ display: 'flex', gap: '12px', marginTop: '4px' }}>
          <span style={{ fontSize: '12px', color: '#666' }}>
            <LayoutOutlined /> {preset.layout}宫格
          </span>
          <Tag color={getQualityColor(preset.quality)} style={{ margin: 0 }}>
            {preset.quality}
          </Tag>
          <span style={{ fontSize: '12px', color: '#666' }}>
            <ThunderboltOutlined /> {preset.bitrate} kbps
          </span>
        </div>
      </div>

      {/* 操作按钮 */}
      <div className="preset-item-actions" style={{ display: 'flex', gap: '4px' }}>
        {/* 设为默认 */}
        {!preset.isBuiltIn && !preset.isDefault && (
          <Button
            type="text"
            size="small"
            icon={<StarOutlined />}
            onClick={(e) => {
              e.stopPropagation();
              onSetDefault?.(preset);
            }}
            title="设为默认"
          />
        )}

        {/* 编辑 */}
        {canEdit && (
          <Button
            type="text"
            size="small"
            icon={<EditOutlined />}
            onClick={(e) => {
              e.stopPropagation();
              onEdit?.(preset);
            }}
            title="编辑"
          />
        )}

        {/* 删除 */}
        {canDelete && (
          <Popconfirm
            title="确认删除"
            description={`确定要删除预设"${preset.presetName || preset.name}"吗？`}
            onConfirm={(e) => {
              e.stopPropagation();
              onDelete?.(preset);
            }}
            okText="删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
          >
            <Button
              type="text"
              size="small"
              danger
              icon={<DeleteOutlined />}
              onClick={(e) => e.stopPropagation()}
              title="删除"
            />
          </Popconfirm>
        )}

        {/* 选中状态指示 */}
        {isActive && (
          <CheckOutlined style={{ color: '#52c41a', marginLeft: '8px' }} />
        )}
      </div>
    </div>
  );
};

/**
 * 预设选择器组件
 */
const PresetSelector = ({
  presets = [],
  builtInPresets = [],
  activePresetId,
  isBuiltInPreset,
  canEditPreset,
  canDeletePreset,
  onSelect,
  onCreate,
  onEdit,
  onDelete,
  onSetDefault,
  onReorder,
  loading = false,
}) => {
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingPreset, setEditingPreset] = useState(null);
  const [form] = Form.useForm();
  const [editForm] = Form.useForm();

  // DnD sensors
  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  // 处理拖拽结束
  const handleDragEnd = (event) => {
    const { active, over } = event;

    if (active.id !== over?.id) {
      const oldIndex = presets.findIndex(p => p.id === active.id);
      const newIndex = presets.findIndex(p => p.id === over?.id);

      if (oldIndex !== -1 && newIndex !== -1) {
        const newOrder = arrayMove(presets, oldIndex, newIndex);
        onReorder?.(newOrder);
      }
    }
  };

  // 打开创建弹窗
  const handleOpenCreate = () => {
    form.resetFields();
    setIsCreateModalOpen(true);
  };

  // 创建预设
  const handleCreatePreset = async () => {
    try {
      const values = await form.validateFields();
      onCreate?.(values.name, values);
      setIsCreateModalOpen(false);
      message.success('预设创建成功');
    } catch (error) {
      console.error('Create preset failed:', error);
    }
  };

  // 打开编辑弹窗
  const handleOpenEdit = (preset) => {
    setEditingPreset(preset);
    editForm.setFieldsValue({
      name: preset.presetName || preset.name,
      layout: preset.layout,
      quality: preset.quality,
      bitrate: preset.bitrate,
    });
    setIsEditModalOpen(true);
  };

  // 更新预设
  const handleUpdatePreset = async () => {
    try {
      const values = await editForm.validateFields();
      onEdit?.(editingPreset.id, values);
      setIsEditModalOpen(false);
      setEditingPreset(null);
      message.success('预设更新成功');
    } catch (error) {
      console.error('Update preset failed:', error);
    }
  };

  // 合并所有预设 (用户预设 + 内置预设)
  const allPresets = [...presets, ...builtInPresets];

  // 用户预设和内置预设分组
  const userPresets = presets;
  const builtIn = builtInPresets;

  return (
    <div className="preset-selector">
      {/* 标题 */}
      <div className="preset-selector-header" style={{ marginBottom: '12px' }}>
        <Space>
          <StarOutlined />
          <span>预设管理</span>
        </Space>
        <Button
          type="primary"
          size="small"
          icon={<PlusOutlined />}
          onClick={handleOpenCreate}
        >
          新建预设
        </Button>
      </div>

      {/* 用户预设列表 (可拖拽) */}
      {userPresets.length > 0 && (
        <div className="preset-section" style={{ marginBottom: '16px' }}>
          <Text type="secondary" style={{ fontSize: '12px', marginBottom: '8px', display: 'block' }}>
            我的预设
          </Text>
          <DndContext
            sensors={sensors}
            collisionDetection={closestCenter}
            onDragEnd={handleDragEnd}
          >
            <SortableContext
              items={userPresets.map(p => p.id)}
              strategy={verticalListSortingStrategy}
            >
              <div className="preset-list">
                {userPresets.map(preset => (
                  <SortablePresetItem
                    key={preset.id}
                    preset={preset}
                    isActive={activePresetId === preset.id}
                    canEdit={true}
                    canDelete={true}
                    onSelect={onSelect}
                    onEdit={handleOpenEdit}
                    onDelete={onDelete}
                    onSetDefault={onSetDefault}
                  />
                ))}
              </div>
            </SortableContext>
          </DndContext>
        </div>
      )}

      {/* 内置预设 (固定顺序) */}
      {builtIn.length > 0 && (
        <div className="preset-section">
          <Text type="secondary" style={{ fontSize: '12px', marginBottom: '8px', display: 'block' }}>
            系统预设
          </Text>
          <div className="preset-list">
            {builtIn.map(preset => (
              <SortablePresetItem
                key={preset.id}
                preset={preset}
                isActive={activePresetId === preset.id}
                canEdit={false}
                canDelete={false}
                onSelect={onSelect}
                onEdit={handleOpenEdit}
                onDelete={onDelete}
                onSetDefault={onSetDefault}
              />
            ))}
          </div>
        </div>
      )}

      {/* 空状态 */}
      {allPresets.length === 0 && !loading && (
        <Alert
          message="暂无预设"
          description="点击上方按钮创建您的第一个预设"
          type="info"
          showIcon
        />
      )}

      {/* 创建预设弹窗 */}
      <Modal
        title="新建预设"
        open={isCreateModalOpen}
        onOk={handleCreatePreset}
        onCancel={() => setIsCreateModalOpen(false)}
        okText="创建"
        cancelText="取消"
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            name: '',
            layout: '4',
            quality: '720p',
            bitrate: 2048,
          }}
        >
          <Form.Item
            name="name"
            label="预设名称"
            rules={[{ required: true, message: '请输入预设名称' }]}
          >
            <Input placeholder="请输入预设名称" maxLength={50} showCount />
          </Form.Item>
        </Form>
      </Modal>

      {/* 编辑预设弹窗 */}
      <Modal
        title="编辑预设"
        open={isEditModalOpen}
        onOk={handleUpdatePreset}
        onCancel={() => {
          setIsEditModalOpen(false);
          setEditingPreset(null);
        }}
        okText="保存"
        cancelText="取消"
        width={400}
      >
        <Form
          form={editForm}
          layout="vertical"
        >
          <Form.Item
            name="name"
            label="预设名称"
            rules={[{ required: true, message: '请输入预设名称' }]}
          >
            <Input placeholder="请输入预设名称" maxLength={50} showCount />
          </Form.Item>
          
          <Form.Item
            name="layout"
            label="布局"
            rules={[{ required: true, message: '请选择布局' }]}
          >
            <Radio.Group buttonStyle="solid">
              <Radio.Button value="1">1宫格</Radio.Button>
              <Radio.Button value="4">4宫格</Radio.Button>
              <Radio.Button value="9">9宫格</Radio.Button>
              <Radio.Button value="16">16宫格</Radio.Button>
            </Radio.Group>
          </Form.Item>
          
          <Form.Item
            name="quality"
            label="画质"
            rules={[{ required: true, message: '请选择画质' }]}
          >
            <Radio.Group buttonStyle="solid">
              <Radio.Button value="480p">480p</Radio.Button>
              <Radio.Button value="720p">720p</Radio.Button>
              <Radio.Button value="1080p">1080p</Radio.Button>
            </Radio.Group>
          </Form.Item>
          
          <Form.Item
            name="bitrate"
            label={`码率: ${editForm.getFieldValue('bitrate') || 2048} kbps`}
            tooltip="调整视频码率，影响视频清晰度和带宽"
          >
            <Slider
              min={512}
              max={8192}
              step={128}
              marks={{
                512: '512',
                2048: '2048',
                4096: '4096',
                8192: '8192',
              }}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default PresetSelector;
