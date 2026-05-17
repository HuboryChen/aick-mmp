import { renderHook, act } from '@testing-library/react';
import '@testing-library/jest-dom';

// ==================== Mocks ====================

jest.mock('../../api/videoWallConfig', () => ({
  videoWallConfigApi: {
    getPresets: jest.fn(),
    createPreset: jest.fn(),
    updatePreset: jest.fn(),
    deletePreset: jest.fn(),
    reorderPresets: jest.fn(),
    setDefaultPreset: jest.fn(),
    applyPreset: jest.fn(),
  },
}));

import useVideoWallPresets from '../useVideoWallPresets';
import { videoWallConfigApi } from '../../api/videoWallConfig';
import { BUILT_IN_PRESETS } from '../../components/VideoWall/builtInPresets';

// ==================== Tests ====================

describe('useVideoWallPresets', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.restoreAllMocks();
    jest.spyOn(Storage.prototype, 'getItem').mockReturnValue(null);
    jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {});
    jest.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => {});
    // Default API mock returns
    videoWallConfigApi.getPresets.mockResolvedValue({ data: [] });
    videoWallConfigApi.createPreset.mockResolvedValue({ data: { id: 1, presetName: 'Test', layout: '4', quality: '720p', bitrate: 2048, cameraIds: [], isDefault: false } });
    videoWallConfigApi.updatePreset.mockResolvedValue({ data: { id: 1, presetName: 'Updated', layout: '9', quality: '1080p', bitrate: 4096, cameraIds: [], isDefault: false } });
    videoWallConfigApi.deletePreset.mockResolvedValue({});
    videoWallConfigApi.reorderPresets.mockResolvedValue({});
    videoWallConfigApi.setDefaultPreset.mockResolvedValue({});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  // ==================== Initial State ====================

  it('returns empty presets array and null activePresetId initially', () => {
    const { result } = renderHook(() => useVideoWallPresets());

    expect(result.current.presets).toEqual([]);
    expect(result.current.activePresetId).toBeNull();
    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  // ==================== getAllPresets ====================

  it('getAllPresets returns builtInPresets combined with user presets', () => {
    const { result } = renderHook(() => useVideoWallPresets());

    // Initially should only have built-in presets
    const initialPresets = result.current.getAllPresets();
    expect(initialPresets).toHaveLength(BUILT_IN_PRESETS.length);
    expect(initialPresets).toEqual(BUILT_IN_PRESETS);

    // Add a user preset
    act(() => {
      result.current.setPresets([
        { id: 1, presetName: 'My Preset', isBuiltIn: false },
      ]);
    });

    const allPresets = result.current.getAllPresets();
    expect(allPresets).toHaveLength(BUILT_IN_PRESETS.length + 1);
    expect(allPresets[BUILT_IN_PRESETS.length].presetName).toBe('My Preset');
  });

  // ==================== createPreset ====================

  it('createPreset creates preset via API and adds to list', async () => {
    const { result } = renderHook(() => useVideoWallPresets());

    const apiResponse = {
      id: 10,
      presetName: 'My Custom',
      layout: '9',
      quality: '1080p',
      bitrate: 4096,
      cameraIds: [],
      isDefault: false,
    };
    videoWallConfigApi.createPreset.mockResolvedValue({ data: apiResponse });

    await act(async () => {
      const created = await result.current.createPreset('My Custom', {
        layout: '9',
        quality: '1080p',
        bitrate: 4096,
      });
      expect(created).toEqual(apiResponse);
    });

    expect(result.current.presets).toHaveLength(1);
    expect(result.current.presets[0].presetName).toBe('My Custom');
    expect(result.current.presets[0].layout).toBe('9');
    expect(localStorage.setItem).toHaveBeenCalled();
  });

  it('createPreset applies fallback defaults when config is partial', async () => {
    const { result } = renderHook(() => useVideoWallPresets());

    const apiResponse = {
      id: 11,
      presetName: 'Partial',
      layout: '4',
      quality: '720p',
      bitrate: 2048,
      cameraIds: [],
      isDefault: false,
    };
    videoWallConfigApi.createPreset.mockResolvedValue({ data: apiResponse });

    await act(async () => {
      await result.current.createPreset('Partial', { layout: '4' });
    });

    // The API call should have used fallbacks for missing fields
    const callArg = videoWallConfigApi.createPreset.mock.calls[0][0];
    expect(callArg.quality).toBe('720p');
    expect(callArg.bitrate).toBe(2048);
  });

  // ==================== updatePreset ====================

  it('updatePreset updates preset via API', async () => {
    const { result } = renderHook(() => useVideoWallPresets());

    // Seed initial preset
    act(() => {
      result.current.setPresets([
        { id: 1, presetName: 'Original', layout: '4', quality: '720p', bitrate: 2048, isBuiltIn: false },
      ]);
    });

    const updatedData = {
      id: 1,
      presetName: 'Updated',
      layout: '9',
      quality: '1080p',
      bitrate: 4096,
      cameraIds: [],
      isDefault: false,
    };
    videoWallConfigApi.updatePreset.mockResolvedValue({ data: updatedData });

    await act(async () => {
      const result2 = await result.current.updatePreset(1, { presetName: 'Updated', layout: '9' });
      expect(result2).toEqual(updatedData);
    });

    expect(result.current.presets).toHaveLength(1);
    expect(result.current.presets[0].presetName).toBe('Updated');
    expect(result.current.presets[0].layout).toBe('9');
  });

  it('updatePreset throws on non-existent preset', async () => {
    const { result } = renderHook(() => useVideoWallPresets());

    await act(async () => {
      await expect(result.current.updatePreset(999, {})).rejects.toThrow('预设不存在或不可编辑');
    });
  });

  // ==================== deletePreset ====================

  it('deletePreset removes preset from list', async () => {
    const { result } = renderHook(() => useVideoWallPresets());

    act(() => {
      result.current.setPresets([
        { id: 1, presetName: 'Preset A', isBuiltIn: false },
        { id: 2, presetName: 'Preset B', isBuiltIn: false },
      ]);
    });

    await act(async () => {
      await result.current.deletePreset(1);
    });

    expect(result.current.presets).toHaveLength(1);
    expect(result.current.presets[0].id).toBe(2);
  });

  it('deletePreset clears activePresetId when deleting active preset', async () => {
    const { result } = renderHook(() => useVideoWallPresets());

    act(() => {
      result.current.setPresets([
        { id: 1, presetName: 'Active', isBuiltIn: false },
        { id: 2, presetName: 'Other', isBuiltIn: false },
      ]);
      result.current.setActivePresetId(1);
    });

    await act(async () => {
      await result.current.deletePreset(1);
    });

    expect(result.current.activePresetId).toBeNull();
  });

  it('deletePreset does not clear activePresetId when deleting non-active preset', async () => {
    const { result } = renderHook(() => useVideoWallPresets());

    act(() => {
      result.current.setPresets([
        { id: 1, presetName: 'Active', isBuiltIn: false },
        { id: 2, presetName: 'To Delete', isBuiltIn: false },
      ]);
      result.current.setActivePresetId(1);
    });

    await act(async () => {
      await result.current.deletePreset(2);
    });

    expect(result.current.activePresetId).toBe(1);
  });

  it('deletePreset throws on non-existent preset', async () => {
    const { result } = renderHook(() => useVideoWallPresets());

    await act(async () => {
      await expect(result.current.deletePreset(999)).rejects.toThrow('预设不存在或不可删除');
    });
  });

  // ==================== reorderPresets ====================

  it('reorderPresets reorders presets via API', async () => {
    const { result } = renderHook(() => useVideoWallPresets());

    act(() => {
      result.current.setPresets([
        { id: 1, presetName: 'First' },
        { id: 2, presetName: 'Second' },
        { id: 3, presetName: 'Third' },
      ]);
    });

    await act(async () => {
      await result.current.reorderPresets([{ id: 3 }, { id: 1 }, { id: 2 }]);
    });

    expect(result.current.presets).toHaveLength(3);
    expect(result.current.presets[0].id).toBe(3);
    expect(result.current.presets[0].sortOrder).toBe(0);
    expect(result.current.presets[1].id).toBe(1);
    expect(result.current.presets[1].sortOrder).toBe(1);
    expect(result.current.presets[2].id).toBe(2);
    expect(result.current.presets[2].sortOrder).toBe(2);
    expect(videoWallConfigApi.reorderPresets).toHaveBeenCalledWith([3, 1, 2]);
  });

  // ==================== Helper Functions ====================

  it('isBuiltInPreset returns true for presets with isBuiltIn === true', () => {
    const { result } = renderHook(() => useVideoWallPresets());

    expect(result.current.isBuiltInPreset({ isBuiltIn: true })).toBe(true);
    expect(result.current.isBuiltInPreset({ isBuiltIn: false })).toBe(false);
    expect(result.current.isBuiltInPreset({})).toBe(false);
    expect(result.current.isBuiltInPreset(null)).toBe(false);
  });

  it('canEditPreset returns false for built-in presets, true for user presets', () => {
    const { result } = renderHook(() => useVideoWallPresets());

    expect(result.current.canEditPreset({ isBuiltIn: true })).toBe(false);
    expect(result.current.canEditPreset({ isBuiltIn: false })).toBe(true);
    expect(result.current.canEditPreset(BUILT_IN_PRESETS[0])).toBe(false);
  });

  it('canDeletePreset returns false for built-in presets, true for user presets', () => {
    const { result } = renderHook(() => useVideoWallPresets());

    expect(result.current.canDeletePreset({ isBuiltIn: true })).toBe(false);
    expect(result.current.canDeletePreset({ isBuiltIn: false })).toBe(true);
    expect(result.current.canDeletePreset(BUILT_IN_PRESETS[0])).toBe(false);
  });

  // ==================== Error Handling ====================

  it('createPreset handles API errors gracefully', async () => {
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    const { result } = renderHook(() => useVideoWallPresets());

    videoWallConfigApi.createPreset.mockRejectedValue(new Error('Network error'));

    await act(async () => {
      try {
        await result.current.createPreset('Fail', {});
      } catch (e) {
        // Expected to throw
      }
    });

    expect(result.current.error).toBe('创建预设失败');
    expect(result.current.presets).toHaveLength(0);
    expect(result.current.isLoading).toBe(false);

    consoleErrorSpy.mockRestore();
  });

  it('deletePreset handles API errors gracefully', async () => {
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    const { result } = renderHook(() => useVideoWallPresets());

    act(() => {
      result.current.setPresets([
        { id: 1, presetName: 'Test', isBuiltIn: false },
      ]);
    });

    videoWallConfigApi.deletePreset.mockRejectedValue(new Error('Network error'));

    await act(async () => {
      try {
        await result.current.deletePreset(1);
      } catch (e) {
        // Expected to throw
      }
    });

    expect(result.current.error).toBe('删除预设失败');
    // Presets should remain unchanged
    expect(result.current.presets).toHaveLength(1);

    consoleErrorSpy.mockRestore();
  });

  // ==================== setAsDefaultPreset ====================

  it('setAsDefaultPreset updates isDefault flag', async () => {
    const { result } = renderHook(() => useVideoWallPresets());

    act(() => {
      result.current.setPresets([
        { id: 1, presetName: 'A', isDefault: false, isBuiltIn: false },
        { id: 2, presetName: 'B', isDefault: false, isBuiltIn: false },
      ]);
    });

    await act(async () => {
      await result.current.setAsDefaultPreset(1);
    });

    const preset1 = result.current.presets.find(p => p.id === 1);
    const preset2 = result.current.presets.find(p => p.id === 2);
    expect(preset1.isDefault).toBe(true);
    expect(preset2.isDefault).toBe(false);
    expect(videoWallConfigApi.setDefaultPreset).toHaveBeenCalledWith(1);
  });

  // ==================== localStorage Sync ====================

  it('savePresetsToLocalStorage stores presets', () => {
    const { result } = renderHook(() => useVideoWallPresets());
    const presets = [{ id: 1, presetName: 'Saved', isBuiltIn: false }];

    act(() => {
      result.current.savePresetsToLocalStorage(presets);
    });

    expect(localStorage.setItem).toHaveBeenCalledWith(
      'VIDEO_WALL_PRESETS',
      JSON.stringify(presets)
    );
  });
});
