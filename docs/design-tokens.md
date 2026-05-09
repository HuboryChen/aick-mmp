# Design Tokens - 工业风设计系统

本文档记录了 AICK-MMP 项目中所有 CSS Variables 的用途和使用规范。

## 🎨 设计理念

**Industrial Command Center** 风格，专为专业监控场景打造：
- **深空灰背景** + **霓虹蓝点缀**
- **科技感动效** + **高可读性**
- **双主题支持**（暗色/亮色）

## 📋 Token 分类

### 1. 背景色（Background Colors）

| Token | 暗色模式 | 亮色模式 | 用途 |
|-------|---------|---------|------|
| `--color-bg-primary` | `#0a0e17` | `#f1f5f9` | 页面主背景 |
| `--color-bg-secondary` | `#141820` | `#ffffff` | 次级背景、输入框背景 |
| `--color-bg-card` | `#1a1f2e` | `#ffffff` | 卡片背景 |
| `--color-bg-elevated` | `#242b3d` | `#f8fafc` | 弹出层、下拉菜单背景 |

**使用示例**：
```css
.my-component {
  background: var(--color-bg-primary);
}
```

### 2. 强调色（Accent Colors）

| Token | 暗色模式 | 亮色模式 | 用途 |
|-------|---------|---------|------|
| `--color-accent` | `#00d4ff` | `#0284c7` | 主强调色（霓虹蓝/深蓝） |
| `--color-accent-hover` | `#33ddff` | `#0369a1` | 悬停状态强调色 |
| `--color-accent-muted` | `rgba(0, 212, 255, 0.1)` | `rgba(2, 132, 199, 0.1)` | 强调色淡化背景 |

**使用示例**：
```jsx
<button style={{ 
  background: 'var(--color-accent)',
  color: '#000'
}}>
  主按钮
</button>
```

### 3. 文本色（Text Colors）

| Token | 暗色模式 | 亮色模式 | 用途 |
|-------|---------|---------|------|
| `--color-text-primary` | `#ffffff` | `#0f172a` | 主要文本 |
| `--color-text-secondary` | `#94a3b8` | `#64748b` | 次要文本、标签 |
| `--color-text-muted` | `#64748b` | `#94a3b8` | 弱化文本、占位符 |

**对比度要求**：
- 主要文本：对比度 > 7:1 (AAA)
- 次要文本：对比度 > 4.5:1 (AA)

### 4. 状态色（Status Colors）

| Token | 暗色模式 | 亮色模式 | 语义 |
|-------|---------|---------|------|
| `--status-online` | `#00ff88` | `#16a34a` | 在线/正常 |
| `--status-offline` | `#ff4757` | `#dc2626` | 离线/错误 |
| `--status-warning` | `#fbbf24` | `#d97706` | 告警/警告 |
| `--status-info` | `#60a5fa` | `#2563eb` | 信息/提示 |

**使用场景**：
```jsx
<StatusIndicator status="online" />  // 绿色脉冲
<StatusIndicator status="offline" /> // 红色静态
<StatusIndicator status="warning" /> // 黄色脉冲
```

### 5. 边框与阴影（Borders & Shadows）

#### 边框

| Token | 暗色模式 | 亮色模式 | 用途 |
|-------|---------|---------|------|
| `--color-border` | `rgba(255, 255, 255, 0.1)` | `rgba(0, 0, 0, 0.1)` | 默认边框 |
| `--color-border-hover` | `rgba(255, 255, 255, 0.2)` | `rgba(0, 0, 0, 0.2)` | 悬停边框 |

#### 阴影

| Token | 暗色模式值 | 用途 |
|-------|-----------|------|
| `--shadow-sm` | `0 2px 8px rgba(0, 0, 0, 0.3)` | 小阴影 |
| `--shadow-md` | `0 4px 16px rgba(0, 0, 0, 0.4)` | 中阴影 |
| `--shadow-lg` | `0 8px 32px rgba(0, 0, 0, 0.5)` | 大阴影 |
| `--shadow-glow` | `0 0 20px rgba(0, 212, 255, 0.3)` | 发光效果 |
| `--shadow-glow-strong` | `0 0 30px rgba(0, 212, 255, 0.5)` | 强发光效果 |

**发光效果示例**：
```css
.industrial-card:hover {
  border-color: var(--color-accent);
  box-shadow: var(--shadow-glow);
}
```

### 6. 渐变（Gradients）

| Token | 暗色模式值 | 用途 |
|-------|-----------|------|
| `--gradient-accent` | `linear-gradient(135deg, #00d4ff 0%, #0088cc 100%)` | 强调色渐变 |
| `--gradient-card` | `linear-gradient(145deg, rgba(26, 31, 46, 0.9) 0%, rgba(20, 24, 32, 0.95) 100%)` | 卡片背景渐变 |

### 7. 字体（Typography）

| Token | 值 | 用途 |
|-------|---|------|
| `--font-sans` | `'Inter', 'Noto Sans SC', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif` | 主字体 |
| `--font-mono` | `'JetBrains Mono', 'Fira Code', 'SF Mono', Consolas, monospace` | 等宽字体 |

### 8. 布局间距（Layout Spacing）

| Token | 值 | 用途 |
|-------|---|------|
| `--header-height` | `64px` | Header 高度 |
| `--sidebar-width` | `200px` | Sidebar 展开宽度 |
| `--sidebar-collapsed-width` | `80px` | Sidebar 折叠宽度 |

#### 8.1 卡片布局间距规范

> **详细规范请参考**：[card-layout-spacing-guide.md](./frontend/card-layout-spacing-guide.md)

**gutter 值规范**：

| 场景 | gutter 值 | 说明 |
|------|-----------|------|
| 标准页面 | `gutter={[16, 16]}` | Dashboard, Analytics 等 |
| Modal 表单 | `gutter={[16, 16]}` | 统一使用数组语法 |
| 响应式 | `{ xs: 8, sm: 12, lg: 16 }` | 移动端适配 |

**区块间距层级**：

| 层级 | Tailwind | 像素值 | 使用场景 |
|------|----------|--------|----------|
| 第一层 | `mb-6` | 24px | 页面主要区块之间 |
| 第二层 | `mb-4` | 16px | 区块内卡片行之间 |
| 第三层 | `mt-2`/`mb-2` | 8px | 标签页之间、表格操作栏 |
| 组件内 | `mt-3`/`mb-1` | 12px/4px | 卡片内部元素 |

**Card padding 规范**：

| Card 类型 | padding | 使用场景 |
|-----------|---------|----------|
| 统计卡片 | 20px | Dashboard KPI、Analytics 统计 |
| 普通卡片 | 16px | 区域统计、列表容器 |
| 紧凑卡片 | 12px | 筛选条件、历史记录、Modal |

#### 8.2 代码规范

```jsx
// ✅ 正确：使用数组语法
<Row gutter={[16, 16]}>
  <Col span={6}>
    <Card>...</Card>
  </Col>
</Row>

// ❌ 错误：使用单一数值
<Row gutter={16}>
  <Col span={6}>
    <Card>...</Card>
  </Col>
</Row>

// ✅ 正确：使用 Tailwind 类
<div className="mb-6">主要区块</div>

// ❌ 错误：内联样式
<div style={{ marginBottom: 24 }}>主要区块</div>
```

## 🎬 动画 Token

动画定义在 `theme.css` 中，通过 `@keyframes` 实现：

| 动画名称 | 持续时间 | 用途 |
|---------|---------|------|
| `pulse-glow` | 2s | 脉冲呼吸灯（状态指示器） |
| `count-up` | 0.6s | 数字跳动动画 |
| `fade-in` | 0.3s | 淡入动画 |
| `slide-in` | 0.3s | 滑入动画 |
| `scan-line` | 3s | 扫描线效果 |
| `breathing` | 4s | 呼吸灯效果 |

**使用示例**：
```css
.status-dot {
  animation: pulse-glow 2s ease-in-out infinite;
}

.loading-scan {
  animation: scan-line 3s linear infinite;
}
```

## 📱 响应式断点

虽然没有定义为 CSS Variables，但建议使用以下断点：

| 断点 | 宽度 | 用途 |
|------|------|------|
| `sm` | < 640px | 手机 |
| `md` | 640px - 768px | 平板竖屏 |
| `lg` | 768px - 1024px | 平板横屏/小笔记本 |
| `xl` | > 1024px | 桌面 |

## 🔧 使用指南

### 1. 在 CSS 中使用

```css
.my-component {
  background: var(--color-bg-primary);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
}

.my-component:hover {
  border-color: var(--color-accent);
  box-shadow: var(--shadow-glow);
}
```

### 2. 在 Tailwind CSS 中使用

```jsx
// tailwind.config.js 已配置映射
<div className="bg-background-primary text-text-primary">
  <button className="bg-primary text-black hover:bg-primary-dark">
    按钮
  </button>
</div>
```

### 3. 在内联样式中使用

```jsx
<div style={{
  background: 'var(--color-bg-card)',
  borderColor: 'var(--color-border)',
  boxShadow: 'var(--shadow-md)'
}}>
  卡片内容
</div>
```

## 🎯 最佳实践

### ✅ 推荐做法

1. **始终使用语义化 Token**：
   ```css
   /* ✅ 好的做法 */
   color: var(--color-text-secondary);
   
   /* ❌ 避免 */
   color: #94a3b8;
   ```

2. **主题切换自动适配**：
   ```css
   /* ✅ 自动适配双主题 */
   background: var(--color-bg-primary);
   
   /* ❌ 硬编码颜色，主题切换失效 */
   background: #0a0e17;
   ```

3. **组合使用阴影和发光**：
   ```css
   .card {
     box-shadow: var(--shadow-md);
   }
   
   .card:hover {
     box-shadow: var(--shadow-lg), var(--shadow-glow);
   }
   ```

### ❌ 避免的做法

1. **不要覆盖 Token 值**：
   ```css
   /* ❌ 不要这样做 */
   :root {
     --color-accent: #ff0000; /* 覆盖全局 Token */
   }
   ```

2. **不要混用硬编码颜色**：
   ```css
   /* ❌ 不一致的颜色系统 */
   background: var(--color-bg-primary);
   color: #333; /* 应该用 var(--color-text-primary) */
   ```

## 📊 性能优化

- **CSS Variables 性能**：现代浏览器原生支持，性能优秀
- **主题切换**：通过 `[data-theme]` 属性切换，无闪烁
- **包大小**：所有 Token 定义仅增加约 1KB (未压缩)

## 🔍 Token 统计

- **总 Token 数量**：100+
- **暗色主题 Token**：40+
- **亮色主题 Token**：40+
- **动画定义**：6 个
- **布局 Token**：3 个

---

**维护者**：前端团队  
**最后更新**：2026-04-06  
**相关文档**：
- [前端架构信息](../spec/AI2AI/前端架构信息.md)
- [README.md UI/UX 章节](../README.md#-uiux-亮点)
