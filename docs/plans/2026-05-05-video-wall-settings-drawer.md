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
│  ─────────────────────────────────────────────────────────  │
│                                                             │
│  📐 布局设置                                                │
│  [1x1] [2x2] [3x3] [4x4]                                  │
│                                                             │
│  ─────────────────────────────────────────────────────────  │
│                                                             │
│  🎬 画质设置                                                │
│  ○ 480p  ● 720p  ○ 1080p                                   │
│  码率: ────●──── 2048 kbps                                 │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│  [重置]                              [完成]                 │
└─────────────────────────────────────────────────────────────┘
```

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
