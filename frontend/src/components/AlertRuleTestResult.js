import React, { useState } from 'react';
import { Card, Result, Button, Spin, Space, Tag, Descriptions, Collapse, Empty, Typography } from 'antd';
import { 
  CheckCircleOutlined, CloseCircleOutlined, 
  ReloadOutlined, LoadingOutlined, InfoCircleOutlined 
} from '@ant-design/icons';

const { Panel } = Collapse;
const { Text } = Typography;

/**
 * 告警规则测试结果展示组件
 */
const AlertRuleTestResult = ({ testResult, ruleId, onTest, loading = false, disabled = false }) => {
  const [testing, setTesting] = useState(false);

  const handleTest = async () => {
    setTesting(true);
    try {
      await onTest?.();
    } finally {
      setTesting(false);
    }
  };

  const renderTestStatus = () => {
    if (testing || loading) {
      return (
        <Card>
          <div style={{ textAlign: 'center', padding: 24 }}>
            <Spin indicator={<LoadingOutlined style={{ fontSize: 32 }} spin />} />
            <div style={{ marginTop: 16 }}>正在测试规则...</div>
          </div>
        </Card>
      );
    }

    if (!testResult) {
      return (
        <Card>
          <Empty 
            description="点击下方按钮测试规则" 
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          >
            {!disabled && (
              <Button 
                type="primary" 
                icon={<ReloadOutlined />} 
                onClick={handleTest}
              >
                测试规则
              </Button>
            )}
          </Empty>
        </Card>
      );
    }

    const isSuccess = testResult.success;

    return (
      <Card>
        <Result
          status={isSuccess ? 'success' : 'error'}
          icon={isSuccess ? <CheckCircleOutlined /> : <CloseCircleOutlined />}
          title={isSuccess ? '测试通过' : '测试失败'}
          subTitle={testResult.message}
          extra={
            !disabled && (
              <Button type="primary" icon={<ReloadOutlined />} onClick={handleTest}>
                重新测试
              </Button>
            )
          }
        />

        {/* 测试详情 */}
        {testResult.details && Object.keys(testResult.details).length > 0 && (
          <Collapse ghost defaultActiveKey={['conditions', 'evaluation']}>
            <Panel header="测试详情" key="details">
              <Descriptions size="small" column={2} bordered>
                {Object.entries(testResult.details).map(([key, value]) => (
                  <Descriptions.Item key={key} label={key}>
                    {typeof value === 'boolean' ? (
                      <Tag color={value ? 'green' : 'red'}>
                        {value ? '满足' : '不满足'}
                      </Tag>
                    ) : (
                      <Text>{String(value)}</Text>
                    )}
                  </Descriptions.Item>
                ))}
              </Descriptions>
            </Panel>
          </Collapse>
        )}

        {/* 条件评估详情 */}
        {testResult.conditionResults && testResult.conditionResults.length > 0 && (
          <Collapse defaultActiveKey={['conditions']}>
            <Panel header="条件评估详情" key="conditions">
              {testResult.conditionResults.map((result, index) => (
                <Card key={index} size="small" style={{ marginBottom: 8 }}>
                  <Space>
                    {result.passed ? (
                      <CheckCircleOutlined style={{ color: '#52c41a' }} />
                    ) : (
                      <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
                    )}
                    <Text strong>条件 {index + 1}: {result.conditionName || result.metricName}</Text>
                    <Tag color={result.passed ? 'success' : 'error'}>
                      {result.passed ? '通过' : '未通过'}
                    </Tag>
                  </Space>
                  
                  <Descriptions size="small" column={3} style={{ marginTop: 8 }}>
                    <Descriptions.Item label="指标">{result.metricName}</Descriptions.Item>
                    <Descriptions.Item label="操作符">{result.operator}</Descriptions.Item>
                    <Descriptions.Item label="阈值">{result.thresholdValue}</Descriptions.Item>
                    <Descriptions.Item label="实际值">{result.actualValue ?? '-'}</Descriptions.Item>
                    <Descriptions.Item label="逻辑关系">{result.logicType || 'AND'}</Descriptions.Item>
                    <Descriptions.Item label="持续时间">{result.durationSeconds || 0}秒</Descriptions.Item>
                  </Descriptions>
                </Card>
              ))}
            </Panel>
          </Collapse>
        )}

        {/* 警告信息 */}
        {testResult.warnings && testResult.warnings.length > 0 && (
          <Collapse ghost>
            <Panel header={`警告信息 (${testResult.warnings.length})`} key="warnings">
              {testResult.warnings.map((warning, index) => (
                <Tag key={index} icon={<InfoCircleOutlined />} color="warning" style={{ marginBottom: 4 }}>
                  {warning}
                </Tag>
              ))}
            </Panel>
          </Collapse>
        )}

        {/* 错误信息 */}
        {testResult.errors && testResult.errors.length > 0 && (
          <Collapse ghost>
            <Panel header={`错误信息 (${testResult.errors.length})`} key="errors">
              {testResult.errors.map((error, index) => (
                <Tag key={index} color="error" style={{ marginBottom: 4 }}>
                  {error}
                </Tag>
              ))}
            </Panel>
          </Collapse>
        )}
      </Card>
    );
  };

  return (
    <div className="alert-rule-test-result">
      <Card 
        title="规则测试" 
        extra={
          ruleId && !disabled && (
            <Button 
              icon={<ReloadOutlined />} 
              onClick={handleTest}
              loading={testing}
            >
              测试
            </Button>
          )
        }
      >
        {renderTestStatus()}
      </Card>
    </div>
  );
};

export default AlertRuleTestResult;
