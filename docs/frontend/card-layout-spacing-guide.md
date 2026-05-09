# 卡片布局与间距设计规范

本文档定义了 AICK-MMP 项目中所有卡片组件的布局与间距标准。

## 📌 规范状态

| 项目 | 值 |
|------|-----|
| 状态 | 建议稿 |
| 版本 | 1.0 |
| 创建日期 | 2026-05-05 |

---

## 1. 间距系统

### 1.1 间距规格表

| Token (Tailwind) | 像素值 | 使用场景 |
|-----------------|--------|----------|
| `space-2` | 8px | 极度紧凑的布局（如视频墙） |
| `space-3` | 12px | 组件内部紧密元素 |
| `space-4` | 16px | 同一区块内卡片间距、Card 内边距 |
| `space-5` | 20px | 统计数据卡片内边距 |
| `space-6` | 24px | 页面主要区块之间间距 |
| `space-8` | 32px | 大区块分隔 |

### 1.2 间距使用规则

```
页面结构
├── 页面标题
│
├── 主要区块 A
│   └── Row (gutter={[16, 16]})
│       ├── Card 1
│       ├── Card 2
│       └── Card 3
│
├── 主要区块 B          ← mb-6 (24px)
│   └── Row (gutter={[16, 16]})
│       ├── Card 1
│       └── Card 2
│
└── 主要区块 C          ← mb-6 (24px)
```

---

## 2. Row/Col 网格系统

### 2.1 gutter 值规范

| 场景 | gutter 值 | Tailwind 写法 | 适用页面 |
|------|-----------|---------------|----------|
| **标准页面** | `[16, 16]` | `gutter={[16, 16]}` | Dashboard, Analytics, EdgeNodeManagement |
| **紧凑布局** | `[12, 12]` | `gutter={[12, 12]}` | 表单密集区域 |
| **视频墙** | `[16, 16]` | `gutter={[16, 16]}` | **视频墙改为与页面一致** |
| **Modal 表单** | `[16, 16]` | `gutter={[16, 16]}` | CameraDiscovery, AlertManagement |

### 2.2 统一规则

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
```

### 2.3 特殊场景

| 场景 | gutter 值 | 原因 |
|------|-----------|------|
| 视频墙 | `[16, 16]` | 统一标准，视频间距通过 Card 内部 padding 调整 |
| 表单密集区域 | `[12, 12]` | Modal 内表单行间距可适当收紧 |

---

## 3. 卡片组件规范

### 3.1 Card 类型与 Padding

| Card 类型 | padding | 使用场景 | 代码示例 |
|-----------|---------|----------|----------|
| **统计卡片** | 20px | Dashboard 顶部的 KPI 卡片 | `styles={{ body: { padding: 20 } }}` |
| **普通卡片** | 16px | 区域统计、列表容器 | `styles={{ body: { padding: 16 } }}` |
| **紧凑卡片** | 12px | 筛选条件、历史记录 | `styles={{ body: { padding: 12 } }}` |
| **列表卡片** | `header: 12px 20px, body: 12px` | 告警列表、活动列表 | 见下方示例 |

### 3.2 统一 Card 样式

```jsx
// styles/theme.css 中定义统一的 Card 样式

// 普通卡片
.ant-card.industrial-card {
  padding: 16px;
}

// 统计卡片
.ant-card.stat-card,
.ant-card.industrial-stat-card {
  padding: 20px;
}

// 紧凑卡片
.ant-card.compact-card {
  padding: 12px;
}

// 列表卡片
.ant-card.list-card {
  --card-header-padding: 12px 20px;
  --card-body-padding: 12px;
}
```

### 3.3 Card 组件使用示例

```jsx
// ✅ 统一使用方式
<Card 
  className="industrial-card"
  styles={{ body: { padding: 16 } }}
>
  内容
</Card>

// ✅ 列表卡片特殊样式
<Card
  className="industrial-card list-card"
  styles={{
    header: { padding: '12px 20px' },
    body: { padding: 12 }
  }}
>
  内容
</Card>
```

---

## 4. 区块间距规范

### 4.1 页面区块层级

```
页面
├── 第一层：页面标题 (mb-6)
│
├── 第二层：主要功能区块 (mb-6)
│   ├── 统计卡片区
│   ├── 图表区
│   └── 列表区
│
├── 第三层：次要功能区块 (mb-4)
│   ├── 筛选条件
│   ├── 操作栏
│   └── 详情面板
│
└── 第四层：组件内部元素 (mt-2, mt-3, mb-2)
```

### 4.2 区块间距规则

| 层级 | Tailwind 类 | 像素值 | 使用位置 |
|------|-------------|--------|----------|
| 第一层 | `mb-6` | 24px | 页面标题下方、主要区块之间 |
| 第二层 | `mb-4` | 16px | 区块内卡片行之间、筛选条件与内容之间 |
| 第三层 | `mt-2` / `mb-2` | 8px | 标签页之间、表格操作栏 |
| 组件内 | `mt-3` / `mb-1` | 12px / 4px | 卡片内部元素间距 |

### 4.3 区块间距示例

```jsx
// ✅ 正确的页面结构
<div className="page-container">
  <PageHeader title="页面标题" />  {/* mb-6 在 PageHeader 内 */}

  {/* 主要区块 1 */}
  <section className="mb-6">
    <h3 className="mb-4">区块标题</h3>
    <Row gutter={[16, 16]}>
      <Col span={6}><Card>卡片1</Card></Col>
      <Col span={6}><Card>卡片2</Card></Col>
      <Col span={6}><Card>卡片3</Card></Col>
      <Col span={6}><Card>卡片4</Card></Col>
    </Row>
  </section>

  {/* 主要区块 2 */}
  <section className="mb-6">
    <h3 className="mb-4">区块标题</h3>
    <Row gutter={[16, 16]}>
      <Col span={8}><Card>卡片A</Card></Col>
      <Col span={8}><Card>卡片B</Card></Col>
      <Col span={8}><Card>卡片C</Card></Col>
    </Row>
  </section>
</div>
```

---

## 5. 响应式间距

### 5.1 响应式规则

| 屏幕尺寸 | 断点 | gutter 调整 | 区块间距调整 |
|----------|------|-------------|--------------|
| 手机 (< 640px) | `xs` | `[8, 8]` | `mb-4` |
| 平板 (640-768px) | `sm` | `[12, 12]` | `mb-4` |
| 小屏 (768-1024px) | `lg` | `[16, 16]` | `mb-6` |
| 大屏 (> 1024px) | `xl` | `[16, 16]` | `mb-6` |

### 5.2 响应式示例

```jsx
// ✅ 响应式 Row
<Row 
  gutter={[
    { xs: 8, sm: 12, lg: 16 },  // 水平间距
    { xs: 8, sm: 12, lg: 16 }   // 垂直间距
  ]}
>
  <Col xs={24} sm={12} lg={6}>
    <Card>...</Card>
  </Col>
</Row>

// ✅ 响应式区块间距
<section className="mb-4 xs:mb-4 lg:mb-6">
  内容
</section>
```

### 5.3 移动端优化

```css
/* index.css 或 theme.css */

@media (max-width: 640px) {
  .industrial-card {
    margin-bottom: 8px;
  }
  
  .page-section {
    margin-bottom: 16px;
  }
}
```

---

## 6. Modal 内间距

### 6.1 Modal Card 规范

| 场景 | Card size | padding | gutter |
|------|-----------|---------|--------|
| 普通 Modal | `size="small"` 或默认 | 16px | `[16, 16]` |
| 表单密集 Modal | `size="small"` | 12px | `[12, 12]` |
| 详情查看 Modal | `size="small"` | 16px | `[16, 16]` |

### 6.2 Modal 内间距示例

```jsx
// ✅ Modal 内表单布局
<Modal
  title="添加设备"
  width={600}
>
  <Card size="small" styles={{ body: { padding: 16 } }}>
    <Form layout="vertical">
      <Row gutter={[16, 16]}>
        <Col span={12}>
          <Form.Item label="名称">
            <Input />
          </Form.Item>
        </Col>
        <Col span={12}>
          <Form.Item label="类型">
            <Select />
          </Form.Item>
        </Col>
      </Row>
    </Form>
  </Card>
</Modal>

// ✅ Modal 内统计卡片
<Modal
  title="健康检查详情"
>
  <Row gutter={[16, 16]} className="mb-4">
    <Col span={8}>
      <Card size="small" styles={{ body: { padding: 16 } }}>
        <Statistic title="CPU" value={75} suffix="%" />
      </Card>
    </Col>
    <Col span={8}>
      <Card size="small" styles={{ body: { padding: 16 } }}>
        <Statistic title="内存" value={60} suffix="%" />
      </Card>
    </Col>
    <Col span={8}>
      <Card size="small" styles={{ body: { padding: 16 } }}>
        <Statistic title="磁盘" value={45} suffix="%" />
      </Card>
    </Col>
  </Row>
</Modal>
```

---

## 7. 特殊场景规范

### 7.1 视频墙

| 元素 | 值 | 说明 |
|------|-----|------|
| 视频网格 gutter | `[16, 16]` | **修改当前 [8, 8]** |
| 视频卡片内部间距 | 0 | 视频贴边 |
| 视频卡片边框 | 1px | 保持边框可见性 |
| Content padding | 16px | 页面边距 |

### 7.2 统计仪表盘

| 元素 | 值 | 说明 |
|------|-----|------|
| KPI 卡片 gutter | `[16, 16]` | 4 列 |
| KPI 卡片 padding | 20px | 稍大内边距 |
| 图表区域 gutter | `[16, 16]` | 与统计一致 |
| 图表 padding | 16px | 统一内边距 |

### 7.3 表单密集区域

| 元素 | 值 | 说明 |
|------|-----|------|
| 表单 Row gutter | `[12, 12]` | 更紧凑 |
| Form.Item margin | 0 | 使用 gutter 控制 |
| 按钮组间距 | `space-2` | 8px |

---

## 8. 代码规范

### 8.1 禁止的做法

```jsx
// ❌ 禁止：内联样式指定间距
<div style={{ marginBottom: 16 }}>

// ✅ 应该：使用 Tailwind 类
<div className="mb-4">

// ❌ 禁止：混用 gutter 语法
<Row gutter={16}>

// ✅ 应该：使用数组语法
<Row gutter={[16, 16]}>

// ❌ 禁止：不同页面使用不同 gutter
// 页面 A: gutter={[8, 8]}
// 页面 B: gutter={[16, 16]}

// ✅ 应该：统一间距
// 所有页面: gutter={[16, 16]}
```

### 8.2 推荐的做法

```jsx
// ✅ 推荐：使用公共组件
<CardGrid gutter={[16, 16]} className="mb-6">
  <StatCard title="在线设备" value={128} />
  <StatCard title="离线设备" value={12} />
  <StatCard title="告警数" value={5} />
  <StatCard title="带宽" value="1.2Gbps" />
</CardGrid>

// ✅ 推荐：使用一致的间距变量
<div className={{
  'mb-4': isCompactSection,
  'mb-6': !isCompactSection
}}>
```

---

## 9. 修复清单

根据当前代码分析，以下是需要修复的问题：

### 高优先级

| # | 问题 | 当前状态 | 目标状态 |
|---|------|----------|----------|
| 1 | VideoWall gutter 过紧 | `[8, 8]` | `[16, 16]` |
| 2 | CameraDiscovery 使用内联样式 | `style={{ marginBottom: 16 }}` | `className="mb-4"` |
| 3 | EdgeNodeManagement 使用内联样式 | `style={{ marginBottom: 16 }}` | `className="mb-4"` |
| 4 | 统计卡片 padding 不统一 | `padding: 20` (Dashboard) vs `padding: 16` (其他) | 统一为 20px |

### 中优先级

| # | 问题 | 当前状态 | 目标状态 |
|---|------|----------|----------|
| 5 | AlertList gutter 语法 | `gutter={16}` | `gutter={[16, 16]}` |
| 6 | AlertManagement gutter 语法 | `gutter={16}` | `gutter={[16, 16]}` |
| 7 | Dashboard 内部 gutter 不一致 | `gutter={12}`, `gutter={16}` 混用 | 统一 `[16, 16]` |
| 8 | 缺少响应式间距 | 无响应式处理 | 添加响应式断点 |

### 低优先级

| # | 问题 | 建议方案 |
|---|------|----------|
| 9 | 重复的 gutter 配置 | 抽取为公共组件 |
| 10 | Modal Card size 不一致 | 统一使用 `size="small"` |

---

## 10. 实施建议

### 阶段一：统一规范（预计 2 小时）

1. 更新 `theme.css`，添加统一的 Card 样式类
2. 修改 VideoWall gutter 值：`[8, 8]` → `[16, 16]`
3. 修改 CameraDiscovery 内联样式为 Tailwind 类
4. 修改 EdgeNodeManagement 内联样式为 Tailwind 类

### 阶段二：一致性修复（预计 3 小时）

1. 统一 AlertList gutter 语法
2. 统一 AlertManagement gutter 语法
3. 统一 Dashboard 内部 gutter
4. 统一统计卡片 padding

### 阶段三：响应式优化（预计 2 小时）

1. 为关键页面添加响应式 gutter
2. 添加移动端样式覆盖

---

## 附录：快速参考卡

```
┌─────────────────────────────────────────────────────────┐
│                    间距快速参考                          │
├─────────────────────────────────────────────────────────┤
│  gutter (Row):          [16, 16]                       │
│  区块间距 (section):     mb-6                           │
│  卡片行间距 (Row 内):    gutter 控制                     │
│  组件内间距:            mt-3 / mb-2                     │
│  Card padding (普通):   16px                           │
│  Card padding (统计):   20px                           │
│  Card padding (紧凑):   12px                           │
├─────────────────────────────────────────────────────────┤
│  禁止:                                           │
│  • 内联样式 margin/padding                        │
│  • gutter={16} (单一数值)                         │
│  • 随意变更 gutter 值                            │
└─────────────────────────────────────────────────────────┘
```

---

**维护者**：前端团队
**最后更新**：2026-05-05
**相关文档**：
- [design-tokens.md](./design-tokens.md) - 设计系统基础
- [前端架构信息](../spec/AI2AI/前端架构信息.md) - 前端架构
