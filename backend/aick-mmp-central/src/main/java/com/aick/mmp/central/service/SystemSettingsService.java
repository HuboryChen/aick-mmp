package com.aick.mmp.central.service;

import java.util.Map;

public interface SystemSettingsService {
    Map<String, Object> getSystemSettings();
    void updateSystemSettings(Map<String, Object> settings);
    String getSetting(String key);
    void updateSetting(String key, String value);
}