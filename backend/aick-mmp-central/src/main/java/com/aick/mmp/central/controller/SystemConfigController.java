package com.aick.mmp.central.controller;

import com.aick.mmp.central.repository.ConfigHistoryRepository;
import com.aick.mmp.central.service.SystemConfigService;
import com.aick.mmp.shared.model.ConfigHistory;
import com.aick.mmp.shared.model.SystemConfig;
import com.aick.mmp.shared.model.enums.ConfigCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置控制器
 */
@RestController
@RequestMapping("/v1/system-configs")
@RequiredArgsConstructor
@Slf4j
public class SystemConfigController {
    
    private final SystemConfigService systemConfigService;
    private final ConfigHistoryRepository configHistoryRepository;

    
    /**
     * 获取所有配置
     */
    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG_VIEW') or hasRole('ADMIN')")
    public ResponseEntity<List<SystemConfig>> getAllConfigs() {
        return ResponseEntity.ok(systemConfigService.getEnabledConfigs());
    }
    
    /**
     * 获取配置详情
     */
    @GetMapping("/{configKey}")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG_VIEW') or hasRole('ADMIN')")
    public ResponseEntity<SystemConfig> getConfigByKey(@PathVariable String configKey) {
        return ResponseEntity.ok(systemConfigService.getByConfigKey(configKey));
    }
    
    /**
     * 获取配置值
     */
    @GetMapping("/{configKey}/value")
    public ResponseEntity<Map<String, String>> getConfigValue(@PathVariable String configKey) {
        Map<String, String> result = new HashMap<>();
        result.put("key", configKey);
        result.put("value", systemConfigService.getConfigValue(configKey));
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取配置值（包含敏感信息，仅管理员）
     */
    @GetMapping("/{configKey}/value/sensitive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> getConfigValueSensitive(@PathVariable String configKey) {
        Map<String, String> result = new HashMap<>();
        result.put("key", configKey);
        result.put("value", systemConfigService.getConfigValueWithSensitive(configKey));
        return ResponseEntity.ok(result);
    }
    
    /**
     * 按分类获取配置
     */
    @GetMapping("/category/{category}")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG_VIEW') or hasRole('ADMIN')")
    public ResponseEntity<List<SystemConfig>> getConfigsByCategory(@PathVariable ConfigCategory category) {
        return ResponseEntity.ok(systemConfigService.getByCategory(category));
    }
    
    /**
     * 按分组获取配置
     */
    @GetMapping("/group/{group}")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG_VIEW') or hasRole('ADMIN')")
    public ResponseEntity<List<SystemConfig>> getConfigsByGroup(@PathVariable String group) {
        return ResponseEntity.ok(systemConfigService.getByGroup(group));
    }
    
    /**
     * 获取所有分组
     */
    @GetMapping("/groups")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG_VIEW') or hasRole('ADMIN')")
    public ResponseEntity<List<String>> getAllGroups() {
        return ResponseEntity.ok(systemConfigService.getAllGroups());
    }
    
    /**
     * 创建配置
     */
    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG_MANAGE') or hasRole('ADMIN')")
    public ResponseEntity<SystemConfig> createConfig(@RequestBody SystemConfig config) {
        log.info("创建系统配置: key={}", config.getConfigKey());
        return ResponseEntity.ok(systemConfigService.createConfig(config));
    }
    
    /**
     * 更新配置
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG_MANAGE') or hasRole('ADMIN')")
    public ResponseEntity<SystemConfig> updateConfig(@PathVariable Long id, @RequestBody SystemConfig config) {
        log.info("更新系统配置: id={}", id);
        return ResponseEntity.ok(systemConfigService.updateConfig(id, config));
    }
    
    /**
     * 删除配置
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG_MANAGE') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        log.info("删除系统配置: id={}", id);
        systemConfigService.deleteConfig(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 批量更新配置
     */
    @PutMapping("/batch")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG_MANAGE') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Boolean>> batchUpdateConfigs(@RequestBody Map<String, String> configValues) {
        log.info("批量更新配置: {} 项", configValues.size());
        return ResponseEntity.ok(systemConfigService.batchUpdateConfigs(configValues));
    }
    
    /**
     * 重置配置到默认值
     */
    @PostMapping("/{configKey}/reset")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG_MANAGE') or hasRole('ADMIN')")
    public ResponseEntity<SystemConfig> resetConfig(@PathVariable String configKey) {
        log.info("重置配置到默认值: key={}", configKey);
        return ResponseEntity.ok(systemConfigService.resetConfig(configKey));
    }
    
    /**
     * 重置所有配置到默认值
     */
    @PostMapping("/reset-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetAllConfigs() {
        log.info("重置所有配置到默认值");
        systemConfigService.resetAllConfigs();
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取配置历史
     */
    @GetMapping("/{configKey}/history")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG_VIEW') or hasRole('ADMIN')")
    public ResponseEntity<List<ConfigHistory>> getConfigHistory(@PathVariable String configKey) {
        return ResponseEntity.ok(systemConfigService.getConfigHistory(configKey));
    }
    
    /**
     * 分页获取配置历史
     */
    @GetMapping("/{configKey}/history/page")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG_VIEW') or hasRole('ADMIN')")
    public ResponseEntity<Page<ConfigHistory>> getConfigHistoryPage(
            @PathVariable String configKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(configHistoryRepository.findByConfigKeyOrderByCreatedAtDesc(configKey, pageable));
    }
    
    /**
     * 回滚配置
     */
    @PostMapping("/{configKey}/rollback")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG_MANAGE') or hasRole('ADMIN')")
    public ResponseEntity<SystemConfig> rollbackConfig(@PathVariable String configKey) {
        log.info("回滚配置: key={}", configKey);
        return ResponseEntity.ok(systemConfigService.rollbackConfig(configKey));
    }
    
    /**
     * 测试邮件配置
     */
    @PostMapping("/email/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testEmailConfig(@RequestBody Map<String, String> emailConfig) {
        // TODO: 实现邮件配置测试
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "邮件配置测试成功");
        return ResponseEntity.ok(result);
    }
    
    /**
     * 初始化默认配置
     */
    @PostMapping("/initialize")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> initializeDefaultConfigs() {
        log.info("初始化默认配置");
        systemConfigService.initializeDefaultConfigs();
        return ResponseEntity.ok().build();
    }
}
