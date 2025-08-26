package com.aick.mmp.central.config;

import com.aick.mmp.shared.model.User;
import com.aick.mmp.central.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializerConfig implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeDefaultUsers();
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