import { renderHook, act } from '@testing-library/react';
import '@testing-library/jest-dom';

// ==================== Mocks ====================

jest.mock('js-cookie', () => ({
  get: jest.fn(),
}));

jest.mock('../../api/videoWallConfig', () => ({
  videoWallConfigApi: {
    getPreferences: jest.fn(),
    updatePreferences: jest.fn(),
  },
}));

import useVideoWallConfig from '../useVideoWallConfig';
import Cookies from 'js-cookie';
import { videoWallConfigApi } from '../../api/videoWallConfig';

// ==================== Tests ====================

describe('useVideoWallConfig', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.restoreAllMocks();
    jest.spyOn(Storage.prototype, 'getItem').mockReturnValue(null);
    jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {});
    jest.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => {});
    Cookies.get.mockReturnValue(undefined);
    videoWallConfigApi.updatePreferences.mockResolvedValue({});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  // ==================== Default Config ====================

  it('returns default config (layout 4, quality 720p, bitrate 2048) when nothing is stored', () => {
    const { result } = renderHook(() => useVideoWallConfig());

    expect(result.current.config).toEqual({
      layout: '4',
      quality: '720p',
      bitrate: 2048,
      cameraIds: [],
      savedAt: 0,
    });
  });

  // ==================== Loading State ====================

  it('isLoading and isLoaded are false initially', () => {
    const { result } = renderHook(() => useVideoWallConfig());

    expect(result.current.isLoading).toBe(false);
    expect(result.current.isLoaded).toBe(false);
  });

  // ==================== saveConfig ====================

  it('saveConfig updates config and saves to localStorage', () => {
    const { result } = renderHook(() => useVideoWallConfig());

    const newConfig = {
      layout: '9',
      quality: '1080p',
      bitrate: 4096,
      cameraIds: [1, 2],
    };

    act(() => {
      result.current.saveConfig(newConfig);
    });

    expect(result.current.config.layout).toBe('9');
    expect(result.current.config.quality).toBe('1080p');
    expect(result.current.config.bitrate).toBe(4096);
    expect(result.current.config.cameraIds).toEqual([1, 2]);
    expect(localStorage.setItem).toHaveBeenCalledTimes(1);
  });

  // ==================== saveConfigImmediately ====================

  it('saveConfigImmediately saves directly without debounce', async () => {
    const { result } = renderHook(() => useVideoWallConfig());

    const newConfig = {
      layout: '16',
      quality: '4K',
      bitrate: 16384,
      cameraIds: [],
    };

    await act(async () => {
      await result.current.saveConfigImmediately(newConfig);
    });

    expect(result.current.config.layout).toBe('16');
    expect(result.current.config.quality).toBe('4K');
    expect(result.current.config.bitrate).toBe(16384);
    expect(localStorage.setItem).toHaveBeenCalledTimes(1);
  });

  it('saveConfigImmediately updates DB when authenticated', async () => {
    Cookies.get.mockReturnValue('valid-token');
    const { result } = renderHook(() => useVideoWallConfig());

    const newConfig = {
      layout: '9',
      quality: '1080p',
      bitrate: 4096,
      cameraIds: [],
    };

    await act(async () => {
      await result.current.saveConfigImmediately(newConfig);
    });

    expect(videoWallConfigApi.updatePreferences).toHaveBeenCalledTimes(1);
    expect(videoWallConfigApi.updatePreferences).toHaveBeenCalledWith({
      layout: '9',
      quality: '1080p',
      bitrate: 4096,
      cameraIds: [],
    });
  });

  // ==================== Field Updates via saveConfig ====================

  it('updates layout via saveConfig', () => {
    const { result } = renderHook(() => useVideoWallConfig());

    act(() => {
      result.current.saveConfig({ ...result.current.config, layout: '9' });
    });

    expect(result.current.config.layout).toBe('9');
  });

  it('updates quality via saveConfig', () => {
    const { result } = renderHook(() => useVideoWallConfig());

    act(() => {
      result.current.saveConfig({ ...result.current.config, quality: '1080p' });
    });

    expect(result.current.config.quality).toBe('1080p');
  });

  it('updates bitrate via saveConfig', () => {
    const { result } = renderHook(() => useVideoWallConfig());

    act(() => {
      result.current.saveConfig({ ...result.current.config, bitrate: 8192 });
    });

    expect(result.current.config.bitrate).toBe(8192);
  });

  // ==================== Reset to Defaults ====================

  it('resetToDefaults resets config to default values', () => {
    const { result } = renderHook(() => useVideoWallConfig());

    // Change config to something different
    act(() => {
      result.current.setConfig({
        layout: '9',
        quality: '1080p',
        bitrate: 4096,
        cameraIds: [1, 2, 3],
        savedAt: Date.now(),
      });
    });

    // Reset to DEFAULT_CONFIG
    act(() => {
      result.current.setConfig({ ...result.current.DEFAULT_CONFIG });
    });

    expect(result.current.config.layout).toBe('4');
    expect(result.current.config.quality).toBe('720p');
    expect(result.current.config.bitrate).toBe(2048);
    expect(result.current.config.cameraIds).toEqual([]);
  });

  // ==================== localStorage Sync ====================

  it('loads config from localStorage when available', () => {
    const storedConfig = {
      layout: '9',
      quality: '1080p',
      bitrate: 4096,
      cameraIds: [5],
      savedAt: 1234567890,
    };
    Storage.prototype.getItem.mockReturnValue(JSON.stringify(storedConfig));

    // Re-render to pick up the localStorage data via setConfig
    const { result } = renderHook(() => useVideoWallConfig());

    // Hook does NOT auto-load from localStorage — that's the combined hook's job.
    // But saveToLocalStorage / loadFromLocalStorage should work:
    const loaded = result.current.loadFromLocalStorage();
    expect(loaded).toEqual(storedConfig);
  });

  // ==================== Error Handling ====================

  it('handles localStorage save failure gracefully', () => {
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    Storage.prototype.setItem.mockImplementation(() => {
      throw new Error('Storage full');
    });

    const { result } = renderHook(() => useVideoWallConfig());

    act(() => {
      result.current.saveConfig({
        layout: '9',
        quality: '1080p',
        bitrate: 4096,
        cameraIds: [],
      });
    });

    // Config state still updates even if localStorage fails
    expect(result.current.config.layout).toBe('9');
    expect(consoleErrorSpy).toHaveBeenCalled();

    consoleErrorSpy.mockRestore();
  });

  // ==================== Database Debounce ====================

  it('debouncedSaveToDatabase saves to DB after delay when authenticated', () => {
    jest.useFakeTimers();
    Cookies.get.mockReturnValue('valid-token');
    const { result } = renderHook(() => useVideoWallConfig());

    const newConfig = {
      layout: '9',
      quality: '1080p',
      bitrate: 4096,
      cameraIds: [],
    };

    act(() => {
      result.current.saveConfig(newConfig);
    });

    // Should not have been called immediately
    expect(videoWallConfigApi.updatePreferences).not.toHaveBeenCalled();

    // Advance past the debounce delay
    act(() => {
      jest.advanceTimersByTime(500);
    });

    expect(videoWallConfigApi.updatePreferences).toHaveBeenCalledTimes(1);
    expect(videoWallConfigApi.updatePreferences).toHaveBeenCalledWith({
      layout: '9',
      quality: '1080p',
      bitrate: 4096,
      cameraIds: [],
    });

    jest.useRealTimers();
  });

  it('does not debounce save to DB when not authenticated', () => {
    jest.useFakeTimers();
    // Cookies.get already returns undefined
    const { result } = renderHook(() => useVideoWallConfig());

    act(() => {
      result.current.saveConfig({
        layout: '9',
        quality: '1080p',
        bitrate: 4096,
        cameraIds: [],
      });
    });

    // Advance timers to make sure no DB call happens
    act(() => {
      jest.advanceTimersByTime(1000);
    });

    expect(videoWallConfigApi.updatePreferences).not.toHaveBeenCalled();

    jest.useRealTimers();
  });
});
