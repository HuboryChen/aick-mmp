## 1. 基础规范更新

- [x] 1.1 在 `theme.css` 中添加统一的 Card 样式类（industrial-card, stat-card, compact-card）
- [x] 1.2 在 `docs/design-tokens.md` 中补充布局间距规范章节（引用 `docs/frontend/card-layout-spacing-guide.md`）
- [x] 1.3 在 `docs/frontend/` 下创建 `card-layout-spacing-guide.md`（已完成，上一阶段）

## 2. VideoWall 调整

- [x] 2.1 将 `VideoWall.js` 中 `gutter={[8, 8]}` 改为 `gutter={[16, 16]}` 并添加响应式支持
- [ ] 2.2 验证视频墙视觉效果，如需调整视频容器宽高比以保持紧密度

## 3. CameraDiscovery 调整

- [x] 3.1 将网络扫描卡片区域的 `style={{ marginBottom: 16 }}` 替换为 `className="mb-4"`
- [x] 3.2 将扫描进度卡片的内联样式替换为 Tailwind 类
- [x] 3.3 将发现设备卡片的内联样式替换为 Tailwind 类
- [x] 3.4 将 Modal 表单中 `gutter={16}` 改为 `gutter={[16, 16]}`

## 4. EdgeNodeManagement 调整

- [x] 4.1 将搜索区域的 `style={{ marginBottom: 16 }}` 替换为 `className="mb-4"`
- [x] 4.2 将批量操作区域的 `style={{ marginBottom: 16, padding: '12px' }}` 替换为 `className="mb-4 p-3"`
- [ ] 4.3 统一 Modal 内 Card 使用 `size="small"`, `styles={{ body: { padding: 16 } }}`

## 5. Dashboard 调整

- [x] 5.1 统一 CdnStatsCard 内部 `gutter={16}` 改为 `gutter={[16, 16]}` 并添加响应式支持
- [x] 5.2 统一 AlertStatsCard 内部 `gutter={12}`, `gutter={16}` 改为 `gutter={[16, 16]}` 并添加响应式支持
- [x] 5.3 将 IndustrialStatCard 的 padding 保持 20px（已符合规范）
- [x] 5.4 将 RegionStatsCard, CdnStatsCard, AlertStatsCard 的 padding 统一为 16px

## 6. Analytics 调整

- [x] 6.1 验证所有 Row 组件使用 `gutter={[16, 16]}`（当前已符合规范）
- [x] 6.2 统一 StatsCard padding 为 20px
- [x] 6.3 统一设备状态/可靠性指标 Card padding 为 16px

## 7. AlertList 调整

- [x] 7.1 将统计卡片行的 `gutter={16}` 改为 `gutter={[16, 16]}` 并添加响应式支持
- [x] 7.2 将详情 Modal 内容的 `gutter={[16, 16]}` 保持（已符合规范）
- [x] 7.3 将 `style={{ marginTop: 16 }}` 替换为 `className="mt-4"`

## 8. AlertManagement 调整

- [x] 8.1 将 Modal 表单中所有 `gutter={16}` 改为 `gutter={[16, 16]}`
- [ ] 8.2 统一 Modal 内 Card 使用 `size="small"`, `styles={{ body: { padding: 16 } }}`

## 9. 响应式测试与验证

- [x] 9.1 在 Dashboard 添加响应式 gutter 支持
- [x] 9.2 在 Analytics 添加响应式 gutter 支持
- [x] 9.3 在 VideoWall 添加响应式 gutter 支持
- [ ] 9.4 测试移动端（< 640px）下的卡片间距
- [ ] 9.5 测试平板端（640-768px）下的卡片间距

## 10. 最终验证

- [ ] 10.1 截图对比所有修改页面的视觉效果
- [ ] 10.2 确认没有遗漏的内联间距样式
- [ ] 10.3 提交代码并更新相关文档
