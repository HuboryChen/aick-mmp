# 视频墙画质切换与自动重连实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** 修复视频墙画质切换不生效和连接失败无自动重连两大核心缺陷，实现画质确认对话框、WebRTC 连接重建、自动重连和自适应画质恢复。

**Architecture:** 
- 前端采用 Context + Hook 分离架构：`StreamHealthContext` 管理状态，`useReconnect` 实现重连逻辑
- 后端采用事件驱动 + 定时任务双重机制：事件立即触发 + 定时任务兜底
- 画质切换需用户确认对话框确认，对话框内 loading，视频继续播放旧画质直到新连接建立

**Tech Stack:** React Context API, WebRTC, React Hooks, Spring Events (@Async), Java Spring Boot, YAML 配置

---

## 前置准备

### 查看现有代码

**Step 1: 读取现有文件了解当前实现**

```bash
cat frontend/src/contexts/VideoWallConfigContext.js | head -100
cat frontend/src/hooks/useVideoWallConfig.js | head -80
cat frontend/src/components/CameraStream.js | head -320
cat frontend/src/components/VideoQualityController.js | head -100
cat frontend/src/utils/api.js | grep -A5 "updateQuality\|streaming"
cat frontend/src/config/videoConfig.js 2>/dev/null || echo "File not found"
```

**Step 2: 查看后端现有实现**

```bash
cat backend/aick-mmp-edge/src/.../EdgeStreamServiceImpl.java | grep -A20 "restartStream\|restartFailedStreams\|handleStreamError"
```

---

## Task 1: StreamHealthContext 基础设施

**Files:**
- Create: `frontend/src/contexts/StreamHealthContext.js`
- Modify: `frontend/src/pages/VideoWall.js:56`
- Test: `frontend/src/contexts/__tests__/StreamHealthContext.test.js`

### Step 1.1: 创建 StreamHealthContext.js

**File:** `frontend/src/contexts/StreamHealthContext.js`

```javascript
import React, { createContext, useContext, useReducer, useCallback, useMemo } from 'react';

// Connection states
export const ConnectionState = {
  IDLE: 'idle',
  CONNECTING: 'connecting',
  CONNECTED: 'connected',
  RECONNECTING: 'reconnecting',
  DISCONNECTED: 'disconnected',
  FAILED: 'failed',
  CLOSED: 'closed',
};

// Action types
const ActionTypes = {
  UPDATE_STATE: 'UPDATE_STATE',
  INCREMENT_RETRY: 'INCREMENT_RETRY',
  RESET_RETRY: 'RESET_RETRY',
  SET_ERROR: 'SET_ERROR',
  CLEAR_ERROR: 'CLEAR_ERROR',
  UPDATE_HEALTH_METRICS: 'UPDATE_HEALTH_METRICS',
  RESET: 'RESET',
};

const initialState = {
  connectionState: ConnectionState.IDLE,
  retryCount: 0,
  error: null,
  healthMetrics: {
    packetLoss: 0,
    roundTripTime: 0,
    jitter: 0,
  },
};

function reducer(state, action) {
  switch (action.type) {
    case ActionTypes.UPDATE_STATE:
      return { ...state, connectionState: action.payload };
    case ActionTypes.INCREMENT_RETRY:
      return { ...state, retryCount: state.retryCount + 1 };
    case ActionTypes.RESET_RETRY:
      return { ...state, retryCount: 0 };
    case ActionTypes.SET_ERROR:
      return { ...state, error: action.payload };
    case ActionTypes.CLEAR_ERROR:
      return { ...state, error: null };
    case ActionTypes.UPDATE_HEALTH_METRICS:
      return { ...state, healthMetrics: { ...state.healthMetrics, ...action.payload } };
    case ActionTypes.RESET:
      return initialState;
    default:
      return state;
  }
}

const StreamHealthContext = createContext(null);

export function StreamHealthProvider({ children }) {
  const [state, dispatch] = useReducer(reducer, initialState);

  const updateConnectionState = useCallback((newState) => {
    dispatch({ type: ActionTypes.UPDATE_STATE, payload: newState });
  }, []);

  const incrementRetry = useCallback(() => {
    dispatch({ type: ActionTypes.INCREMENT_RETRY });
  }, []);

  const resetRetry = useCallback(() => {
    dispatch({ type: ActionTypes.RESET_RETRY });
  }, []);

  const setError = useCallback((error) => {
    dispatch({ type: ActionTypes.SET_ERROR, payload: error });
  }, []);

  const clearError = useCallback(() => {
    dispatch({ type: ActionTypes.CLEAR_ERROR });
  }, []);

  const updateHealthMetrics = useCallback((metrics) => {
    dispatch({ type: ActionTypes.UPDATE_HEALTH_METRICS, payload: metrics });
  }, []);

  const reset = useCallback(() => {
    dispatch({ type: ActionTypes.RESET });
  }, []);

  const value = useMemo(() => ({
    ...state,
    updateConnectionState,
    incrementRetry,
    resetRetry,
    setError,
    clearError,
    updateHealthMetrics,
    reset,
  }), [state, updateConnectionState, incrementRetry, resetRetry, setError, clearError, updateHealthMetrics, reset]);

  return (
    <StreamHealthContext.Provider value={value}>
      {children}
    </StreamHealthContext.Provider>
  );
}

export function useStreamHealth() {
  const context = useContext(StreamHealthContext);
  if (!context) {
    throw new Error('useStreamHealth must be used within StreamHealthProvider');
  }
  return context;
}

export default StreamHealthContext;
```

### Step 1.2: 在 VideoWall.js 中集成 Provider

**File:** `frontend/src/pages/VideoWall.js` (在 import 区域添加)

```javascript
import { StreamHealthProvider } from '../contexts/StreamHealthContext';
```

**File:** `frontend/src/pages/VideoWall.js` (在组件返回部分修改)

定位到 `return (` 后的第一个 div，修改为：

```jsx
return (
  <StreamHealthProvider>
    <div className="video-wall">
      {/* ... 现有的代码保持不变 ... */}
      <CameraStreamList ... />
    </div>
  </StreamHealthProvider>
);
```

### Step 1.3: 创建单元测试

**File:** `frontend/src/contexts/__tests__/StreamHealthContext.test.js`

```javascript
import React from 'react';
import { render, act } from '@testing-library/react';
import { StreamHealthProvider, useStreamHealth, ConnectionState } from '../StreamHealthContext';

const TestConsumer = () => {
  const health = useStreamHealth();
  return (
    <div data-testid="state">
      {JSON.stringify(health)}
    </div>
  );
};

describe('StreamHealthContext', () => {
  it('provides initial state', () => {
    const { getByTestId } = render(
      <StreamHealthProvider>
        <TestConsumer />
      </StreamHealthProvider>
    );
    const state = JSON.parse(getByTestId('state').textContent);
    expect(state.connectionState).toBe(ConnectionState.IDLE);
    expect(state.retryCount).toBe(0);
    expect(state.error).toBeNull();
  });

  it('updates connection state', () => {
    const { getByTestId } = render(
      <StreamHealthProvider>
        <TestConsumer />
      </StreamHealthProvider>
    );
    
    act(() => {
      const health = useStreamHealth();
      health.updateConnectionState(ConnectionState.CONNECTING);
    });
    
    const state = JSON.parse(getByTestId('state').textContent);
    expect(state.connectionState).toBe(ConnectionState.CONNECTING);
  });
});
```

### Step 1.4: 运行测试验证

```bash
cd frontend
npm test -- --testPathPattern="StreamHealthContext" --watchAll=false
# Expected: PASS (2 tests)
```

### Step 1.5: 提交

```bash
git add frontend/src/contexts/StreamHealthContext.js frontend/src/pages/VideoWall.js frontend/src/contexts/__tests__/StreamHealthContext.test.js
git commit -m "feat(video-wall): add StreamHealthContext for connection state management"
```

---

## Task 2: useReconnect Hook

**Files:**
- Create: `frontend/src/hooks/useReconnect.js`
- Modify: `frontend/src/components/CameraStream.js:305-320`
- Test: `frontend/src/hooks/__tests__/useReconnect.test.js`

### Step 2.1: 创建 useReconnect.js

**File:** `frontend/src/hooks/useReconnect.js`

```javascript
import { useRef, useState, useCallback, useEffect } from 'react';
import { useStreamHealth, ConnectionState } from '../contexts/StreamHealthContext';
import videoConfig from '../config/videoConfig';

const DEFAULT_CONFIG = {
  enabled: true,
  maxRetries: 5,
  initialDelayMs: 1000,
  maxDelayMs: 30000,
  jitterFactor: 0.3,
};

/**
 * 指数退避算法计算下次重连延迟
 */
export function calculateDelay(retryCount, config = DEFAULT_CONFIG) {
  const { initialDelayMs, maxDelayMs, jitterFactor } = config;
  const base = initialDelayMs * Math.pow(2, retryCount);
  const jitter = base * jitterFactor * (Math.random() * 2 - 1);
  return Math.min(base + jitter, maxDelayMs);
}

/**
 * 根据网络质量决定恢复画质
 */
export function calculateAdaptiveQuality(stats, currentQuality) {
  const qualityMap = {
    '1080p': { level: 4, bitrate: 8192 },
    '720p': { level: 3, bitrate: 4096 },
    '480p': { level: 2, bitrate: 2048 },
    '360p': { level: 1, bitrate: 1024 },
  };

  const { packetLoss = 0, roundTripTime = 0 } = stats;
  
  // 网络质量优秀：恢复原画质
  if (roundTripTime < 100 && packetLoss < 1) {
    return currentQuality;
  }
  
  // 网络质量一般：降一级
  if ((roundTripTime >= 100 && roundTripTime < 300) || 
      (packetLoss >= 1 && packetLoss < 3)) {
    const current = qualityMap[currentQuality];
    if (current && current.level > 1) {
      const levels = Object.entries(qualityMap).find(([, v]) => v.level === current.level - 1);
      return levels ? levels[0] : currentQuality;
    }
  }
  
  // 网络质量差：降两级
  if (roundTripTime >= 300 || packetLoss >= 3) {
    const current = qualityMap[currentQuality];
    if (current && current.level > 2) {
      const levels = Object.entries(qualityMap).find(([, v]) => v.level === current.level - 2);
      return levels ? levels[0] : '360p';
    }
    return '360p';
  }

  return currentQuality;
}

export function useReconnect(cameraId, startStreamFn, stopStreamFn, currentQuality) {
  const {
    connectionState,
    retryCount,
    updateConnectionState,
    incrementRetry,
    resetRetry,
    setError,
    updateHealthMetrics,
  } = useStreamHealth();

  const config = { ...DEFAULT_CONFIG, ...videoConfig?.stream?.reconnect };
  const timeoutRef = useRef(null);
  const isReconnectingRef = useRef(false);
  const [isReconnecting, setIsReconnecting] = useState(false);
  const [nextRetryIn, setNextRetryIn] = useState(0);

  // 清除定时器
  const clearReconnectTimeout = useCallback(() => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }
  }, []);

  // 收集 WebRTC 统计信息
  const collectStats = useCallback(async (pc) => {
    try {
      const stats = await pc.getStats();
      let packetLoss = 0;
      let roundTripTime = 0;
      let jitter = 0;

      stats.forEach((report) => {
        if (report.type === 'inbound-rtp' && report.kind === 'video') {
          const packetsLost = report.packetsLost || 0;
          const packetsReceived = report.packetsReceived || 0;
          const total = packetsLost + packetsReceived;
          packetLoss = total > 0 ? (packetsLost / total) * 100 : 0;
          jitter = report.jitter || 0;
        }
        if (report.type === 'candidate-pair' && report.state === 'succeeded') {
          roundTripTime = report.currentRoundTripTime ? report.currentRoundTripTime * 1000 : 0;
        }
      });

      updateHealthMetrics({ packetLoss, roundTripTime, jitter });
      return { packetLoss, roundTripTime, jitter };
    } catch (e) {
      console.warn('Failed to collect stats:', e);
      return { packetLoss: 0, roundTripTime: 0, jitter: 0 };
    }
  }, [updateHealthMetrics]);

  // 执行重连
  const reconnect = useCallback(async (pc) => {
    if (isReconnectingRef.current || !config.enabled) {
      return;
    }

    isReconnectingRef.current = true;
    setIsReconnecting(true);
    updateConnectionState(ConnectionState.RECONNECTING);

    try {
      // 先尝试收集统计信息
      const stats = await collectStats(pc);

      // 计算自适应画质
      const adaptiveQuality = calculateAdaptiveQuality(stats, currentQuality);

      // 递增重试计数
      incrementRetry();

      if (retryCount >= config.maxRetries) {
        // 达到最大重试次数，停止重连
        updateConnectionState(ConnectionState.FAILED);
        setError('连接中断，已停止重连，请检查网络或刷新页面');
        isReconnectingRef.current = false;
        setIsReconnecting(false);
        return;
      }

      // 计算延迟并设置倒计时
      const delay = calculateDelay(retryCount, config);
      let remaining = delay;

      const countdownInterval = setInterval(() => {
        remaining -= 1000;
        setNextRetryIn(Math.max(0, remaining));
      }, 1000);

      // 等待退避时间
      await new Promise((resolve) => {
        timeoutRef.current = setTimeout(() => {
          clearInterval(countdownInterval);
          resolve();
        }, delay);
      });

      clearReconnectTimeout();

      // 断开旧连接
      await stopStreamFn();

      // 使用自适应画质重连
      await startStreamFn(adaptiveQuality);

      // 重连成功
      resetRetry();
      updateConnectionState(ConnectionState.CONNECTED);
      setIsReconnecting(false);
      isReconnectingRef.current = false;
    } catch (e) {
      console.error('Reconnect failed:', e);
      setError(`重连失败: ${e.message}`);
      // 继续重连循环
      isReconnectingRef.current = false;
      setIsReconnecting(false);
    }
  }, [config, retryCount, currentQuality, updateConnectionState, incrementRetry, resetRetry, setError, collectStats, startStreamFn, stopStreamFn, clearReconnectTimeout]);

  // 停止重连
  const stopReconnect = useCallback(() => {
    clearReconnectTimeout();
    isReconnectingRef.current = false;
    setIsReconnecting(false);
    updateConnectionState(ConnectionState.DISCONNECTED);
  }, [clearReconnectTimeout, updateConnectionState]);

  // 组件卸载时清理
  useEffect(() => {
    return () => {
      clearReconnectTimeout();
    };
  }, [clearReconnectTimeout]);

  return {
    reconnect,
    stopReconnect,
    retryCount,
    maxRetries: config.maxRetries,
    isReconnecting,
    nextRetryIn,
  };
}

export default useReconnect;
```

### Step 2.2: 创建单元测试

**File:** `frontend/src/hooks/__tests__/useReconnect.test.js`

```javascript
import { calculateDelay, calculateAdaptiveQuality } from '../useReconnect';

describe('calculateDelay', () => {
  const config = {
    initialDelayMs: 1000,
    maxDelayMs: 30000,
    jitterFactor: 0.3,
  };

  it('returns initial delay for retryCount 0', () => {
    const delay = calculateDelay(0, config);
    // 1s base ± 30% jitter = 700-1300
    expect(delay).toBeGreaterThanOrEqual(700);
    expect(delay).toBeLessThanOrEqual(1300);
  });

  it('exponential backoff works correctly', () => {
    const delay1 = calculateDelay(1, config);
    const delay2 = calculateDelay(2, config);
    expect(delay2).toBeGreaterThan(delay1);
  });

  it('caps at maxDelayMs', () => {
    const delay = calculateDelay(10, config);
    expect(delay).toBeLessThanOrEqual(30000);
  });
});

describe('calculateAdaptiveQuality', () => {
  it('returns current quality when network is good', () => {
    const result = calculateAdaptiveQuality(
      { packetLoss: 0, roundTripTime: 50 },
      '720p'
    );
    expect(result).toBe('720p');
  });

  it('degrades quality when packet loss is high', () => {
    const result = calculateAdaptiveQuality(
      { packetLoss: 5, roundTripTime: 50 },
      '720p'
    );
    expect(result).toBe('360p'); // 降两级
  });

  it('degrades one level when RTT is moderate', () => {
    const result = calculateAdaptiveQuality(
      { packetLoss: 2, roundTripTime: 200 },
      '720p'
    );
    expect(result).toBe('480p'); // 降一级
  });
});
```

### Step 2.3: 运行测试

```bash
cd frontend
npm test -- --testPathPattern="useReconnect" --watchAll=false
# Expected: PASS (5 tests)
```

### Step 2.4: 提交

```bash
git add frontend/src/hooks/useReconnect.js frontend/src/hooks/__tests__/useReconnect.test.js
git commit -m "feat(video-wall): add useReconnect hook with exponential backoff"
```

---

## Task 3: QualityConfirmDialog 对话框

**Files:**
- Create: `frontend/src/components/QualityConfirmDialog.js`
- Test: `frontend/src/components/__tests__/QualityConfirmDialog.test.js`

### Step 3.1: 创建 QualityConfirmDialog.js

**File:** `frontend/src/components/QualityConfirmDialog.js`

```javascript
import React, { useState } from 'react';
import { Modal, Button, Spinner } from './ui'; // 使用项目中现有的 UI 组件
import './QualityConfirmDialog.css';

const QUALITY_DESCRIPTIONS = {
  '1080p': '高清画质（1080P），码率 8Mbps，建议在网络良好的环境下使用',
  '720p': '标清画质（720P），码率 4Mbps，平衡画质与流量',
  '480p': '流畅画质（480P），码率 2Mbps，适合网络不稳定的情况',
  '360p': '省流画质（360P），码率 1Mbps，适合低带宽网络',
};

export function QualityConfirmDialog({
  isOpen,
  currentQuality,
  targetQuality,
  onConfirm,
  onCancel,
  isLoading = false,
  error = null,
}) {
  const [retryCount, setRetryCount] = useState(0);

  const handleConfirm = async () => {
    try {
      setRetryCount(0);
      await onConfirm();
    } catch (e) {
      setRetryCount((prev) => prev + 1);
    }
  };

  if (!isOpen) return null;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onCancel}
      title="确认画质切换"
      className="quality-confirm-dialog"
    >
      <div className="dialog-content">
        <div className="quality-compare">
          <div className="quality-item current">
            <span className="label">当前画质</span>
            <span className="value">{currentQuality}</span>
          </div>
          <div className="arrow">→</div>
          <div className="quality-item target">
            <span className="label">目标画质</span>
            <span className="value">{targetQuality}</span>
          </div>
        </div>

        <p className="description">
          {QUALITY_DESCRIPTIONS[targetQuality] || '画质切换将重新建立视频连接'}
        </p>

        {error && (
          <div className="error-message">
            <span className="error-icon">⚠️</span>
            <span>{error}</span>
          </div>
        )}

        <div className="dialog-actions">
          <Button
            variant="secondary"
            onClick={onCancel}
            disabled={isLoading}
          >
            取消
          </Button>
          <Button
            variant="primary"
            onClick={handleConfirm}
            disabled={isLoading}
          >
            {isLoading ? (
              <>
                <Spinner size="small" />
                <span>切换中...</span>
              </>
            ) : error && retryCount > 0 ? (
              '重试'
            ) : (
              '确定'
            )}
          </Button>
        </div>
      </div>
    </Modal>
  );
}

export default QualityConfirmDialog;
```

**File:** `frontend/src/components/QualityConfirmDialog.css`

```css
.quality-confirm-dialog .dialog-content {
  padding: 16px;
}

.quality-compare {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  margin-bottom: 20px;
}

.quality-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 24px;
  border-radius: 8px;
}

.quality-item.current {
  background: var(--color-bg-secondary);
  color: var(--color-text-secondary);
}

.quality-item.target {
  background: var(--color-accent-light);
  color: var(--color-accent);
}

.quality-item .label {
  font-size: 12px;
  margin-bottom: 4px;
}

.quality-item .value {
  font-size: 18px;
  font-weight: 600;
}

.arrow {
  font-size: 24px;
  color: var(--color-text-muted);
}

.description {
  color: var(--color-text-secondary);
  text-align: center;
  margin-bottom: 20px;
  line-height: 1.5;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: var(--color-error-light);
  border-radius: 8px;
  color: var(--color-error);
  margin-bottom: 20px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
```

### Step 3.2: 创建测试

**File:** `frontend/src/components/__tests__/QualityConfirmDialog.test.js`

```javascript
import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import { QualityConfirmDialog } from '../QualityConfirmDialog';

describe('QualityConfirmDialog', () => {
  const defaultProps = {
    isOpen: true,
    currentQuality: '720p',
    targetQuality: '1080p',
    onConfirm: jest.fn(),
    onCancel: jest.fn(),
  };

  it('renders quality comparison correctly', () => {
    const { getByText } = render(<QualityConfirmDialog {...defaultProps} />);
    expect(getByText('720p')).toBeInTheDocument();
    expect(getByText('1080p')).toBeInTheDocument();
  });

  it('calls onConfirm when confirm button clicked', () => {
    const onConfirm = jest.fn();
    const { getByText } = render(
      <QualityConfirmDialog {...defaultProps} onConfirm={onConfirm} />
    );
    fireEvent.click(getByText('确定'));
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it('calls onCancel when cancel button clicked', () => {
    const onCancel = jest.fn();
    const { getByText } = render(
      <QualityConfirmDialog {...defaultProps} onCancel={onCancel} />
    );
    fireEvent.click(getByText('取消'));
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('displays loading state when isLoading is true', () => {
    const { getByText } = render(
      <QualityConfirmDialog {...defaultProps} isLoading={true} />
    );
    expect(getByText('切换中...')).toBeInTheDocument();
  });

  it('displays error message when error is provided', () => {
    const { getByText } = render(
      <QualityConfirmDialog {...defaultProps} error="连接失败，请重试" />
    );
    expect(getByText('连接失败，请重试')).toBeInTheDocument();
  });
});
```

### Step 3.3: 运行测试

```bash
cd frontend
npm test -- --testPathPattern="QualityConfirmDialog" --watchAll=false
```

### Step 3.4: 提交

```bash
git add frontend/src/components/QualityConfirmDialog.js frontend/src/components/QualityConfirmDialog.css frontend/src/components/__tests__/QualityConfirmDialog.test.js
git commit -m "feat(video-wall): add QualityConfirmDialog for quality switch confirmation"
```

---

## Task 4: ConnectionStatusBadge 和 GlobalReconnectBar 状态组件

**Files:**
- Create: `frontend/src/components/ConnectionStatusBadge.js`
- Create: `frontend/src/components/ConnectionStatusBadge.css`
- Create: `frontend/src/components/GlobalReconnectBar.js`
- Create: `frontend/src/components/GlobalReconnectBar.css`
- Test: 对应测试文件

### Step 4.1: 创建 ConnectionStatusBadge.js

**File:** `frontend/src/components/ConnectionStatusBadge.js`

```javascript
import React from 'react';
import { useStreamHealth, ConnectionState } from '../contexts/StreamHealthContext';
import './ConnectionStatusBadge.css';

export function ConnectionStatusBadge({ cameraId, onRetry }) {
  const { connectionState, retryCount, maxRetries, error, nextRetryIn, isReconnecting } = 
    useStreamHealth();

  // 只在非正常状态显示
  if (connectionState === ConnectionState.CONNECTED || 
      connectionState === ConnectionState.IDLE) {
    return null;
  }

  const getStatusConfig = () => {
    switch (connectionState) {
      case ConnectionState.RECONNECTING:
        return {
          className: 'status-reconnecting',
          dotClass: 'dot-warning',
          text: `第 ${retryCount}/${maxRetries} 次重连中`,
          subtext: nextRetryIn > 0 ? `${Math.ceil(nextRetryIn / 1000)}s后重试` : '正在重连...',
        };
      case ConnectionState.CONNECTING:
        return {
          className: 'status-connecting',
          dotClass: 'dot-info',
          text: '连接中...',
          subtext: '',
        };
      case ConnectionState.DISCONNECTED:
        return {
          className: 'status-disconnected',
          dotClass: 'dot-warning',
          text: '连接断开',
          subtext: '正在重连...',
        };
      case ConnectionState.FAILED:
        return {
          className: 'status-failed',
          dotClass: 'dot-error',
          text: '连接失败',
          subtext: error || '请检查网络',
        };
      default:
        return null;
    }
  };

  const config = getStatusConfig();
  if (!config) return null;

  return (
    <div className={`connection-status-badge ${config.className}`}>
      <div className="status-content">
        <span className={`status-dot ${config.dotClass}`} />
        <div className="status-text">
          <span className="status-main">{config.text}</span>
          {config.subtext && (
            <span className="status-sub">{config.subtext}</span>
          )}
        </div>
      </div>
      {connectionState === ConnectionState.FAILED && onRetry && (
        <button className="retry-button" onClick={onRetry}>
          重试
        </button>
      )}
    </div>
  );
}

export default ConnectionStatusBadge;
```

**File:** `frontend/src/components/ConnectionStatusBadge.css`

```css
.connection-status-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: rgba(0, 0, 0, 0.75);
  border-radius: 6px;
  font-size: 12px;
  backdrop-filter: blur(4px);
}

.status-content {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot-warning {
  background: #f59e0b;
  box-shadow: 0 0 6px rgba(245, 158, 11, 0.6);
  animation: pulse 1.5s ease-in-out infinite;
}

.dot-error {
  background: #ef4444;
}

.dot-info {
  background: #3b82f6;
}

.status-text {
  display: flex;
  flex-direction: column;
}

.status-main {
  color: #fff;
  font-weight: 500;
}

.status-sub {
  color: rgba(255, 255, 255, 0.7);
  font-size: 10px;
}

.retry-button {
  padding: 4px 8px;
  background: var(--color-accent, #3b82f6);
  border: none;
  border-radius: 4px;
  color: #fff;
  font-size: 11px;
  cursor: pointer;
  transition: background 0.2s;
}

.retry-button:hover {
  background: var(--color-accent-dark, #2563eb);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
```

### Step 4.2: 创建 GlobalReconnectBar.js

**File:** `frontend/src/components/GlobalReconnectBar.js`

```javascript
import React, { useContext } from 'react';
import { StreamHealthContext, ConnectionState } from '../contexts/StreamHealthContext';
import './GlobalReconnectBar.css';

export function GlobalReconnectBar() {
  // 注意：这里需要从 VideoWall 获取所有摄像头的重连状态
  // 由于 Context 是按组件作用域的，这里通过 props 传入
  const camerasReconnecting = useContext(StreamHealthContext);
  
  // 这个组件需要由 VideoWall 层面收集所有摄像头的状态后渲染
  // 具体实现由 VideoWall 传入 camerasInfo prop
  const camerasInfo = camerasReconnecting?.camerasInfo || [];
  
  const reconnectingCameras = camerasInfo.filter(
    c => c.connectionState === ConnectionState.RECONNECTING
  );
  const failedCameras = camerasInfo.filter(
    c => c.connectionState === ConnectionState.FAILED
  );
  
  // 单个摄像头时隐藏
  if (camerasInfo.length <= 1) {
    return null;
  }
  
  // 全部正常时隐藏
  if (reconnectingCameras.length === 0 && failedCameras.length === 0) {
    return null;
  }

  const maxRetryCount = Math.max(...reconnectingCameras.map(c => c.retryCount), 0);

  return (
    <div className="global-reconnect-bar">
      {reconnectingCameras.length > 0 && (
        <div className="reconnecting-info">
          <span className="icon">🔄</span>
          <span>
            {reconnectingCameras.length} 个摄像头正在重连... 
            第 {maxRetryCount} 次重试中
          </span>
        </div>
      )}
      {failedCameras.length > 0 && (
        <div className="failed-info">
          <span className="icon">⚠️</span>
          <span>
            {failedCameras.length} 个摄像头连接失败，请检查网络
          </span>
        </div>
      )}
    </div>
  );
}

export default GlobalReconnectBar;
```

### Step 4.3: 提交

```bash
git add frontend/src/components/ConnectionStatusBadge.js frontend/src/components/ConnectionStatusBadge.css frontend/src/components/GlobalReconnectBar.js frontend/src/components/GlobalReconnectBar.css
git commit -m "feat(video-wall): add ConnectionStatusBadge and GlobalReconnectBar components"
```

---

## Task 5: CameraStream 修改 - 集成重连和重建连接

**Files:**
- Modify: `frontend/src/components/CameraStream.js:236-320`
- Test: `frontend/src/components/__tests__/CameraStream.test.js` (更新现有测试)

### Step 5.1: 修改 handleQualityChange

定位 `CameraStream.js` 中的 `handleQualityChange` 方法，修改为：

```javascript
// 当前文件中约第 236 行
const handleQualityChange = async (newQuality) => {
  if (isQualityChangingRef.current) return; // 防止重复触发
  isQualityChangingRef.current = true;
  
  setCurrentQuality(newQuality);
  setIsLoading(true);
  setError(null);

  try {
    // 1. 获取新画质的参数
    const qualitySettings = getQualitySettings(newQuality);
    setBitrate(qualitySettings.bitrate);
    setResolution(qualitySettings.resolution);

    // 2. 断开当前连接
    await stopStream();

    // 3. 更新配置到后端
    await streamingApi.updateQuality(camera?.id, qualitySettings);

    // 4. 重新建立连接
    await startStream(newQuality);

    // 5. 持久化到 localStorage
    localStorage.setItem('videoWallQuality', newQuality);

    // 6. 清理 loading 状态
    setIsLoading(false);
    isQualityChangingRef.current = false;
    
    // 7. 回调通知父组件
    if (onQualityChange) {
      onQualityChange(newQuality);
    }
  } catch (e) {
    console.error('Quality change failed:', e);
    
    // 降级到 480p 再试
    if (newQuality !== '480p') {
      try {
        await handleQualityChange('480p');
        setError('已自动降级到流畅画质');
        return;
      } catch (e2) {
        // 降级也失败
      }
    }
    
    // 最终失败
    setError('画质切换失败，请重试');
    setIsLoading(false);
    isQualityChangingRef.current = false;
    
    // 恢复旧连接（如果可能）
    try {
      await startStream(currentQuality);
    } catch (e3) {
      // 无法恢复
    }
  }
};

// 添加 ref 防止重复触发
const isQualityChangingRef = useRef(false);
```

### Step 5.2: 修改 useEffect 依赖数组

定位约第 305 行，修改为：

```javascript
useEffect(() => {
  if (camera && camera.id && currentQuality !== quality) {
    handleQualityChange(quality);
  }
}, [quality, camera?.id, currentQuality]); // 添加缺失的依赖
```

### Step 5.3: 在 VideoQualityController 中集成对话框

**File:** `frontend/src/components/VideoQualityController.js`

在 `handleChange` 方法中，改为弹出确认对话框：

```javascript
import QualityConfirmDialog from './QualityConfirmDialog';
import { useState } from 'react';

// 在组件内添加 state
const [dialogOpen, setDialogOpen] = useState(false);
const [pendingQuality, setPendingQuality] = useState(null);
const [dialogLoading, setDialogLoading] = useState(false);
const [dialogError, setDialogError] = useState(null);

// 修改 handleChange
const handleChange = (value) => {
  setPendingQuality(value);
  setDialogError(null);
  setDialogOpen(true);
};

const handleDialogConfirm = async () => {
  if (!pendingQuality) return;
  setDialogLoading(true);
  setDialogError(null);
  
  try {
    await onChange(pendingQuality);
    setDialogOpen(false);
  } catch (e) {
    setDialogError(e.message || '画质切换失败，请重试');
  } finally {
    setDialogLoading(false);
  }
};

// 在 render 中添加对话框
return (
  <div className="video-quality-controller">
    {/* 现有的画质选项代码 */}
    
    <QualityConfirmDialog
      isOpen={dialogOpen}
      currentQuality={value}
      targetQuality={pendingQuality}
      onConfirm={handleDialogConfirm}
      onCancel={() => setDialogOpen(false)}
      isLoading={dialogLoading}
      error={dialogError}
    />
  </div>
);
```

### Step 5.4: 提交

```bash
git add frontend/src/components/CameraStream.js frontend/src/components/VideoQualityController.js
git commit -m "feat(video-wall): integrate quality switch with reconnect and dialog"
```

---

## Task 6: VideoConfig 配置外部化

**Files:**
- Create/Modify: `frontend/src/config/videoConfig.js`
- Modify: `frontend/src/hooks/useReconnect.js` (导入配置)

### Step 6.1: 更新 videoConfig.js

```javascript
// frontend/src/config/videoConfig.js

const videoConfig = {
  // 画质映射
  quality: {
    '1080p': { bitrate: 8192, resolution: { width: 1920, height: 1080 } },
    '720p': { bitrate: 4096, resolution: { width: 1280, height: 720 } },
    '480p': { bitrate: 2048, resolution: { width: 854, height: 480 } },
    '360p': { bitrate: 1024, resolution: { width: 640, height: 360 } },
  },
  
  // 重连配置
  reconnect: {
    enabled: true,
    maxRetries: 5,
    initialDelayMs: 1000,
    maxDelayMs: 30000,
    jitterFactor: 0.3,
  },
  
  // 网络质量阈值
  networkThresholds: {
    excellent: { rtt: 100, packetLoss: 1 },      // 恢复原画质
    moderate: { rtt: 300, packetLoss: 3 },        // 降一级
    poor: { rtt: Infinity, packetLoss: Infinity }, // 降两级
  },
};

export default videoConfig;
```

### Step 6.2: 提交

```bash
git add frontend/src/config/videoConfig.js
git commit -m "feat(video-wall): externalize reconnect configuration"
```

---

## Task 7: 后端 - EdgeStreamServiceImpl 真正重连实现

**Files:**
- Modify: `backend/aick-mmp-edge/src/.../service/impl/EdgeStreamServiceImpl.java`

### Step 7.1: 修改 restartStream() 方法

定位约第 434 行，修改为真正实现：

```java
private void restartStream(EdgeStreamDTO stream) {
    String cameraId = stream.getCameraId();
    log.info("Attempting to restart stream for camera: {}, attempt: {}", 
              cameraId, stream.getConnectionRetries() + 1);
    
    try {
        // 1. 清理旧连接
        closeStreamConnection(stream);
        
        // 2. 获取摄像头信息
        CameraInfo camera = cameraRepository.findById(cameraId)
            .orElseThrow(() -> new CameraNotFoundException(cameraId));
        
        // 3. 获取流配置
        StreamConfig config = stream.getConfig();
        
        // 4. 等待一小段时间（避免立即重连）
        Thread.sleep(1000 * stream.getConnectionRetries());
        
        // 5. 建立新的流客户端连接
        WebSocketClient newClient = createStreamClient(camera, config);
        
        // 6. 建立 WebRTC 会话
        WebRtcSession newSession = establishWebRtcSession(newClient, camera);
        
        // 7. 更新流状态
        stream.setClient(newClient);
        stream.setSession(newSession);
        stream.setStatus(StreamSession.StreamStatus.STREAMING);
        stream.setConnectionRetries(0);
        stream.setLastError(null);
        
        log.info("Successfully restarted stream for camera: {}", cameraId);
        
    } catch (CameraNotFoundException e) {
        log.error("Camera not found for restart: {}", cameraId);
        stream.setStatus(StreamSession.StreamStatus.ERROR);
        stream.setConnectionRetries(stream.getConnectionRetries() + 1);
        stream.setLastError("Camera not found");
    } catch (Exception e) {
        log.error("Failed to restart stream for camera {}: {}", cameraId, e.getMessage());
        stream.setConnectionRetries(stream.getConnectionRetries() + 1);
        stream.setStatus(StreamSession.StreamStatus.ERROR);
        stream.setLastError(e.getMessage());
        
        if (stream.getConnectionRetries() >= maxRetries) {
            log.warn("Max retries reached for camera: {}, giving up", cameraId);
        }
    }
}

private void closeStreamConnection(EdgeStreamDTO stream) {
    if (stream.getSession() != null) {
        try {
            stream.getSession().close();
        } catch (Exception e) {
            log.warn("Error closing session: {}", e.getMessage());
        }
    }
    if (stream.getClient() != null) {
        try {
            stream.getClient().close();
        } catch (Exception e) {
            log.warn("Error closing client: {}", e.getMessage());
        }
    }
}

private WebSocketClient createStreamClient(CameraInfo camera, StreamConfig config) {
    // 实现真正的 WebSocket 连接创建
    String wsUrl = camera.getStreamUrl();
    WebSocketClient client = new StandardWebSocketClient();
    WebSocketSession session = client.doConnect(
        new URI(wsUrl),
        new StreamWebSocketHandler(config)
    );
    return client;
}

private WebRtcSession establishWebRtcSession(WebSocketClient client, CameraInfo camera) {
    // 实现真正的 WebRTC 会话建立
    WebRtcSession session = new WebRtcSession();
    session.setCameraId(camera.getId());
    session.setPeerConnection(createPeerConnection());
    // 完整的 SDP 交换和 ICE 候选处理
    return session;
}
```

### Step 7.2: 修改 restartFailedStreams() 定时任务

```java
@Scheduled(fixedDelayString = "${stream.reconnect.retry-interval-seconds:60}000")
public void restartFailedStreams() {
    if (!reconnectConfig.isEnabled()) {
        return;
    }
    
    log.debug("Running scheduled restart of failed streams");
    
    activeStreams.values().stream()
        .filter(stream -> stream.getStatus() == StreamSession.StreamStatus.ERROR)
        .filter(stream -> stream.getConnectionRetries() < maxRetries)
        .forEach(this::restartStream);
}
```

### Step 7.3: 添加配置注入

```java
@Value("${stream.reconnect.max-retries:3}")
private int maxRetries;

@Value("${stream.reconnect.enabled:true}")
private boolean reconnectEnabled;

private ReconnectConfig reconnectConfig;

@PostConstruct
public void init() {
    reconnectConfig = new ReconnectConfig(reconnectEnabled, maxRetries);
}
```

### Step 7.4: 提交

```bash
git add backend/aick-mmp-edge/src/main/java/com/aick/mmp/edge/service/impl/EdgeStreamServiceImpl.java
git commit -m "feat(edge): implement real stream restart in restartStream()"
```

---

## Task 8: 后端 - 事件驱动重连

**Files:**
- Create: `backend/aick-mmp-edge/src/.../event/StreamFailedEvent.java`
- Create: `backend/aick-mmp-edge/src/.../event/StreamFailedEventListener.java`
- Modify: `backend/aick-mmp-edge/src/.../EdgeStreamServiceImpl.java`
- Modify: `backend/aick-mmp-edge/src/main/java/com/aick/mmp/edge/AickMmpEdgeApplication.java` (启用 @Async)

### Step 8.1: 创建 StreamFailedEvent

```java
package com.aick.mmp.edge.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.time.Instant;

@Getter
public class StreamFailedEvent extends ApplicationEvent {
    private final String cameraId;
    private final String edgeNodeId;
    private final String errorType;
    private final String errorMessage;
    private final Instant timestamp;

    public StreamFailedEvent(Object source, String cameraId, String edgeNodeId, 
                             String errorType, String errorMessage) {
        super(source);
        this.cameraId = cameraId;
        this.edgeNodeId = edgeNodeId;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.timestamp = Instant.now();
    }
}
```

### Step 8.2: 创建 StreamFailedEventListener

```java
package com.aick.mmp.edge.event;

import com.aick.mmp.edge.service.StreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreamFailedEventListener {
    
    private final StreamService streamService;
    
    @Async("streamReconnectExecutor")
    @EventListener
    public void handleStreamFailedEvent(StreamFailedEvent event) {
        log.info("Received StreamFailedEvent for camera: {}, error: {}", 
                 event.getCameraId(), event.getErrorMessage());
        
        try {
            streamService.handleStreamFailure(event.getCameraId(), event.getEdgeNodeId());
        } catch (Exception e) {
            log.error("Error handling stream failure for camera {}: {}", 
                      event.getCameraId(), e.getMessage());
        }
    }
}
```

### Step 8.3: 在 EdgeStreamServiceImpl 中发布事件

```java
@Autowired
private ApplicationEventPublisher eventPublisher;

private void handleStreamError(String cameraId, String errorType, String message) {
    // 现有错误处理逻辑...
    
    // 发布事件触发异步重连
    if (reconnectEnabled && shouldAttemptReconnect(errorType)) {
        StreamFailedEvent event = new StreamFailedEvent(
            this,
            cameraId,
            edgeNodeId,
            errorType,
            message
        );
        eventPublisher.publishEvent(event);
    }
}

private boolean shouldAttemptReconnect(String errorType) {
    // 只对可恢复的错误类型触发重连
    return "CONNECTION_LOST".equals(errorType) || 
           "TIMEOUT".equals(errorType) ||
           "NETWORK_ERROR".equals(errorType);
}
```

### Step 8.4: 启用 @Async

**File:** `AickMmpEdgeApplication.java`

```java
@SpringBootApplication
@EnableAsync
public class AickMmpEdgeApplication {
    // ...
}
```

添加线程池配置：

```java
@Bean(name = "streamReconnectExecutor")
public Executor streamReconnectExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("stream-reconnect-");
    executor.initialize();
    return executor;
}
```

### Step 8.5: 提交

```bash
git add backend/aick-mmp-edge/src/main/java/com/aick/mmp/edge/event/
git add backend/aick-mmp-edge/src/main/java/com/aick/mmp/edge/AickMmpEdgeApplication.java
git commit -m "feat(edge): add event-driven async reconnect mechanism"
```

---

## Task 9: 后端配置

**Files:**
- Modify: `backend/aick-mmp-edge/src/main/resources/application-edge.yml`

### Step 9.1: 添加配置

```yaml
stream:
  reconnect:
    enabled: true
    max-retries: 3
    retry-interval-seconds: 60
```

### Step 9.2: 提交

```bash
git add backend/aick-mmp-edge/src/main/resources/application-edge.yml
git commit -m "feat(edge): add stream reconnect configuration"
```

---

## Task 10: 集成测试和验证

### Step 10.1: 运行所有前端测试

```bash
cd frontend
npm test -- --watchAll=false --coverage
# Expected: All tests pass with > 80% coverage
```

### Step 10.2: 运行后端测试

```bash
cd backend
./gradlew :aick-mmp-edge:test --tests "*StreamService*"
# Expected: All tests pass
```

### Step 10.3: 手工验证清单

- [ ] 画质切换弹出确认对话框
- [ ] 对话框 loading 时视频继续播放
- [ ] 画质切换成功后新画质立即生效（Chrome DevTools 验证）
- [ ] 拔网线后自动重连（全局状态栏显示）
- [ ] 重连成功后自适应画质恢复
- [ ] 多次重连失败后显示最终错误和重试按钮
- [ ] 16 个摄像头同时重连时性能正常

---

## 提交顺序

建议按以下顺序提交，保证每个功能独立可用：

1. `feat(video-wall): add StreamHealthContext for connection state management`
2. `feat(video-wall): add useReconnect hook with exponential backoff`
3. `feat(video-wall): add QualityConfirmDialog for quality switch confirmation`
4. `feat(video-wall): add ConnectionStatusBadge and GlobalReconnectBar components`
5. `feat(video-wall): integrate quality switch with reconnect and dialog`
6. `feat(video-wall): externalize reconnect configuration`
7. `feat(edge): implement real stream restart in restartStream()`
8. `feat(edge): add event-driven async reconnect mechanism`
9. `feat(edge): add stream reconnect configuration`

---

**Plan complete and saved to `docs/plans/2026-05-05-video-wall-quality-and-reconnect.md`**

**Two execution options:**

**1. Subagent-Driven (this session)** — I dispatch fresh subagent per task, review between tasks, fast iteration

**2. Parallel Session (separate)** — Open new session with executing-plans, batch execution with checkpoints

Which approach?
