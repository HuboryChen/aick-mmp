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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        try {
            User user = userRepository.findActiveUserByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
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

            // 更新最后登录时间
            user.setLastLoginTime(LocalDateTime.now());
            userRepository.save(user);

            String token = jwtUtil.generateToken(user.getUsername());
            UserDTO userDTO = modelMapper.map(user, UserDTO.class);

            log.info("用户 {} 登录成功", user.getUsername());
            
            return new LoginResponse(token, userDTO);
        } catch (Exception e) {
            log.error("用户登录失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public UserDTO getCurrentUser(String username) {
        User user = userRepository.findActiveUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        return modelMapper.map(user, UserDTO.class);
    }

    @Override
    public void logout(String token) {
        // 实际应用中可以将token加入黑名单
        log.info("用户退出登录");
    }

    @Override
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }
}