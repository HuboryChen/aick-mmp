package com.aick.mmp.service.impl;

import com.aick.mmp.dto.LoginRequest;
import com.aick.mmp.dto.LoginResponse;
import com.aick.mmp.dto.UserDTO;
import com.aick.mmp.model.User;
import com.aick.mmp.repository.UserRepository;
import com.aick.mmp.service.AuthService;
import com.aick.mmp.util.JwtUtil;
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
        User user = userRepository.findActiveUserByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername());
        UserDTO userDTO = modelMapper.map(user, UserDTO.class);

        log.info("用户 {} 登录成功", user.getUsername());
        
        return new LoginResponse(token, userDTO);
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