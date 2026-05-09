package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.UserConfigDTO;
import com.aick.mmp.central.security.CurrentUserContext;
import com.aick.mmp.central.security.CustomUserDetails;
import com.aick.mmp.central.service.UserConfigService;
import com.aick.mmp.shared.model.UserConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户配置控制器
 */
@RestController
@RequestMapping("/user-configs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "用户配置", description = "用户个性化配置管理")
public class UserConfigController {
    
    private final UserConfigService userConfigService;
    private final CurrentUserContext currentUserContext;
    
    /**
     * 保存用户配置
     */
    @PostMapping
    @Operation(summary = "保存用户配置")
    public ResponseEntity<UserConfigDTO> saveConfig(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {
        
        Long userId = getCurrentUserId(userDetails);
        String configKey = request.get("configKey");
        String configValue = request.get("configValue");
        
        log.info("Saving config for user {}: {} = {}", userId, configKey, configValue);
        
        UserConfig config = userConfigService.saveConfig(userId, configKey, configValue);
        return ResponseEntity.ok(toDTO(config));
    }
    
    /**
     * 批量保存用户配置
     */
    @PostMapping("/batch")
    @Operation(summary = "批量保存用户配置")
    public ResponseEntity<List<UserConfigDTO>> saveConfigs(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody List<UserConfigDTO> configs) {
        
        Long userId = getCurrentUserId(userDetails);
        log.info("Batch saving {} configs for user {}", configs.size(), userId);
        
        List<UserConfig> savedConfigs = userConfigService.saveConfigs(userId, configs);
        return ResponseEntity.ok(savedConfigs.stream().map(this::toDTO).toList());
    }
    
    /**
     * 获取用户指定配置
     */
    @GetMapping("/{configKey}")
    @Operation(summary = "获取用户指定配置")
    public ResponseEntity<UserConfigDTO> getConfig(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String configKey) {
        
        Long userId = getCurrentUserId(userDetails);
        
        return userConfigService.getConfig(userId, configKey)
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 获取用户所有配置
     */
    @GetMapping
    @Operation(summary = "获取用户所有配置")
    public ResponseEntity<List<UserConfigDTO>> getUserConfigs(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = getCurrentUserId(userDetails);
        List<UserConfig> configs = userConfigService.getUserConfigs(userId);
        return ResponseEntity.ok(configs.stream().map(this::toDTO).toList());
    }
    
    /**
     * 删除用户指定配置
     */
    @DeleteMapping("/{configKey}")
    @Operation(summary = "删除用户指定配置")
    public ResponseEntity<Void> deleteConfig(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String configKey) {
        
        Long userId = getCurrentUserId(userDetails);
        log.info("Deleting config for user {}: {}", userId, configKey);
        
        userConfigService.deleteConfig(userId, configKey);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 删除用户所有配置
     */
    @DeleteMapping
    @Operation(summary = "删除用户所有配置")
    public ResponseEntity<Void> deleteUserConfigs(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = getCurrentUserId(userDetails);
        log.info("Deleting all configs for user {}", userId);
        
        userConfigService.deleteUserConfigs(userId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 从UserDetails获取当前用户ID
     */
    private Long getCurrentUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        if (userDetails instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserId();
        }
        
        // Fallback: 尝试通过CurrentUserContext获取
        Long userId = currentUserContext.getCurrentUserId();
        if (userId != null) {
            return userId;
        }
        
        throw new RuntimeException("Unable to determine user ID");
    }
    
    private UserConfigDTO toDTO(UserConfig config) {
        return UserConfigDTO.builder()
                .id(config.getId())
                .configKey(config.getConfigKey())
                .configValue(config.getConfigValue())
                .description(config.getDescription())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
