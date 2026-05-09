package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.repository.ConfigHistoryRepository;
import com.aick.mmp.central.repository.SystemConfigRepository;
import com.aick.mmp.central.security.CurrentUserContext;
import com.aick.mmp.central.service.SystemConfigService;
import com.aick.mmp.shared.model.ConfigHistory;
import com.aick.mmp.shared.model.SystemConfig;
import com.aick.mmp.shared.model.enums.ConfigCategory;
import com.aick.mmp.shared.model.enums.ConfigValueType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigServiceImpl implements SystemConfigService {
    
    private final SystemConfigRepository systemConfigRepository;
    private final ConfigHistoryRepository configHistoryRepository;
    private final CurrentUserContext currentUserContext;
    
    // 默认配置项定义
    private static final List<SystemConfig> DEFAULT_CONFIGS = Arrays.asList(
        // 视频参数
        createConfigDef("video.default.quality", "默认画质", "视频参数", 
            ConfigValueType.SELECT, "auto,low,medium,high,ultra", "high", 1),
        createConfigDef("video.default.resolution", "默认分辨率", "视频参数",
            ConfigValueType.SELECT, "480p,720p,1080p,2k,4k", "1080p", 2),
        createConfigDef("video.default.framerate", "默认帧率", "视频参数",
            ConfigValueType.NUMBER, null, "30", 3),
        
        // 录像设置
        createConfigDef("recording.auto.enabled", "自动录像", "录像设置",
            ConfigValueType.BOOLEAN, null, "false", 10),
        createConfigDef("recording.schedule.enabled", "启用录像计划", "录像设置",
            ConfigValueType.BOOLEAN, null, "false", 11),
        createConfigDef("recording.retention.days", "录像保留天数", "录像保留",
            ConfigValueType.NUMBER, null, "7", 12),
        
        // 负载均衡
        createConfigDef("loadbalance algorithm", "负载均衡算法", "负载均衡",
            ConfigValueType.SELECT, "round_robin,wlc,geo_wlc", "wlc", 20),
        createConfigDef("loadbalance.wlc.weight", "WLC默认权重", "负载均衡",
            ConfigValueType.NUMBER, null, "100", 21),
        
        // CDN节点
        createConfigDef("cdn.healthcheck.interval", "健康检查间隔(秒)", "CDN节点",
            ConfigValueType.NUMBER, null, "30", 22),
        createConfigDef("cdn.healthcheck.timeout", "健康检查超时(秒)", "CDN节点",
            ConfigValueType.NUMBER, null, "10", 23),
        
        // 安全策略
        createConfigDef("security.jwt.expiration", "JWT过期时间(秒)", "认证配置",
            ConfigValueType.NUMBER, null, "86400", 30),
        createConfigDef("security.jwt.refresh.expiration", "JWT刷新过期时间(秒)", "认证配置",
            ConfigValueType.NUMBER, null, "604800", 31),
        createConfigDef("security.password.minLength", "密码最小长度", "认证配置",
            ConfigValueType.NUMBER, null, "8", 32),
        createConfigDef("security.session.timeout", "会话超时(分钟)", "认证配置",
            ConfigValueType.NUMBER, null, "30", 33),
        
        // 告警设置
        createConfigDef("alert.cooldown.minutes", "告警冷却时间(分钟)", "告警设置",
            ConfigValueType.NUMBER, null, "5", 40),
        createConfigDef("alert.maxPerHour", "每小时最大告警数", "告警设置",
            ConfigValueType.NUMBER, null, "100", 41),
        createConfigDef("escalation.check.interval", "升级检查间隔(秒)", "告警设置",
            ConfigValueType.NUMBER, null, "60", 42),
        
        // 通知渠道
        createConfigDef("notification.email.enabled", "启用邮件通知", "通知渠道",
            ConfigValueType.BOOLEAN, null, "false", 43),
        createConfigDef("notification.sms.enabled", "启用短信通知", "通知渠道",
            ConfigValueType.BOOLEAN, null, "false", 44),
        createNotificationConfigDef("notification.email.smtp.host", "SMTP服务器", "通知渠道",
            true, 44),
        createNotificationConfigDef("notification.email.smtp.port", "SMTP端口", "通知渠道",
            true, 45),
        createNotificationConfigDef("notification.email.username", "邮箱用户名", "通知渠道",
            true, 46),
        createNotificationConfigDef("notification.email.password", "邮箱密码", "通知渠道",
            true, 47),
        
        // 边缘节点
        createConfigDef("edge.heartbeat.timeout", "边缘节点心跳超时(秒)", "边缘节点",
            ConfigValueType.NUMBER, null, "180", 50),
        createConfigDef("edge.failover.enabled", "启用故障转移", "边缘故障转移",
            ConfigValueType.BOOLEAN, null, "false", 51),
        createConfigDef("edge.failover.delay.seconds", "故障转移延迟(秒)", "边缘故障转移",
            ConfigValueType.NUMBER, null, "60", 52),
        createConfigDef("edge.failover.batch.size", "故障转移批次大小", "边缘故障转移",
            ConfigValueType.NUMBER, null, "10", 53)
    );
    
    private static SystemConfig createConfigDef(String key, String name, String group,
            ConfigValueType type, String options, String defaultValue, int sortOrder) {
        return SystemConfig.builder()
                .configKey(key)
                .configName(name)
                .configGroup(group)
                .valueType(type)
                .options(options)
                .defaultValue(defaultValue)
                .configValue(defaultValue)
                .sortOrder(sortOrder)
                .enabled(true)
                .editable(true)
                .sensitive(false)
                .required(false)
                .build();
    }
    
    private static SystemConfig createNotificationConfigDef(String key, String name, 
            String group, boolean sensitive, int sortOrder) {
        return SystemConfig.builder()
                .configKey(key)
                .configName(name)
                .configGroup(group)
                .valueType(ConfigValueType.STRING)
                .sortOrder(sortOrder)
                .enabled(true)
                .editable(true)
                .sensitive(sensitive)
                .required(false)
                .build();
    }
    
    @Override
    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }
    
    @Override
    public List<SystemConfig> getEnabledConfigs() {
        return systemConfigRepository.findByEnabledTrueOrderByCategoryAscSortOrderAsc();
    }
    
    @Override
    public SystemConfig getByConfigKey(String configKey) {
        return systemConfigRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new IllegalArgumentException("配置项不存在: " + configKey));
    }
    
    @Override
    public String getConfigValue(String configKey) {
        SystemConfig config = getByConfigKey(configKey);
        if (config.getSensitive() != null && config.getSensitive()) {
            return "******"; // 敏感配置不返回实际值
        }
        return config.getConfigValue();
    }
    
    @Override
    public String getConfigValueWithSensitive(String configKey) {
        return getByConfigKey(configKey).getConfigValue();
    }
    
    @Override
    public List<SystemConfig> getByCategory(ConfigCategory category) {
        return systemConfigRepository.findByCategoryAndEnabledTrueOrderBySortOrderAsc(category);
    }
    
    @Override
    public List<SystemConfig> getByGroup(String group) {
        return systemConfigRepository.findByConfigGroupAndEnabledTrueOrderBySortOrderAsc(group);
    }
    
    @Override
    @Transactional
    public SystemConfig createConfig(SystemConfig config) {
        if (systemConfigRepository.existsByConfigKey(config.getConfigKey())) {
            throw new IllegalArgumentException("配置键已存在: " + config.getConfigKey());
        }
        
        // 保存历史
        saveHistory(null, config.getConfigKey(), "CREATE", null, config.getConfigValue(),
                "创建配置");
        
        return systemConfigRepository.save(config);
    }
    
    @Override
    @Transactional
    public SystemConfig updateConfig(Long id, SystemConfig configUpdate) {
        SystemConfig existing = systemConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("配置项不存在"));
        
        if (!existing.getEditable()) {
            throw new IllegalArgumentException("该配置项不可编辑");
        }
        
        // 验证新值
        if (!validateConfigValue(existing, configUpdate.getConfigValue())) {
            throw new IllegalArgumentException("配置值验证失败");
        }
        
        String oldValue = existing.getConfigValue();
        existing.setConfigValue(configUpdate.getConfigValue());
        existing.setDescription(configUpdate.getDescription());
        existing.setUpdatedBy(getCurrentUserId());
        
        // 保存历史
        saveHistory(id, existing.getConfigKey(), "UPDATE", oldValue, 
                configUpdate.getConfigValue(), "更新配置");
        
        return systemConfigRepository.save(existing);
    }
    
    @Override
    @Transactional
    public void deleteConfig(Long id) {
        SystemConfig config = systemConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("配置项不存在"));
        
        if (!config.getEditable()) {
            throw new IllegalArgumentException("该配置项不可删除");
        }
        
        // 保存历史
        saveHistory(id, config.getConfigKey(), "DELETE", 
                config.getConfigValue(), null, "删除配置");
        
        systemConfigRepository.delete(config);
    }
    
    @Override
    @Transactional
    public Map<String, Boolean> batchUpdateConfigs(Map<String, String> configValues) {
        Map<String, Boolean> results = new HashMap<>();
        
        for (Map.Entry<String, String> entry : configValues.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            try {
                SystemConfig config = getByConfigKey(key);
                if (!config.getEditable()) {
                    results.put(key, false);
                    continue;
                }
                
                if (!validateConfigValue(config, value)) {
                    results.put(key, false);
                    continue;
                }
                
                String oldValue = config.getConfigValue();
                config.setConfigValue(value);
                config.setUpdatedBy(getCurrentUserId());
                systemConfigRepository.save(config);
                
                // 保存历史
                saveHistory(config.getId(), key, "UPDATE", oldValue, value, "批量更新");
                
                results.put(key, true);
            } catch (Exception e) {
                log.error("批量更新配置失败: key={}, error={}", key, e.getMessage());
                results.put(key, false);
            }
        }
        
        return results;
    }
    
    @Override
    public boolean validateConfigValue(SystemConfig config, String value) {
        if (config.getRequired() && (value == null || value.isEmpty())) {
            return false;
        }
        
        if (value == null || value.isEmpty()) {
            return true;
        }
        
        ConfigValueType type = config.getValueType();
        switch (type) {
            case NUMBER:
                try {
                    double numValue = Double.parseDouble(value);
                    if (config.getMinValue() != null && numValue < config.getMinValue()) {
                        return false;
                    }
                    if (config.getMaxValue() != null && numValue > config.getMaxValue()) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
                break;
                
            case BOOLEAN:
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    return false;
                }
                break;
                
            case EMAIL:
                if (!Pattern.matches("^[A-Za-z0-9+_.-]+@(.+)$", value)) {
                    return false;
                }
                break;
                
            case URL:
                if (!Pattern.matches("^https?://.*", value)) {
                    return false;
                }
                break;
                
            case IP:
                if (!Pattern.matches("^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$", value)) {
                    return false;
                }
                break;
                
            case PORT:
                try {
                    int port = Integer.parseInt(value);
                    if (port < 1 || port > 65535) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
                break;
                
            case SELECT:
                if (config.getOptions() != null) {
                    Set<String> validOptions = Arrays.stream(config.getOptions().split(","))
                            .map(String::trim)
                            .collect(Collectors.toSet());
                    if (!validOptions.contains(value)) {
                        return false;
                    }
                }
                break;
                
            case JSON:
                // 简单的JSON格式验证
                if (!value.startsWith("{") && !value.startsWith("[")) {
                    return false;
                }
                break;
        }
        
        // 自定义验证规则
        if (config.getValidationRule() != null && !config.getValidationRule().isEmpty()) {
            if (!Pattern.matches(config.getValidationRule(), value)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public List<ConfigHistory> getConfigHistory(String configKey) {
        return configHistoryRepository.findByConfigKeyOrderByCreatedAtDesc(configKey);
    }
    
    @Override
    @Transactional
    public SystemConfig rollbackConfig(String configKey) {
        Optional<ConfigHistory> rollbackPoint = configHistoryRepository.findLastRollbackPoint(configKey);
        
        if (rollbackPoint.isEmpty()) {
            throw new IllegalStateException("无可用的回滚点");
        }
        
        ConfigHistory history = rollbackPoint.get();
        if (history.getOldValue() == null) {
            throw new IllegalStateException("无法回滚到创建状态");
        }
        
        SystemConfig config = getByConfigKey(configKey);
        String oldValue = config.getConfigValue();
        
        config.setConfigValue(history.getOldValue());
        config.setUpdatedBy(getCurrentUserId());
        
        // 标记历史记录为已回滚
        history.setRolledBack(true);
        configHistoryRepository.save(history);
        
        // 保存新的历史记录
        saveHistory(config.getId(), configKey, "ROLLBACK", oldValue, 
                history.getOldValue(), "回滚到历史版本");
        
        return systemConfigRepository.save(config);
    }
    
    @Override
    @Transactional
    public SystemConfig resetConfig(String configKey) {
        SystemConfig config = getByConfigKey(configKey);
        
        if (!config.getEditable()) {
            throw new IllegalArgumentException("该配置项不可重置");
        }
        
        if (config.getDefaultValue() == null) {
            throw new IllegalArgumentException("该配置项没有默认值");
        }
        
        String oldValue = config.getConfigValue();
        config.setConfigValue(config.getDefaultValue());
        config.setUpdatedBy(getCurrentUserId());
        
        // 保存历史
        saveHistory(config.getId(), configKey, "RESET", oldValue, 
                config.getDefaultValue(), "重置为默认值");
        
        return systemConfigRepository.save(config);
    }
    
    @Override
    @Transactional
    public void resetAllConfigs() {
        List<SystemConfig> editableConfigs = systemConfigRepository.findAll().stream()
                .filter(SystemConfig::getEditable)
                .collect(Collectors.toList());
        
        for (SystemConfig config : editableConfigs) {
            if (config.getDefaultValue() != null) {
                config.setConfigValue(config.getDefaultValue());
                config.setUpdatedBy(getCurrentUserId());
            }
        }
        
        systemConfigRepository.saveAll(editableConfigs);
        log.info("已重置 {} 个配置项", editableConfigs.size());
    }
    
    @Override
    public List<String> getAllGroups() {
        return systemConfigRepository.findAllGroups();
    }
    
    @Override
    @Transactional
    public void initializeDefaultConfigs() {
        List<SystemConfig> existingConfigs = systemConfigRepository.findAll();
        Set<String> existingKeys = existingConfigs.stream()
                .map(SystemConfig::getConfigKey)
                .collect(Collectors.toSet());
        
        List<SystemConfig> newConfigs = new ArrayList<>();
        
        for (SystemConfig defaultConfig : DEFAULT_CONFIGS) {
            if (!existingKeys.contains(defaultConfig.getConfigKey())) {
                newConfigs.add(defaultConfig);
            }
        }
        
        if (!newConfigs.isEmpty()) {
            systemConfigRepository.saveAll(newConfigs);
            log.info("初始化了 {} 个默认配置项", newConfigs.size());
        }
    }
    
    private void saveHistory(Long configId, String configKey, String operationType,
            String oldValue, String newValue, String description) {
        HttpServletRequest request = getHttpServletRequest();
        
        ConfigHistory history = ConfigHistory.builder()
                .configId(configId)
                .configKey(configKey)
                .operationType(operationType)
                .oldValue(oldValue)
                .newValue(newValue)
                .changeDescription(description)
                .operatorId(getCurrentUserId())
                .operatorName(getCurrentUserName())
                .operatorIp(request != null ? getClientIp(request) : null)
                .rollbackable(true)
                .rolledBack(false)
                .build();
        
        configHistoryRepository.save(history);
    }
    
    private Long getCurrentUserId() {
        try {
            return currentUserContext.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
    
    private String getCurrentUserName() {
        try {
            return currentUserContext.getCurrentUsername();
        } catch (Exception e) {
            return null;
        }
    }
    
    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
