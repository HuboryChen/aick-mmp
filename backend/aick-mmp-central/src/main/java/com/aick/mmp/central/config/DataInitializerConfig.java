package com.aick.mmp.central.config;

import com.aick.mmp.central.repository.ApiKeyRepository;
import com.aick.mmp.central.repository.CameraConfigTemplateRepository;
import com.aick.mmp.shared.model.ApiKey;
import com.aick.mmp.shared.model.CameraConfigTemplate;
import com.aick.mmp.shared.model.User;
import com.aick.mmp.central.repository.UserRepository;
import com.aick.mmp.shared.model.enums.ApiKeyStatus;
import com.aick.mmp.shared.model.enums.ApiKeyType;
import com.aick.mmp.shared.util.AESEncryptionUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializerConfig implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApiKeyRepository apiKeyRepository;
    private final AESEncryptionUtil encryptionUtil;
    private final CameraConfigTemplateRepository cameraConfigTemplateRepository;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    // 边缘节点使用的固定AK/SK (与docker-compose.yml中配置一致)
    private static final String EDGE_ACCESS_KEY = "ak_udL9QZGQttTjrJ6ryv5j_bmeQLoNm-6HbEv8ZEYSspk";
    private static final String EDGE_SECRET_KEY = "sk_QMfuO1KsTLsyfCP8T7TS1Q9vnEYoVpC_SlCIXLqMfQo";

    @Override
    public void run(String... args) throws Exception {
        initializeDefaultUsers();
        initializeDefaultSystemApps();
        initializeDefaultApiKeys();
        initializePresetTemplates();
    }

    private void initializePresetTemplates() {
        try {
            long presetCount = cameraConfigTemplateRepository.findByIsPresetAndIsDeletedFalse(true).size();
            if (presetCount > 0) {
                log.info("预置摄像头配置模板已存在, 数量: {}, 跳过初始化", presetCount);
                return;
            }

            log.info("开始加载预置摄像头配置模板...");
            Resource resource = resourceLoader.getResource("classpath:templates/preset-cameras.json");
            if (!resource.exists()) {
                log.warn("预置模板文件不存在: classpath:templates/preset-cameras.json");
                return;
            }

            try (InputStream is = resource.getInputStream()) {
                List<CameraConfigTemplate> templates = objectMapper.readValue(is, new TypeReference<List<CameraConfigTemplate>>() {});
                for (CameraConfigTemplate template : templates) {
                    template.setId(null);
                    template.setCreatedAt(LocalDateTime.now());
                    template.setUpdatedAt(LocalDateTime.now());
                    if (template.getIsPreset() == null) template.setIsPreset(true);
                    if (template.getUsageCount() == null) template.setUsageCount(0);
                    if (template.getIsDeleted() == null) template.setIsDeleted(false);
                }
                cameraConfigTemplateRepository.saveAll(templates);
                log.info("预置摄像头配置模板加载完成, 共 {} 条", templates.size());
            }
        } catch (Exception e) {
            log.error("加载预置摄像头配置模板失败: {}", e.getMessage(), e);
        }
    }

    private void initializeDefaultUsers() {
        log.info("开始初始化默认用户...");
        
        createDefaultUser("admin", "admin123", "admin@example.com", "System Administrator", 
                         User.UserRole.ADMIN, "IT Department");
        
        createDefaultUser("operator", "operator123", "operator@example.com", "System Operator", 
                         User.UserRole.OPERATOR, "Operations Department");
        
        createDefaultUser("viewer", "viewer123", "viewer@example.com", "System Viewer", 
                         User.UserRole.VIEWER, "Monitoring Department");
        
        log.info("默认用户初始化完成!");
    }

    private void initializeDefaultSystemApps() {
        log.info("开始初始化默认系统应用...");
        
        createDefaultEdgeNodeApp();
        
        log.info("默认系统应用初始化完成!");
    }

    /**
     * 初始化边缘节点使用的ApiKey (存储在api_keys表中，供AKSK认证使用)
     */
    private void initializeDefaultApiKeys() {
        log.info("开始初始化默认ApiKey...");
        
        try {
            if (!apiKeyRepository.existsByAccessKey(EDGE_ACCESS_KEY)) {
                log.info("创建边缘节点ApiKey");
                
                // 加密Secret Key
                String encryptedSecret = encryptionUtil.encrypt(EDGE_SECRET_KEY);
                
                ApiKey apiKey = ApiKey.builder()
                        .accessKey(EDGE_ACCESS_KEY)
                        .encryptedSecret(encryptedSecret)
                        .userId(null) // 系统级凭证，不关联用户
                        .type(ApiKeyType.USER) // 保持兼容，使用USER类型
                        .name("Edge Node System Key")
                        .status(ApiKeyStatus.ENABLED)
                        .expiresAt(null) // 永不过期
                        .build();

                apiKeyRepository.save(apiKey);
                log.info("边缘节点ApiKey创建成功! AK: {}", EDGE_ACCESS_KEY);
            } else {
                log.info("边缘节点ApiKey已存在: {}", EDGE_ACCESS_KEY);
            }
        } catch (Exception e) {
            log.error("创建边缘节点ApiKey失败: {}", e.getMessage(), e);
        }
        
        log.info("默认ApiKey初始化完成!");
    }

    private void createDefaultEdgeNodeApp() {
        // SystemApp初始化已移除，边缘节点认证现在使用ApiKey
        // 保留此方法以维持架构一致性
        log.info("边缘节点使用ApiKey进行认证");
    }

    private void createDefaultUser(String username, String password, String email, 
                                 String fullName, User.UserRole role, String department) {
        try {
            log.info("检查用户是否存在: {}", username);
            
            if (!userRepository.existsByUsername(username)) {
                log.info("创建新用户: {}", username);
                
                User user = User.builder()
                        .username(username)
                        .password(passwordEncoder.encode(password))
                        .email(email)
                        .fullName(fullName)
                        .phone("13800138000")
                        .department(department)
                        .role(role)
                        .status(User.UserStatus.ACTIVE)
                        .enabled(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                userRepository.save(user);
                log.info("默认{}用户创建成功: {}/{}", role.name().toLowerCase(), username, password);
            } else {
                log.info("{}用户已存在: {}", username, username);
            }
        } catch (Exception e) {
            log.error("创建用户失败: {} - {}", username, e.getMessage(), e);
        }
    }
}
