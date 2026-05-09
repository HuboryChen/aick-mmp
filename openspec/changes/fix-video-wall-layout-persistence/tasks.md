## 1. Fix useVideoWallSettings loading priority

- [x] 1.1 Modify `loadPreferences` in `useVideoWallSettings.js`: after loading DB data, merge with localStorage values, with localStorage taking precedence for `layout`, `quality`, `bitrate`, `cameraIds`
- [x] 1.2 Add timestamp field to saved config to enable future conflict resolution
- [x] 1.3 Ensure that when both DB and localStorage have data, the merge produces a complete config object (no missing fields)

## 2. Save config immediately on explicit user actions

- [x] 2.1 Add `saveConfigImmediately` function to `useVideoWallSettings` that calls `videoWallConfigApi.updatePreferences()` directly (without debounce)
- [x] 2.2 Modify `handleDone` in `VideoWallSettingsDrawer` to call `saveConfigImmediately` before closing the drawer
- [x] 2.3 Keep debounced save for preset operations and rapid adjustments (not removed, just not used for the explicit "完成" action)

## 3. Eliminate duplicate hook instance in Drawer

- [x] 3.1 Remove `useVideoWallSettings()` call from `VideoWallSettingsDrawer.jsx`
- [x] 3.2 Add props: `config`, `setLayout`, `setQuality`, `setBitrate`, `getAllPresets`, `activePresetId`, `applyPreset`, `createPreset`, `updatePreset`, `deletePreset`, `setAsDefaultPreset`, `reorderPresets`, `resetToDefaults`, `canEditPreset`, `canDeletePreset`, `isBuiltInPreset`
- [x] 3.3 Update `VideoWall.js` to pass all required props to `VideoWallSettingsDrawer`
- [x] 3.4 Update Drawer's `handleApplySettings` to call parent's `setLayout`/`setQuality`/`setBitrate` directly via props instead of its own hook instance
- [x] 3.5 Remove the `useEffect` sync from Drawer (line 57-64) since it no longer has its own hook config

## 4. Fix camera selection persistence

- [x] 4.1 Verify `setSelectedCameras` in `useVideoWallSettings` correctly saves `cameraIds` array to localStorage
- [x] 4.2 Verify on load that `cameraIds` from localStorage is properly restored and applied to `selectedCamerasState`
- [x] 4.3 Fix the `initialized` guard in VideoWall.js to properly restore camera selection after config load

## 5. Verify and test

- [x] 5.1 Test: change layout from 1 to 4, click "完成", refresh page — verify layout stays at 4
- [x] 5.2 Test: change layout from 4 to 9, click "完成", navigate to another page, navigate back — verify layout stays at 9
- [x] 5.3 Test: change quality from 720p to 1080p, click "完成", refresh — verify quality persists
- [x] 5.4 Test: assign cameras to specific cells, click "完成", refresh — verify cameras persist
- [x] 5.5 Test: apply a preset, navigate away and back — verify preset layout is applied
- [x] 5.6 Test: verify no console errors about duplicate saves or stale config
