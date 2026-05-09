import React, { useState, useEffect } from 'react';
import { Card, Button, Space, Select, InputNumber, Tag, Tooltip, Popconfirm, message, Empty } from 'antd';
import { PlusOutlined, DeleteOutlined, SwapOutlined, BranchesOutlined } from '@ant-design/icons';

const { Option } = Select;

/**
 * 告警条件编辑器组件
 * 支持 AND/OR 组合条件的可视化编辑
 */
const AlertConditionEditor = ({ conditions = [], onChange, disabled = false }) => {
  const [localConditions, setLocalConditions] = useState(conditions);

  useEffect(() => {
    setLocalConditions(conditions);
  }, [conditions]);

  const updateCondition = (index, field, value) => {
    const newConditions = [...localConditions];
    newConditions[index] = { ...newConditions[index], [field]: value };
    setLocalConditions(newConditions);
    onChange?.(newConditions);
  };

  const addCondition = () => {
    const newCondition = {
      id: `temp_${Date.now()}`,
      conditionName: '',
      conditionType: 'METRIC',
      metricName: 'cpu_usage',
      operator: 'GT',
      thresholdValue: 80,
      logicType: localConditions.length > 0 ? 'AND' : 'AND',
      sortOrder: localConditions.length + 1,
      isEnabled: true,
    };
    const newConditions = [...localConditions, newCondition];
    setLocalConditions(newConditions);
    onChange?.(newConditions);
  };

  const removeCondition = (index) => {
    const newConditions = localConditions.filter((_, i) => i !== index);
    // 更新 logicType
    if (newConditions.length > 1) {
      newConditions[0].logicType = 'AND';
    }
    setLocalConditions(newConditions);
    onChange?.(newConditions);
  };

  const toggleLogicType = (index) => {
    if (index === 0) return; // 第一个条件不能切换逻辑类型
    const newConditions = [...localConditions];
    const currentLogic = newConditions[index].logicType;
    newConditions[index] = { 
      ...newConditions[index], 
      logicType: currentLogic === 'AND' ? 'OR' : 'AND' 
    };
    setLocalConditions(newConditions);
    onChange?.(newConditions);
  };

  // 条件类型选项
  const conditionTypeOptions = [
    { value: 'METRIC', label: '指标' },
    { value: 'STATUS', label: '状态' },
    { value: 'TIME', label: '时间' },
    { value: 'EXPRESSION', label: '表达式' },
  ];

  // 指标名称选项
  const metricNameOptions = [
    { value: 'cpu_usage', label: 'CPU使用率', unit: '%' },
    { value: 'memory_usage', label: '内存使用率', unit: '%' },
    { value: 'disk_usage', label: '磁盘使用率', unit: '%' },
    { value: 'network_latency', label: '网络延迟', unit: 'ms' },
    { value: 'bitrate', label: '码率', unit: 'kbps' },
    { value: 'frame_rate', label: '帧率', unit: 'fps' },
    { value: 'packet_loss', label: '丢包率', unit: '%' },
  ];

  // 操作符选项
  const operatorOptions = [
    { value: 'GT', label: '大于', symbol: '>' },
    { value: 'GTE', label: '大于等于', symbol: '>=' },
    { value: 'LT', label: '小于', symbol: '<' },
    { value: 'LTE', label: '小于等于', symbol: '<=' },
    { value: 'EQ', label: '等于', symbol: '=' },
    { value: 'NEQ', label: '不等于', symbol: '!=' },
  ];

  // 状态选项
  const statusOptions = [
    { value: 'ONLINE', label: '在线' },
    { value: 'OFFLINE', label: '离线' },
    { value: 'ERROR', label: '错误' },
    { value: 'RECORDING', label: '录像中' },
  ];

  const getOperatorSymbol = (op) => {
    const option = operatorOptions.find(o => o.value === op);
    return option?.symbol || op;
  };

  const getMetricUnit = (metric) => {
    const option = metricNameOptions.find(o => o.value === metric);
    return option?.unit || '';
  };

  const renderCondition = (condition, index) => {
    const isFirst = index === 0;
    const isLast = index === localConditions.length - 1;

    return (
      <div key={condition.id || index} className="condition-item">
        {/* 逻辑连接符 */}
        {!isFirst && (
          <div className="condition-logic">
            <Tooltip title="点击切换逻辑关系">
              <Tag 
                color={condition.logicType === 'AND' ? 'blue' : 'orange'}
                onClick={() => !disabled && toggleLogicType(index)}
                style={{ cursor: disabled ? 'default' : 'pointer' }}
                icon={<BranchesOutlined />}
              >
                {condition.logicType === 'AND' ? '并且' : '或者'}
              </Tag>
            </Tooltip>
          </div>
        )}

        {/* 条件行 */}
        <Card 
          size="small" 
          className="condition-card"
          style={{ 
            borderColor: condition.logicType === 'OR' ? '#faad14' : '#1890ff',
            backgroundColor: condition.isEnabled === false ? '#f5f5f5' : '#fff'
          }}
        >
          <div className="condition-row">
            <Space size="small" wrap>
              {/* 条件名称 */}
              <InputNumber
                placeholder="条件名称"
                value={condition.conditionName ? parseInt(condition.conditionName) : null}
                onChange={(val) => updateCondition(index, 'conditionName', val?.toString())}
                disabled={disabled}
                style={{ width: 100 }}
                min={1}
              />
              
              {/* 条件类型 */}
              <Select
                placeholder="类型"
                value={condition.conditionType}
                onChange={(val) => updateCondition(index, 'conditionType', val)}
                disabled={disabled}
                style={{ width: 100 }}
              >
                {conditionTypeOptions.map(opt => (
                  <Option key={opt.value} value={opt.value}>{opt.label}</Option>
                ))}
              </Select>

              {/* 指标名称 */}
              {condition.conditionType === 'METRIC' && (
                <Select
                  placeholder="指标"
                  value={condition.metricName}
                  onChange={(val) => updateCondition(index, 'metricName', val)}
                  disabled={disabled}
                  style={{ width: 130 }}
                >
                  {metricNameOptions.map(opt => (
                    <Option key={opt.value} value={opt.value}>{opt.label}</Option>
                  ))}
                </Select>
              )}

              {/* 状态选择 */}
              {condition.conditionType === 'STATUS' && (
                <Select
                  placeholder="状态"
                  value={condition.stringValue}
                  onChange={(val) => updateCondition(index, 'stringValue', val)}
                  disabled={disabled}
                  style={{ width: 130 }}
                >
                  {statusOptions.map(opt => (
                    <Option key={opt.value} value={opt.value}>{opt.label}</Option>
                  ))}
                </Select>
              )}

              {/* 操作符 */}
              {condition.conditionType === 'METRIC' && (
                <Select
                  placeholder="操作"
                  value={condition.operator}
                  onChange={(val) => updateCondition(index, 'operator', val)}
                  disabled={disabled}
                  style={{ width: 100 }}
                >
                  {operatorOptions.map(opt => (
                    <Option key={opt.value} value={opt.value}>{opt.label} {opt.symbol}</Option>
                  ))}
                </Select>
              )}

              {/* 阈值 */}
              {condition.conditionType === 'METRIC' && (
                <InputNumber
                  placeholder="阈值"
                  value={condition.thresholdValue}
                  onChange={(val) => updateCondition(index, 'thresholdValue', val)}
                  disabled={disabled}
                  style={{ width: 100 }}
                  addonAfter={getMetricUnit(condition.metricName)}
                />
              )}

              {/* 持续时间 */}
              <InputNumber
                placeholder="持续"
                value={condition.durationSeconds}
                onChange={(val) => updateCondition(index, 'durationSeconds', val)}
                disabled={disabled}
                style={{ width: 100 }}
                addonAfter="秒"
                min={0}
              />
            </Space>

            {/* 删除按钮 */}
            {!disabled && (
              <Popconfirm
                title="确定删除此条件？"
                onConfirm={() => removeCondition(index)}
                okText="确定"
                cancelText="取消"
              >
                <Button 
                  type="text" 
                  danger 
                  icon={<DeleteOutlined />}
                  size="small"
                />
              </Popconfirm>
            )}
          </div>

          {/* 条件预览 */}
          {condition.conditionType === 'METRIC' && (
            <div className="condition-preview" style={{ marginTop: 8, color: '#666', fontSize: 12 }}>
              {metricNameOptions.find(o => o.value === condition.metricName)?.label || condition.metricName} 
              {' '}{getOperatorSymbol(condition.operator)}{' '}
              {condition.thresholdValue}{getMetricUnit(condition.metricName)}
              {condition.durationSeconds > 0 && ` 持续 ${condition.durationSeconds} 秒`}
            </div>
          )}
        </Card>
      </div>
    );
  };

  return (
    <div className="alert-condition-editor">
      <div className="condition-list">
        {localConditions.length === 0 ? (
          <Empty 
            description="暂无告警条件，点击下方按钮添加" 
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          />
        ) : (
          localConditions.map((condition, index) => renderCondition(condition, index))
        )}
      </div>

      {!disabled && (
        <Button 
          type="dashed" 
          icon={<PlusOutlined />} 
          onClick={addCondition}
          style={{ marginTop: 16, width: '100%' }}
        >
          添加条件
        </Button>
      )}

      <style>{`
        .alert-condition-editor .condition-item {
          margin-bottom: 8px;
        }
        .alert-condition-editor .condition-logic {
          display: flex;
          justify-content: center;
          margin: 4px 0;
        }
        .alert-condition-editor .condition-card {
          margin: 4px 0;
        }
        .alert-condition-editor .condition-row {
          display: flex;
          justify-content: space-between;
          align-items: center;
        }
        .alert-condition-editor .condition-preview {
          border-top: 1px dashed #d9d9d9;
          padding-top: 8px;
        }
      `}</style>
    </div>
  );
};

export default AlertConditionEditor;
