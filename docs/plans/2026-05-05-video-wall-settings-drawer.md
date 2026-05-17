# Video Wall Settings Drawer - Implementation Plan

**Date**: 2026-05-05
**Change**: `video-wall-settings-drawer`
**Status**: Ready for Implementation

---

## 1. Overview

This document outlines the implementation plan for separating video wall layout and quality settings into a dedicated settings drawer, with preset management and user preference persistence.

### Key Decisions

| # | Decision | Choice |
|---|----------|--------|
| 1 | Preset limit | No limit |
| 2 | Built-in presets | Hardcoded, cannot be deleted |
| 3 | Preference vs Preset | Preferences take priority over presets |
| 4 | Data sync | Dual sync (localStorage + database, debounced) |
| 5 | Preset sorting | Drag-to-reorder |
| 6 | Preset editing | Full edit (name + configuration) |

---

## 2. Data Loading Flow

```
Page Load
    │
    ├─► [Authenticated?] ─Yes─► Load from Database
    │                                    │
    │                              Success?
    │                                    │
    │                           Yes──► Apply User Preferences
    │                                    │
    │                           No───► Load from localStorage
    │                                          │
    │                                    Success?
    │                                          │
    │                                 Yes──► Apply localStorage
    │                                          │
    │                                 No───► Apply Built-in Preset
    │                                              (四分屏)
    │
    └─► [Not Authenticated] ──► Load from localStorage
                                          │
                                    Success?
                                          │
                                 Yes──► Apply localStorage
                                          │
                                 No───► Apply Built-in Preset
```

---

## 3. Data Sync Strategy

### Save Flow

```
User Changes Setting
        │
        ▼
Save to localStorage (immediate)
        │
        ▼
Debounce 500ms ──► Save to Database
```

### Conflict Resolution

| Scenario | Resolution |
|----------|------------|
| localStorage vs Database on login | Database wins (remote wins) |
| Preset vs Manual adjustment | Manual adjustment wins (preset deactivated) |

---

## 4. Built-in Presets

```javascript
const BUILT_IN_PRESETS = [
  {
    id: 'sys-1',
    name: '单屏监控',
    layout: '1',
    quality: '1080p',
    bitrate: 4096,
    cameraIds: [],
    isDefault: false,
    isBuiltIn: true
  },
  {
    id: 'sys-2',
    name: '四分屏',
    layout: '4',
    quality: '720p',
    bitrate: 2048,
    cameraIds: [],
    isDefault: true,
    isBuiltIn: true
  },
  {
    id: 'sys-3',
    name: '九宫格',
    layout: '9',
    quality: '480p',
    bitrate: 1024,
    cameraIds: [],
    isDefault: false,
    isBuiltIn: true
  }
];
```

---

## 5. Component Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    VideoWall Page                           │
├─────────────────────────────────────────────────────────────┤
│  Header: [Title] [Camera Count] [⚙️ Settings] [🔄] [⛶]   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│              Video Grid (Full Width)                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              VideoWallSettingsDrawer (320px)                │
├─────────────────────────────────────────────────────────────┤
│  📐 布局设置                                                │
│  [1x1] [2x2] [3x3] [4x4]   ← visual grid icons           │
│                                                             │
│  ─────────────────────────────────────────────────────────  │
│                                                             │
│  🎬 画质设置                                                │
│  ○ 480p  ● 720p  ○ 1080p    ← large touch targets         │
│  码率: ────●──── 2048 kbps                                 │
│                                                             │
│  ─────────────────────────────────────────────────────────  │
│                                                             │
│  ⭐ 预设管理                                                 │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ [拖拽排序区域 - 用户预设]                            │   │
│  │  • 预设1 (可编辑/删除)                              │   │
│  │  • 预设2 (可编辑/删除)                              │   │
│  ├─────────────────────────────────────────────────────┤   │
│  │ [内置预设 - 固定位置]                               │   │
│  │  ⭐ 单屏监控 (不可编辑/删除)                        │   │
│  │  ⭐ 四分屏 (不可编辑/删除)                          │   │
│  │  ⭐ 九宫格 (不可编辑/删除)                          │   │
│  └─────────────────────────────────────────────────────┘   │
│  [+ 新建预设]                                              │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│  [重置]                              [完成]                 │
└─────────────────────────────────────────────────────────────┘
```

**Hierarchy rationale**: Layout is the primary user action (changing what they see) and placed first. Quality is secondary refinement. Presets are tertiary (saving/reusing configurations).

---

## 6. API Endpoints

### Preferences

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/video-wall/preferences` | Get user preferences |
| PUT | `/api/video-wall/preferences` | Update user preferences |

### Presets

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/video-wall/presets` | Get all user presets |
| POST | `/api/video-wall/presets` | Create new preset |
| PUT | `/api/video-wall/presets/{id}` | Update preset |
| DELETE | `/api/video-wall/presets/{id}` | Delete preset |
| POST | `/api/video-wall/presets/{id}/apply` | Apply preset |
| POST | `/api/video-wall/presets/{id}/set-default` | Set as default |
| PUT | `/api/video-wall/presets/reorder` | Batch update sort order |

---

## 7. Database Schema

### video_wall_presets

```sql
CREATE TABLE video_wall_presets (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    preset_name     VARCHAR(50) NOT NULL,
    layout          VARCHAR(10) NOT NULL DEFAULT '4',
    quality         VARCHAR(10) NOT NULL DEFAULT '720p',
    bitrate         INT DEFAULT 2048,
    camera_ids      JSON,
    is_default      BOOLEAN DEFAULT FALSE,
    sort_order      INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_preset_name (user_id, preset_name)
);
```

### video_wall_preferences

```sql
CREATE TABLE video_wall_preferences (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL UNIQUE,
    layout          VARCHAR(10) NOT NULL DEFAULT '4',
    quality         VARCHAR(10) NOT NULL DEFAULT '720p',
    bitrate         INT DEFAULT 2048,
    camera_ids      JSON,
    auto_apply      BOOLEAN DEFAULT TRUE,
    last_preset_id  BIGINT,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

---

## 8. Frontend File Structure

```
frontend/src/
├── api/
│   └── videoWallConfig.js              # API definitions
├── hooks/
│   └── useVideoWallSettings.js         # State management hook
├── components/
│   └── VideoWall/
│       ├── VideoWallSettingsDrawer.jsx # Main drawer component
│       ├── PresetSelector.jsx          # Preset list with drag-drop
│       ├── PresetEditModal.jsx         # Edit preset modal
│       ├── LayoutSelector.jsx          # Simplified 2x2 grid
│       ├── QualitySelector.jsx         # Radio + Slider
│       └── VideoWallSettingsDrawer.css # Styles
└── pages/
    └── VideoWall.jsx                   # Modified main page
```

---

## 9. Implementation Order

### Phase 1: Backend (Tasks 1-3)
1. Create entities and repositories
2. Implement service layer
3. Create REST controller

### Phase 2: Frontend Foundation (Tasks 4-5)
4. Create API layer
5. Implement useVideoWallSettings hook

### Phase 3: UI Components (Tasks 6-7)
6. Create selector components
7. Build drawer component

### Phase 4: Integration (Task 8)
8. Integrate into VideoWall page

### Phase 5: Polish (Tasks 9-10)
9. Database migration
10. Testing and refinement

---

## 10. Key Behaviors

### Manual Adjustment Deactivates Preset

```javascript
// When user manually changes any setting
const setLayout = (layout) => {
  setConfig(prev => ({ ...prev, layout }));
  setActivePresetId(null); // Preset no longer "active"
};
```

### Dual Sync Save

```javascript
const saveConfig = debounce(async (config) => {
  // Immediate: localStorage
  localStorage.setItem('VIDEO_WALL_CONFIG', JSON.stringify(config));
  
  // Debounced: database
  await videoWallConfigApi.updatePreferences(config);
}, 500);
```

### Built-in Preset Protection

```javascript
// In PresetSelector
const canEdit = (preset) => !preset.isBuiltIn;
const canDelete = (preset) => !preset.isBuiltIn;
const canDrag = (preset) => !preset.isBuiltIn;
```

---

## 11. Interaction States

Every component must handle all relevant states. No default/fallback UI is acceptable.

### Settings Drawer — State Coverage

| Feature | Loading | Empty | Error | Success | Saving |
|---------|---------|-------|-------|---------|--------|
| **All settings (initial load)** | Skeleton shimmer for each section (card outlines + shimmer animation) | N/A | Error banner: red-tinted card at drawer top with error message + "重试" button. Drawer stays open. | Data displayed normally | N/A |
| **Layout selector** | Ghost buttons (gray outlined grid icons) | N/A | Per-section inline error (see above) | Selected layout highlighted with accent border | Spin on save → ✓ "已保存" |
| **Quality selector** | Ghost radio buttons + ghost slider track | N/A | Per-section inline error | Radio selected, slider positioned | Spin on save → ✓ "已保存" |
| **Preset list** | Skeleton rows (3 lines) | "暂无自定义预设" card with explanation text and prominent [+ 创建预设] CTA | Inline error badge on preset section | Presets displayed with drag handles | Spin on save |
| **Save action** | N/A | N/A | Error banner + button re-enabled | Button shows ✓ "已保存" for 1.5s, then resets to normal state | Button shows spinner, inputs disabled |

### Empty State Design

When user has no custom presets:
- Show a subtle ghost area the same height as a populated preset list would be
- Ghost icon inside (a preset outline)
- Text: "暂无自定义预设 — 调整布局和画质后点击下方创建"
- CTA: [+ 创建预设] primary button

### Error State Design

```
┌─────────────────────────────────────────┐
│ ⚠ 加载失败                        [重试] │  ← red-tinted banner, bg rgba(255,71,87,0.1)
│ 网络错误，请稍后重试                      │     border-left: 3px solid --status-offline
└─────────────────────────────────────────┘
```

### Saving Feedback

```
Before save:  [完成]           ← GlowButton, normal state
Saving:       [◌ 保存中...]   ← Button spins, inputs disabled
Success:      [✓ 已保存]      ← 1.5s, then reset to normal state
```

### Toast Notification

On every successful save (debounced auto-save or explicit save):
- Toast: "设置已保存" with --status-online checkmark icon
- Auto-dismiss after 2s
- Position: top-right (consistent with rest of app)

---

## 12. User Journey & Motion Design

### Drawer Open/Close Animation

| Action | Animation | Timing |
|--------|-----------|--------|
| Drawer opens | Slide in from right (`slide-in` from theme.css) | 0.3s ease-out |
| Backdrop overlay | Fade from transparent to 60% opacity dark overlay | 0.3s ease-out |
| Drawer closes | Slide out to right | 0.2s ease-in |
| Backdrop dismiss | Fade out | 0.2s ease-in |

### Layout Hover Preview

When user hovers over a layout grid button (e.g., [3x3]):
- The video wall grid momentarily shows a dimmed preview of that layout
- The hovered button gets a subtle glow (`--shadow-glow`)
- On click, the preview becomes the active layout

### Preset Apply Feedback

When user clicks a preset (including built-in):
1. Preset card highlights with accent border + subtle glow
2. All settings auto-fill to match the preset
3. Toast appears: "已应用预设: {预设名称}" — auto-dismiss 2s
4. Config saves automatically (debounced 500ms)

### First Visit Experience

- On 3rd visit to Video Wall page (tracked via localStorage counter):
  - Settings gear icon gets a subtle pulse-glow badge ("新功能")
  - Single tooltip on first drawer open: "可在此调整布局、画质或保存预设"

### Save Flow Visual Timeline

```
User changes setting
  │
  ├─► localStorage save (instant, no visual feedback)
  │
  ├─► Debounce 500ms
  │     │
  │     ├─► DB save starts → button shows spinner
  │     │
  │     ├─► Success → button shows ✓ "已保存" → toast → reset
  │     │
  │     └─► Error → inline error banner + button re-enabled
  │
  └─► Close drawer (X or click outside) → no confirmation needed
       (auto-save ensures no data loss)
```

---

## 13. Design System Alignment

All visual elements must use existing design tokens and components. No hard-coded colors, fonts, or spacing.

### Drawer & Container

| Element | Token / Component | Value |
|---------|------------------|-------|
| Drawer container | Ant Design `Drawer` (dark theme) | bg `--color-bg-elevated #242b3d`, glassmorphism: backdrop-filter blur(10px) |
| Drawer width | Desktop: 320px, Mobile: 100vw | - |
| Drawer backdrop | `--color-bg-primary` at 60% opacity | `rgba(10, 14, 23, 0.6)` |
| Section dividers | `--color-border` | `rgba(255, 255, 255, 0.1)` |
| Section header text | `--color-text-secondary` + font stack | `#94a3b8`, Inter/Noto Sans SC |

### Layout Selector

| Element | Token / Component | Value |
|---------|------------------|-------|
| Grid buttons (selected) | bg `--color-accent`, border glow | `#00d4ff` bg, `--shadow-glow` |
| Grid buttons (unselected) | bg `--color-bg-card`, border `--color-border` | `#1a1f2e`, `rgba(255,255,255,0.1)` |
| Grid icons | Tiny SVG grid previews (not text like `[1x1]`) | 16x16px, accent colored when selected |
| Hover state on buttons | `--color-border-hover` + `--shadow-glow` | `rgba(255,255,255,0.2)`, see theme.css |
| Touch target min | 44x44px | - |

### Quality Selector

| Element | Token / Component | Value |
|---------|------------------|-------|
| Radio buttons (selected) | `--color-accent` fill | `#00d4ff` |
| Radio buttons (unselected) | `--color-text-muted` border | `#64748b` |
| Radio labels | `--color-text-primary` text | `#ffffff` |
| Bitrate slider track | Ant Design `Slider` customized | Active track: `--gradient-accent`, handle: `--color-accent` with glow |
| Slider inactive | `--color-bg-secondary` | `#141820` |

### Preset Management

| Element | Token / Component | Value |
|---------|------------------|-------|
| Preset card (default) | `IndustrialCard` pattern | `--color-bg-card #1a1f2e`, `--gradient-card`, glassmorphism |
| Preset card (active/selected) | `--color-accent` left border + `--shadow-glow` | 2px `#00d4ff` left border, glow shadow |
| Preset card (hover) | `--color-border-hover` | `rgba(255,255,255,0.2)` |
| Drag handle icon | `HolderOutlined` (Ant Design) | `--color-text-muted #64748b` |
| Built-in preset badge | Small icon "⭐" → Ant Design `StarFilled` | `--status-warning #fbbf24` |
| Empty preset area | Ghost outline + CTA | `--color-bg-secondary`, `--color-accent` for CTA |
| Delete button (hover) | `--status-offline` color | `#ff4757` |

### Buttons

| Element | Token / Component | Value |
|---------|------------------|-------|
| "完成" Done button | `GlowButton` | `--gradient-accent` bg, `--shadow-glow` on hover |
| "重置" Reset button | Default Ant Design button, dark theme | Transparent bg, `--color-text-secondary` text |
| "+ 新建预设" | `GlowButton` ghost variant | Transparent bg, `--color-accent` border + text |
| Delete icon (hover) | `--status-offline` | `#ff4757` |

### Typography

| Element | Token | Value |
|---------|-------|-------|
| Drawer title | `--color-text-primary` | `#ffffff`, font-size 18px, font-weight 600 |
| Section headers | `--color-text-secondary` | `#94a3b8`, font-size 13px, uppercase tracking-wider |
| Preset names | `--color-text-primary` | `#ffffff`, font-size 14px |
| Preset descriptions | `--color-text-muted` | `#64748b`, font-size 12px |
| All text | Font stack | `'Inter', 'Noto Sans SC', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif` |

### Spacing

All spacing follows the established hierarchy in design-tokens.md:
- Section padding: 16px (card standard)
- Section gap: 24px (mb-6)
- Between items in a section: 8px (mb-2)
- Card internal padding: 12px (compact card)
- Drawer padding: 20px

### Icons

Use Ant Design icons throughout (not emoji):
- Settings gear: `SettingOutlined`
- Preset star: `StarFilled`
- Layout: `AppstoreOutlined` / custom grid SVG
- Quality: `HdOutlined`
- Drag handle: `HolderOutlined`
- Delete: `DeleteOutlined`
- Edit: `EditOutlined`
- Add: `PlusOutlined`

---

## 14. Responsive & Accessibility

### Responsive Behavior

| Viewport | Drawer behavior |
|----------|----------------|
| Desktop (≥1024px) | 320px right drawer, overlays video wall |
| Tablet (640-1024px) | 320px right drawer, full-height |
| Mobile (<640px) | Full-screen drawer (100vw × 100vh), slides from right |
| Mobile layout selector | 2-column grid (2x2 per row instead of 4 inline) |
| Mobile quality selector | Full-width radio items (vertical stack) |
| Mobile presets | Accordion-collapsible preset section (collapsed by default) |
| Mobile buttons | "重置" and "完成" fixed at bottom, full-width |

### Responsive Drawer: Mobile (Full-Screen)

```
┌──────────────────────────────────┐
│ [←] 设置                    [完成] │  ← header with back arrow and save
├──────────────────────────────────┤
│                                  │
│ 📐 布局设置                       │
│ [1x1] [2x2]                      │  ← 2-column grid
│ [3x3] [4x4]                      │
│                                  │
│ ──────────────────────────────   │
│                                  │
│ 🎬 画质设置                       │
│ ○ 480p                           │  ← vertical radio list
│ ● 720p                           │
│ ○ 1080p                          │
│ 码率: ────●──── 2048 kbps       │
│                                  │
│ ──────────────────────────────   │
│                                  │
│ ⭐ 预设管理                       │
│ [▼ 展开]                         │  ← accordion, collapsed by default
│                                  │
├──────────────────────────────────┤
│ [重置]            [✓ 完成]        │  ← fixed bottom bar
└──────────────────────────────────┘
```

### Accessibility Requirements

| Requirement | Implementation |
|-------------|----------------|
| Drawer ARIA | `role="dialog"`, `aria-modal="true"`, `aria-labelledby="drawer-title"` |
| Focus trap | Tab cycling stays within drawer while open |
| Close on Escape | `onKeyDown` handler for Escape key |
| Close button label | `aria-label="关闭设置"` on X button |
| Drag-and-drop keyboard | Each preset has ↑ (Move up) and ↓ (Move down) icon buttons alongside the drag handle. Visible on focus within the preset section or always visible. |
| Preset actions | Edit/Delete buttons have `aria-label="编辑 {预设名}"` / `aria-label="删除 {预设名}"` |
| Saving announcement | `aria-live="polite"` region announces "保存中" / "已保存" / "保存失败" |
| Toast notifications | `role="status"` with `aria-live="polite"` |
| Touch targets | All interactive elements ≥44×44px |
| Color contrast | All text meets AA minimum (already AAA per design tokens) |
| Layout grid buttons | `role="radio"` with `aria-checked` and `aria-label="3x3 布局"` |
| Quality radios | `role="radio"` group with `aria-label="画质选择"` |
| Slider | Native Ant Design `Slider` handles keyboard nav by default |
| Mobile keyboards | No text inputs in drawer (except preset name modal) |

---

## 15. Implementation Tasks

Synthesized from this design review. Each task derives from a specific finding.

- [ ] **T1 (P1, human: ~4h / CC: ~30min)** — Drawer — Reorder section hierarchy to Layout → Quality → Presets
  - Surfaced by: Pass 1 (Information Architecture) — Preset section placed first, but layout changes are the primary user action
  - Files: `frontend/src/components/VideoWall/VideoWallSettingsDrawer.jsx`
  - Verify: Drawer opens with layout selector at the top, preset management below quality

- [ ] **T2 (P1, human: ~3h / CC: ~20min)** — Drawer — Implement all interaction states (loading skeleton, empty presets, error banner, save spinner + checkmark)
  - Surfaced by: Pass 2 (Interaction State Coverage) — Zero states specified in original plan
  - Files: `VideoWallSettingsDrawer.jsx`, `PresetSelector.jsx`, `VideoWallSettingsDrawer.css`
  - Verify: States render correctly: loading → skeleton, empty presets → CTA, network error → retry banner, save → spin → checkmark

- [ ] **T3 (P2, human: ~2h / CC: ~15min)** — Drawer — Add motion design: drawer slide-in/out (0.3s), layout hover preview, preset apply highlight + toast, first-visit badge
  - Surfaced by: Pass 3 (User Journey) — No emotional arc or motion specified
  - Files: `VideoWallSettingsDrawer.jsx`, `VideoWallSettingsDrawer.css`
  - Verify: Drawer animates in/out, settings toast appears on save, first-time badge visible on 3rd visit

- [ ] **T4 (P1, human: ~2h / CC: ~10min)** — Components — Map all visual elements to design tokens (see Design System Alignment table, section 13)
  - Surfaced by: Pass 5 (Design System Alignment) — No token references in original plan
  - Files: All component CSS files
  - Verify: No hardcoded colors, all use `var(--color-*)` tokens

- [ ] **T5 (P2, human: ~3h / CC: ~45min)** — Drawer — Implement responsive layout: full-screen drawer on mobile, accordion presets, fixed bottom buttons
  - Surfaced by: Pass 6 (Responsive) — 320px drawer unusable on mobile
  - Files: `VideoWallSettingsDrawer.jsx`, `VideoWallSettingsDrawer.css`
  - Verify: 320px on desktop, full-screen on mobile (<640px), presets collapsible

- [ ] **T6 (P2, human: ~2h / CC: ~15min)** — Drawer — Add accessibility: ARIA roles, focus trap, keyboard reorder (up/down buttons for presets), escape-to-close
  - Surfaced by: Pass 6 (Accessibility) — Drag-drop is keyboard-inaccessible
  - Files: `PresetSelector.jsx`, `VideoWallSettingsDrawer.jsx`
  - Verify: Tab cycles within drawer, Escape closes, preset up/down buttons functional via keyboard

- [ ] **T7 (P3, human: ~30min / CC: ~5min)** — Components — Replace emoji placeholders in ASCII mockup with actual Ant Design icons (StarFilled, SettingOutlined, DeleteOutlined, etc.)
  - Surfaced by: Pass 4 (AI Slop) — Emoji icons are planning artifacts, not implementation spec
  - Files: All VideoWall component JSX files
  - Verify: No emoji in rendered output, all icons from `@ant-design/icons`

_No new tasks from Pass 7 (Unresolved Decisions) — all were resolved during this review._

---

## 16. Design Review Completion Summary

```
  +====================================================================+
  |         DESIGN PLAN REVIEW — COMPLETION SUMMARY                    |
  +====================================================================+
  | System Audit         | design-tokens.md exists. No DESIGN.md.      |
  |                      | UI scope: Video Wall Settings Drawer.       |
  | Step 0               | Initial rating: 4/10. Focus: all 7 passes. |
  | Pass 1  (Info Arch)  | 3/10 → 8/10 after reorder                  |
  | Pass 2  (States)     | 1/10 → 9/10 after states spec              |
  | Pass 3  (Journey)    | 2/10 → 8/10 after motion spec              |
  | Pass 4  (AI Slop)    | 8/10 → 9/10 after icon spec                |
  | Pass 5  (Design Sys) | 5/10 → 10/10 after alignment table         |
  | Pass 6  (Responsive) | 1/10 → 9/10 after responsive + a11y       |
  | Pass 7  (Decisions)  | 4 resolved, 0 deferred                     |
  +--------------------------------------------------------------------+
  | NOT in scope         | Multi-device sync conflict resolution       |
  | What already exists  | design-tokens.md, theme.css, IndustrialCard |
  | Implementation tasks | 7 tasks proposed (4 P1, 2 P2, 1 P3)        |
  | Approved Mockups     | Not generated (design API key unavailable)  |
  | Decisions made       | 4 added to plan                             |
  | Decisions deferred   | 0                                           |
  | Overall design score | 4/10 → 8.5/10                               |
  +====================================================================+
```

---

## 17. Engineering Review Findings

Reviewed against the implemented code (all ~2700 lines are live). The implementation is structurally sound but has specific gaps from the design review and testing.

### Implementation Status vs Plan

| Aspect | Plan Says | Code Does | Gap? |
|--------|-----------|-----------|------|
| Drawer section order | Layout → Quality → Presets | Preset → Layout → Quality | **YES** — needs reorder |
| CSS theme | Dark industrial tokens | Light Ant Design defaults | **YES** — hardcoded `#fff`, `#fafafa`, `#1890ff` |
| Loading/error states | Skeleton + error banner | Not rendered (`isLoading`, `error` state unused in UI) | **YES** |
| Tests | Specified in design review | None | **YES** |
| useVideoWallSettings hook | Not reviewed | 663-line god hook | **YES** — split recommended |
| Keyboard accessibility | Up/down buttons for presets | @dnd-kit KeyboardSensor configured | Partial — lacking explicit up/down buttons |
| Mobile responsive | Full-screen drawer | `@media (max-width: 576px) { width: 100% }` | Code does this ✅ |
| Drag-and-drop | @dnd-kit | @dnd-kit used | ✅ |
| API endpoints | 8 endpoints | 8 endpoints implemented | ✅ |
| DB schema | 2 tables + indexes | 2 tables + indexes | ✅ |

### Key Findings

**1. Architecture**: The implementation follows clean MVC/component patterns. Backend service layer has proper validation, transaction boundaries, and ownership checks. Frontend component separation is clean.

**2. Code Quality**: `useVideoWallSettings` hook at 663 lines bundles config management, preset CRUD, localStorage sync, and debounced DB persistence — should be split into 2 hooks.

**3. Design Token Compliance**: CSS files hardcode Ant Design light theme colors (white backgrounds, blue accent `#1890ff`). Must use `--color-bg-card`, `--color-accent #00d4ff`, etc. from theme.css.

**4. Tests**: Zero test coverage for 15 new files. Backend service and controller need unit tests. Frontend hook needs tests for the state machine (sync logic, error handling, CRUD).

### Updated Implementation Tasks

- [ ] **T1 (P1, human: ~1h / CC: ~10min)** — Drawer — Reorder sections to Layout → Quality → Presets
  - Surfaced by: Eng Review + Design Review — code still uses original order
  - Files: `frontend/src/components/VideoWall/VideoWallSettingsDrawer.jsx`
  - Verify: Layout selector renders above quality selector, preset management at bottom

- [ ] **T2 (P1, human: ~2h / CC: ~30min)** — CSS — Replace hardcoded light colors with dark theme design tokens
  - Surfaced by: Eng Review — CSS uses `#fafafa`, `#1890ff`, `#fff` instead of `--color-bg-card`, `--color-accent`, `--color-bg-elevated`
  - Files: `VideoWallSettingsDrawer.css`, `LayoutSelector.jsx`, `QualitySelector.jsx`, `PresetSelector.jsx`
  - Verify: All colors via `var(--color-*)`, drawer matches dark industrial theme

- [ ] **T3 (P1, human: ~1h / CC: ~15min)** — Drawer — Add loading skeleton + error banner UI
  - Surfaced by: Eng Review — `isLoading` and `error` state tracked but never rendered
  - Files: `VideoWallSettingsDrawer.jsx`, `VideoWallSettingsDrawer.css`
  - Verify: Ant Design Skeleton shown during loading, Alert banner shown on error

- [ ] **T4 (P2, human: ~1h / CC: ~15min)** — Hook — Split `useVideoWallSettings` into `useVideoWallConfig` + `useVideoWallPresets`
  - Surfaced by: Eng Review — 663-line hook bundles orthogonal concerns
  - Files: `frontend/src/hooks/useVideoWallSettings.js`
  - Verify: Two hooks, each <350 lines, same external API via re-export

- [ ] **T5 (P1, human: ~4h / CC: ~2h)** — Tests — Add backend + frontend unit tests
  - Surfaced by: Eng Review — zero test coverage for 15 new files
  - Files: `VideoWallConfigServiceImplTest.java`, `VideoWallConfigControllerTest.java`, `useVideoWallSettings.test.js`
  - Verify: Service layer >80% coverage (CRUD + validation + error paths), hook covers sync logic and preset operations

- [ ] **T6 (P2, human: ~1h / CC: ~10min)** — Drawer — Add motion: drawer slide-in, save spinner/checkmark, preset apply toast
  - Surfaced by: Design Review Pass 3 — user journey lacks emotional arc
  - Files: `VideoWallSettingsDrawer.jsx`, `VideoWallSettingsDrawer.css`
  - Verify: Drawer slides in (0.3s), save button shows spin → checkmark, toast on preset apply
