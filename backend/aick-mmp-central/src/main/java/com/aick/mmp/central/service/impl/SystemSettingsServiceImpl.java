package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.service.SystemSettingsService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SystemSettingsServiceImpl implements SystemSettingsService {
    
    // 模拟存储系统设置 - 实际项目中应该存储在数据库中
    private Map<String, Object> systemSettings = new HashMap<>();
    private Map<String, String> settings = new HashMap<>();
    
    public SystemSettingsServiceImpl() {
        // 初始化一些默认设置
        systemSettings.put("retentionDays", 30);
        systemSettings.put("maxStorageGB", 1000);
        systemSettings.put("enableAlerts", true);
        systemSettings.put("alertEmail", "admin@example.com");
        
        settings.put("timezone", "UTC");
        settings.put("language", "zh-CN");
    }

    @Override
    public Map<String, Object> getSystemSettings() {
        return systemSettings;
    }

    @Override
    public void updateSystemSettings(Map<String, Object> settings) {
        this.systemSettings.putAll(settings);
    }

    @Override
    public String getSetting(String key) {
        return settings.getOrDefault(key, "");
    }

    @Override
    public void updateSetting(String key, String value) {
        settings.put(key, value);
    }
}