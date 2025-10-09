package com.aick.mmp.central.controller;

import com.aick.mmp.central.service.SystemSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/settings")
public class SystemSettingsController {

    private final SystemSettingsService systemSettingsService;

    @Autowired
    public SystemSettingsController(SystemSettingsService systemSettingsService) {
        this.systemSettingsService = systemSettingsService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getSystemSettings() {
        Map<String, Object> settings = systemSettingsService.getSystemSettings();
        return ResponseEntity.ok(settings);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateSystemSettings(@RequestBody Map<String, Object> settings) {
        systemSettingsService.updateSystemSettings(settings);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{key}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Map<String, String>> getSetting(@PathVariable String key) {
        String value = systemSettingsService.getSetting(key);
        return ResponseEntity.ok(Collections.singletonMap("value", value));
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateSetting(@PathVariable String key, @RequestBody Map<String, String> payload) {
        systemSettingsService.updateSetting(key, payload.get("value"));
        return ResponseEntity.ok().build();
    }
}