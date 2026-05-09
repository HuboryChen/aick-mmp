# Tasks: Video Wall Settings Drawer

## 0. Built-in Presets Definition

- [ ] 0.1 Define hardcoded built-in presets in frontend (单屏监控, 四分屏, 九宫格)
- [ ] 0.2 Add `isBuiltIn` flag to preset data structure
- [ ] 0.3 Ensure built-in presets are excluded from edit/delete operations

## 1. Backend Data Layer

- [ ] 1.1 Create VideoWallPreset entity with JPA annotations
- [ ] 1.2 Create VideoWallPreferences entity with JPA annotations
- [ ] 1.3 Create VideoWallPresetRepository extending JpaRepository
- [ ] 1.4 Create VideoWallPreferencesRepository extending JpaRepository

## 2. Backend Service Layer

- [ ] 2.1 Create VideoWallConfigService interface
- [ ] 2.2 Implement VideoWallConfigServiceImpl with CRUD operations
- [ ] 2.3 Add preset management methods (create, update, delete, apply, setDefault, reorder)
- [ ] 2.4 Add preference management methods (get, update)

## 3. Backend Controller Layer

- [ ] 3.1 Create VideoWallConfigController with REST endpoints
- [ ] 3.2 Implement GET /api/video-wall/preferences
- [ ] 3.3 Implement PUT /api/video-wall/preferences
- [ ] 3.4 Implement GET /api/video-wall/presets
- [ ] 3.5 Implement POST /api/video-wall/presets (create)
- [ ] 3.6 Implement PUT /api/video-wall/presets/{id} (update)
- [ ] 3.7 Implement DELETE /api/video-wall/presets/{id} (delete)
- [ ] 3.8 Implement POST /api/video-wall/presets/{id}/apply
- [ ] 3.9 Implement POST /api/video-wall/presets/{id}/set-default
- [ ] 3.10 Implement PUT /api/video-wall/presets/reorder (batch update sort order)

## 4. Frontend API Layer

- [ ] 4.1 Create api/videoWallConfig.js with API definitions
- [ ] 4.2 Add preference API methods (getPreferences, updatePreferences)
- [ ] 4.3 Add preset API methods (getPresets, createPreset, updatePreset, deletePreset, applyPreset, setDefaultPreset, reorderPresets)

## 5. Frontend Hook Layer

- [ ] 5.1 Create hooks/useVideoWallSettings.js
- [ ] 5.2 Implement state management (config, presets, activePresetId, builtInPresets)
- [ ] 5.3 Implement loadPreferences with database-first, localStorage fallback, built-in preset fallback
- [ ] 5.4 Implement dual-sync savePreferences (localStorage immediate + database debounced)
- [ ] 5.5 Implement applyPreset, createPreset, updatePreset, deletePreset, resetToDefaults
- [ ] 5.6 Implement preset reordering logic

## 6. Frontend Components - Selectors

- [ ] 6.1 Create components/VideoWall/LayoutSelector.jsx (simplified 2x2 grid)
- [ ] 6.2 Create components/VideoWall/QualitySelector.jsx (Radio Group + Slider)
- [ ] 6.3 Create components/VideoWall/PresetSelector.jsx with create/delete/select/edit
- [ ] 6.4 Add drag-and-drop reordering to PresetSelector using @dnd-kit

## 7. Frontend Components - Drawer

- [ ] 7.1 Create components/VideoWall/VideoWallSettingsDrawer.jsx
- [ ] 7.2 Implement Drawer layout with sections
- [ ] 7.3 Add footer with Reset and Done buttons
- [ ] 7.4 Integrate PresetSelector, LayoutSelector, QualitySelector
- [ ] 7.5 Create VideoWallSettingsDrawer.css styles

## 8. Frontend Page Integration

- [ ] 8.1 Modify pages/VideoWall.jsx to add settings button to header
- [ ] 8.2 Replace Card components in header with drawer trigger button
- [ ] 8.3 Integrate VideoWallSettingsDrawer component
- [ ] 8.4 Connect drawer state management to useVideoWallSettings hook
- [ ] 8.5 Update VideoWall.css if needed for new layout

## 9. Database Migration

- [ ] 9.1 Create migration for video_wall_presets table
- [ ] 9.2 Create migration for video_wall_preferences table
- [ ] 9.3 Add foreign key constraints
- [ ] 9.4 Add unique constraint on preset name per user
- [ ] 9.5 Add sort_order column to video_wall_presets

## 10. Testing & Polish

- [ ] 10.1 Test drawer open/close functionality
- [ ] 10.2 Test preset creation, selection, deletion
- [ ] 10.3 Test preset editing (name and configuration)
- [ ] 10.4 Test drag-and-drop preset reordering
- [ ] 10.5 Test built-in presets (display, non-editable, non-deletable)
- [ ] 10.6 Test preference persistence (database + localStorage dual sync)
- [ ] 10.7 Test fallback behavior (database unavailable)
- [ ] 10.8 Test cross-device sync (login on different device)
- [ ] 10.9 Add loading states and error handling
- [ ] 10.10 Verify responsive behavior on different screen sizes
- [ ] 10.11 Test manual adjustment overrides preset (activePresetId = null)
