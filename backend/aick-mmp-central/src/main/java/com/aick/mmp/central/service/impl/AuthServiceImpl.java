package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.dto.LoginRequest;
import com.aick.mmp.central.dto.LoginResponse;
import com.aick.mmp.central.dto.UserDTO;
import com.aick.mmp.shared.model.User;
import com.aick.mmp.central.repository.UserRepository;
import com.aick.mmp.central.service.AuthService;
import com.aick.mmp.shared.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ModelMapper modelMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${security.jwt.refresh-expiration}")
    private long jwtRefreshExpiration;

    @Value("${security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${auth.login.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${auth.login.lock-duration-minutes:10}")
    private int lockDurationMinutes;

    @Value("${auth.session.timeout-minutes:30}")
    private int sessionTimeoutMinutes;

    private static final String FAILED_LOGIN_PREFIX = "login_failed:";
    private static final String TOKEN_BLACKLIST_PREFIX = "token_blacklist:";
    private static final String SESSION_TIMEOUT_PREFIX = "session_timeout:";

    @Override
    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        try {
            log.info("Attempting login for user: {}", loginRequest.getUsername());

            // 检查是否被锁定
            if (isAccountLocked(loginRequest.getUsername())) {
                long remainingMinutes = getLockRemainingMinutes(loginRequest.getUsername());
                log.warn("用户 {} 账户已锁定，剩余锁定时间: {} 分钟", loginRequest.getUsername(), remainingMinutes);
                throw new RuntimeException(String.format("账户已锁定，请 %d 分钟后再试", remainingMinutes));
            }

            User user = userRepository.findActiveUserByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> {
                        incrementFailedAttempts(loginRequest.getUsername());
                        log.warn("User not found: {}", loginRequest.getUsername());
                        return new RuntimeException("用户名或密码错误");
                    });

            log.info("User found: {}, enabled: {}, status: {}", user.getUsername(), user.isEnabled(), user.getStatus());

            // 检查用户是否被锁定
            if (user.isAccountLocked()) {
                log.warn("用户 {} 账户已锁定", loginRequest.getUsername());
                throw new RuntimeException("账户已锁定，请联系管理员");
            }

            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                // 密码错误，增加失败次数
                handleFailedLogin(user);
                log.warn("用户 {} 密码验证失败", loginRequest.getUsername());
                throw new RuntimeException("用户名或密码错误");
            }

            if (!user.isEnabled()) {
                log.warn("用户 {} 账户已禁用", loginRequest.getUsername());
                throw new RuntimeException("账户已禁用，请联系管理员");
            }

            if (user.getStatus() != User.UserStatus.ACTIVE) {
                log.warn("用户 {} 账户状态异常: {}", loginRequest.getUsername(), user.getStatus());
                throw new RuntimeException("账户状态异常，请联系管理员");
            }

            // 登录成功，重置失败次数
            resetFailedAttempts(user.getUsername());

            // 更新最后登录时间
            user.setLastLoginTime(LocalDateTime.now());
            userRepository.save(user);

            String token = jwtUtil.generateToken(user.getUsername());
            String refreshToken = generateRefreshToken(user.getUsername());

            // 设置会话超时
            setSessionTimeout(user.getUsername());

            UserDTO userDTO = modelMapper.map(user, UserDTO.class);

            log.info("用户 {} 登录成功", user.getUsername());

            return new LoginResponse(token, refreshToken, userDTO);
        } catch (Exception e) {
            log.error("用户登录失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    private boolean isAccountLocked(String username) {
        String key = FAILED_LOGIN_PREFIX + username;
        String failedCount = redisTemplate.opsForValue().get(key);
        if (failedCount != null) {
            int count = Integer.parseInt(failedCount);
            return count >= maxFailedAttempts;
        }
        return false;
    }

    private long getLockRemainingMinutes(String username) {
        String key = FAILED_LOGIN_PREFIX + username;
        Long remaining = redisTemplate.getExpire(key, TimeUnit.MINUTES);
        return remaining != null && remaining > 0 ? remaining : 0;
    }

    private void incrementFailedAttempts(String username) {
        String key = FAILED_LOGIN_PREFIX + username;
        String current = redisTemplate.opsForValue().get(key);
        int count = (current != null) ? Integer.parseInt(current) + 1 : 1;
        redisTemplate.opsForValue().set(key, String.valueOf(count), lockDurationMinutes, TimeUnit.MINUTES);
    }

    private void handleFailedLogin(User user) {
        incrementFailedAttempts(user.getUsername());

        // 更新数据库中的失败次数
        user.setLoginFailedCount(user.getLoginFailedCount() + 1);

        // 检查是否需要锁定账户
        int failedCount = user.getLoginFailedCount();
        if (failedCount >= maxFailedAttempts) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(lockDurationMinutes));
            user.setStatus(User.UserStatus.LOCKED);
            log.warn("用户 {} 登录失败次数达到上限 {} 次，账户已锁定", user.getUsername(), failedCount);
        }

        userRepository.save(user);
    }

    private void resetFailedAttempts(String username) {
        String key = FAILED_LOGIN_PREFIX + username;
        redisTemplate.delete(key);

        // 更新数据库
        userRepository.findActiveUserByUsername(username).ifPresent(user -> {
            user.setLoginFailedCount(0);
            user.setLockedUntil(null);
            if (user.getStatus() == User.UserStatus.LOCKED) {
                user.setStatus(User.UserStatus.ACTIVE);
            }
            userRepository.save(user);
        });
    }

    private String generateRefreshToken(String username) {
        return jwtUtil.generateToken(username, jwtRefreshExpiration);
    }

    private void setSessionTimeout(String username) {
        String key = SESSION_TIMEOUT_PREFIX + username;
        redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()),
                                     sessionTimeoutMinutes, TimeUnit.MINUTES);
    }

    @Override
    public UserDTO getCurrentUser(String username) {
        User user = userRepository.findActiveUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        return modelMapper.map(user, UserDTO.class);
    }

    @Override
    public void logout(String token) {
        try {
            // 将token加入黑名单
            String username = jwtUtil.getUsernameFromToken(token);
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
            long remainingTime = jwtUtil.getTokenRemainingTime(token);

            if (remainingTime > 0) {
                redisTemplate.opsForValue().set(blacklistKey, "1", remainingTime, TimeUnit.MILLISECONDS);
            }

            // 清除会话超时
            String sessionKey = SESSION_TIMEOUT_PREFIX + username;
            redisTemplate.delete(sessionKey);

            log.info("用户 {} 退出登录", username);
        } catch (Exception e) {
            log.error("退出登录失败: {}", e.getMessage(), e);
            throw new RuntimeException("退出登录失败");
        }
    }

    @Override
    public boolean validateToken(String token) {
        // 检查token是否在黑名单中
        if (isTokenBlacklisted(token)) {
            log.warn("Token已在黑名单中");
            return false;
        }

        return jwtUtil.validateToken(token);
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        try {
            // 验证refresh token
            if (!validateToken(refreshToken)) {
                throw new RuntimeException("Refresh token无效或已过期");
            }

            String username = jwtUtil.getUsernameFromToken(refreshToken);

            // 检查用户是否存在且可用
            User user = userRepository.findActiveUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在或已被禁用"));

            // 生成新的access token
            String newToken = jwtUtil.generateToken(username);
            String newRefreshToken = generateRefreshToken(username);

            // 更新会话超时
            setSessionTimeout(username);

            UserDTO userDTO = modelMapper.map(user, UserDTO.class);

            log.info("用户 {} token刷新成功", username);

            return new LoginResponse(newToken, newRefreshToken, userDTO);
        } catch (Exception e) {
            log.error("Token刷新失败: {}", e.getMessage(), e);
            throw new RuntimeException("Token刷新失败: " + e.getMessage());
        }
    }

    @Override
    public boolean checkSessionTimeout(String username) {
        String key = SESSION_TIMEOUT_PREFIX + username;
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }

    private boolean isTokenBlacklisted(String token) {
        String key = TOKEN_BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}