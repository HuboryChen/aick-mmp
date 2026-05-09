## Context

### 背景

AICK-MMP 前端采用 **Industrial Command Center** 设计风格，使用 Antd Design 组件库 + Tailwind CSS 进行样式扩展。当前系统中存在多种卡片组件：

| 卡片类型 | 当前状态 | 所在页面 |
|----------|----------|----------|
| IndustrialStatCard | padding: 20px | Dashboard |
| RegionStatsCard / CdnStatsCard / AlertStatsCard | padding: 16px | Dashboard |
| IndustrialCard | padding: 16px, border-radius: 12px | 通用 |
| VideoCard | 无边框，gutter: [8, 8] | VideoWall |
| Antd Card (列表) | header/body 不同 padding | AlertList, Dashboard |

### 当前问题

```
问题分布：
├── gutter 语法不统一
│   ├── [16, 16] (Tailwind 数组语法) → Dashboard, Analytics
│   ├── gutter={16} (Antd 单一数值) → CameraDiscovery, AlertList, AlertManagement
│   └── [8, 8] (特殊值) → VideoWall
│
├── 间距写法不统一
│   ├── Tailwind 类: mb-4, mb-6
│   └── 内联样式: style={{ marginBottom: 16 }}
│
└── Card padding 不一致
    ├── 统计卡片: 20px
    ├── 普通卡片: 16px
    └── 紧凑卡片: 12px (但未标准化)
```

## Goals / Non-Goals

**Goals:**
- 统一全系统卡片布局间距规范
- 建立可复用的间距 Token 和组件规范
- 提升页面视觉一致性和可维护性
- 支持响应式布局适配

**Non-Goals:**
- 不重构页面整体布局结构
- 不修改卡片组件的样式主题（颜色、边框等）
- 不创建新的 Card 组件（仅规范化使用方式）
- 不涉及后端或数据库变更

## Decisions

### Decision 1: gutter 值统一为 [16, 16]

**选择**：所有页面统一使用 `gutter={[16, 16]}`

**原因**：
- 16px 是 Tailwind 默认 spacing 基准值
- 兼顾可读性和空间利用率
- 与当前大多数页面一致

**替代方案**：
| 方案 | 优点 | 缺点 |
|------|------|------|
| [8, 8] | 更紧凑 | 与大多数页面不一致，VideoWall 改回 [8, 8] 会割裂体验 |
| [12, 12] | 居中 | 非标准值，与 Tailwind 不对齐 |
| **[16, 16]** ✓ | 标准值，与现有多数页面一致 | 无 |

### Decision 2: 使用 Tailwind 类替代内联样式

**选择**：所有间距使用 Tailwind 类（`mb-4`、`mb-6` 等）

**原因**：
- 符合项目技术栈规范（Tailwind 配置完整）
- 响应式支持更便捷
- 代码可读性和维护性更好

**替代方案**：
| 方案 | 优点 | 缺点 |
|------|------|------|
| 继续使用内联样式 | 无迁移成本 | 响应式支持差，与 Tailwind 混用 |
| CSS Modules | 隔离性好 | 引入复杂度，项目未使用此方案 |
| **Tailwind 类** ✓ | 响应式、代码一致 | 需要统一规范 |

### Decision 3: Card padding 按类型分级

**选择**：

| Card 类型 | padding | 使用场景 |
|-----------|---------|----------|
| 统计卡片 | 20px | Dashboard KPI、Analytics 统计 |
| 普通卡片 | 16px | 区域统计、列表容器、详情面板 |
| 紧凑卡片 | 12px | 筛选条件、历史记录、Modal 内 |

**原因**：
- 视觉层级分明：统计卡片（重要）用更大 padding
- 密度适配：紧凑场景用更小 padding 容纳更多信息

**替代方案**：
| 方案 | 缺点 |
|------|------|
| 全部统一 16px | 统计卡片视觉权重不足，紧凑场景空间浪费 |
| 全部统一 20px | 紧凑场景过于松散 |
| **分级** ✓ | 根据场景灵活适配 |

### Decision 4: 视频墙不再特殊化 gutter

**选择**：VideoWall gutter 从 [8, 8] 改为 [16, 16]

**原因**：
- 与全系统保持一致
- 视频之间的间距可通过 Card 内部 padding 调整，而非依赖 gutter
- 视频墙实际需要的紧密度应通过视频容器 aspect ratio 实现，而非间距

**替代方案**：
| 方案 | 缺点 |
|------|------|
| 保持 [8, 8] 特殊化 | 破坏系统一致性，VideoWall 与其他页面割裂 |
| **统一 [16, 16]** ✓ | 需要重新评估视频墙视觉效果 |

### Decision 5: 区块间距层级

**选择**：

| 层级 | Tailwind | 像素 | 场景 |
|------|----------|------|------|
| 第一层 | `mb-6` | 24px | 页面主要区块之间 |
| 第二层 | `mb-4` | 16px | 区块内卡片行之间、筛选与内容间 |
| 第三层 | `mt-2` / `mb-2` | 8px | 标签页之间、表格操作栏 |
| 组件内 | `mt-3` / `mb-1` | 12px / 4px | 卡片内部元素 |

## Risks / Trade-offs

### Risk 1: VideoWall 视觉效果变化
**风险**：视频墙间距从 8px 增大到 16px，可能导致视频区域缩小

**缓解**：
1. 调整后截图对比评估
2. 如需更紧凑的视频墙，通过调整视频容器宽高比实现，而非 gutter
3. 预留 8px 版本供参考对比

### Risk 2: 历史代码惯性
**风险**：开发者可能习惯性使用内联样式或旧值

**缓解**：
1. 在 design-tokens.md 中明确标注规范
2. 代码审查时检查间距规范
3. 可考虑 ESLint 规则检测（可选）

### Risk 3: 响应式效果不确定
**风险**：移动端适配需要实际设备测试

**缓解**：
1. 从断点 640px 开始适配（Tailwind xs）
2. 先在小范围页面测试响应式
3. 保留调整空间

### Trade-off: 短期成本 vs 长期收益

| | 短期 | 长期 |
|---|------|------|
| **成本** | 修改 7 个页面，约 7 小时工作量 | 维护统一规范 |
| **收益** | 视觉一致性提升 | 新增页面有章可循，代码可维护性提升 |

**结论**：长期收益显著，值得投入。

## Migration Plan

### 实施顺序

```
Phase 1: 基础规范（约 2 小时）
├── 1.1 更新 theme.css，添加统一 Card 样式类
├── 1.2 更新 design-tokens.md，补充间距规范
└── 1.3 验证规范正确性

Phase 2: 页面修改（约 4 小时）
├── 2.1 VideoWall gutter 调整
├── 2.2 CameraDiscovery 内联样式替换
├── 2.3 EdgeNodeManagement 内联样式替换
├── 2.4 Dashboard gutter/padding 统一
├── 2.5 Analytics gutter 统一
├── 2.6 AlertList gutter 统一
└── 2.7 AlertManagement gutter 统一

Phase 3: 验证与优化（约 1 小时）
├── 3.1 响应式测试
├── 3.2 视觉效果检查
└── 3.3 文档更新
```

### 回滚策略

如需回滚，可通过 Git revert 恢复。由于是纯样式变更，无数据依赖，回滚风险低。

## Open Questions

| 问题 | 状态 | 说明 |
|------|------|------|
| VideoWall 实际需要多紧的间距？ | 待验证 | 需调整后截图确认 |
| 响应式断点是否需要更细粒度？ | 待定 | 根据实际效果决定 |
| 是否需要 ESLint 规则强制规范？ | 可选 | 可作为后续改进项 |
