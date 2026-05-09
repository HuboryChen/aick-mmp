package com.aick.mmp.central.service.impl;


import com.aick.mmp.central.dto.ChangePasswordDTO;
import com.aick.mmp.central.dto.CreateUserRequestDTO;
import com.aick.mmp.central.dto.UserDTO;
import com.aick.mmp.central.repository.UserRepository;
import com.aick.mmp.central.service.UserService;
import com.aick.mmp.shared.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    @Override
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(user -> modelMapper.map(user, UserDTO.class));
    }

    @Override
    public Page<UserDTO> searchUsers(String keyword, User.UserRole role, User.UserStatus status, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isEmpty()) {
                predicates.add(cb.or(
                        cb.like(root.get("username"), "%" + keyword + "%"),
                        cb.like(root.get("email"), "%" + keyword + "%"),
                        cb.like(root.get("fullName"), "%" + keyword + "%")
                ));
            }

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return userRepository.findAll(spec, pageable)
                .map(user -> modelMapper.map(user, UserDTO.class));
    }

    @Override
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return modelMapper.map(user, UserDTO.class);
    }

    @Override
    @Transactional
    public UserDTO createUser(CreateUserRequestDTO userDTO) {
        // 检查用户名和邮箱是否已存在
        if (userRepository.existsByUsername(userDTO.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new RuntimeException("邮箱已存在");
        }

        // 创建新用户
        User user = User.builder()
                .username(userDTO.getUsername())
                .password(passwordEncoder.encode(userDTO.getPassword()))
                .email(userDTO.getEmail())
                .fullName(userDTO.getFullName())
                .phone(userDTO.getPhone())
                .department(userDTO.getDepartment())
                .role(userDTO.getRole())
                .status(userDTO.getStatus() != null ? userDTO.getStatus() : User.UserStatus.ACTIVE)
                .enabled(userDTO.isEnabled())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("创建新用户: {}", savedUser.getUsername());
        
        return modelMapper.map(savedUser, UserDTO.class);
    }

    @Override
    @Transactional
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查用户名是否被其他用户使用
        if (!existingUser.getUsername().equals(userDTO.getUsername()) 
                && userRepository.existsByUsername(userDTO.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否被其他用户使用
        if (!existingUser.getEmail().equals(userDTO.getEmail()) 
                && userRepository.existsByEmail(userDTO.getEmail())) {
            throw new RuntimeException("邮箱已存在");
        }

        // 更新用户信息
        existingUser.setUsername(userDTO.getUsername());
        existingUser.setEmail(userDTO.getEmail());
        existingUser.setFullName(userDTO.getFullName());
        existingUser.setPhone(userDTO.getPhone());
        existingUser.setDepartment(userDTO.getDepartment());
        existingUser.setRole(userDTO.getRole());
        existingUser.setStatus(userDTO.getStatus());
        existingUser.setEnabled(userDTO.isEnabled());
        existingUser.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(existingUser);
        log.info("更新用户信息: {}", updatedUser.getUsername());
        
        return modelMapper.map(updatedUser, UserDTO.class);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 不能删除自己
        if (user.getUsername().equals("admin")) {
            throw new RuntimeException("不能删除管理员账户");
        }
        
        userRepository.deleteById(id);
        log.info("删除用户: {}", user.getUsername());
    }

    @Override
    public List<User.UserRole> getAllRoles() {
        return Arrays.asList(User.UserRole.values());
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordDTO changePasswordDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 验证旧密码
        if (!passwordEncoder.matches(changePasswordDTO.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("旧密码不正确");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("用户 {} 修改密码成功", user.getUsername());
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 重置密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("管理员重置用户 {} 的密码", user.getUsername());
    }

    @Override
    @Transactional
    public void batchDeleteUsers(List<Long> userIds) {
        List<User> users = userRepository.findAllById(userIds);

        // 检查是否包含admin
        boolean containsAdmin = users.stream()
                .anyMatch(user -> user.getUsername().equals("admin"));

        if (containsAdmin) {
            throw new RuntimeException("不能删除管理员账户");
        }

        List<String> usernames = users.stream()
                .map(User::getUsername)
                .collect(Collectors.toList());

        userRepository.deleteAllById(userIds);
        log.info("批量删除用户: {}", usernames);
    }

    @Override
    @Transactional
    public void batchEnableUsers(List<Long> userIds, boolean enabled) {
        List<User> users = userRepository.findAllById(userIds);

        // 检查是否包含admin
        boolean containsAdmin = users.stream()
                .anyMatch(user -> user.getUsername().equals("admin"));

        if (containsAdmin && !enabled) {
            throw new RuntimeException("不能禁用管理员账户");
        }

        users.forEach(user -> {
            user.setEnabled(enabled);
            user.setUpdatedAt(LocalDateTime.now());
        });

        userRepository.saveAll(users);

        List<String> usernames = users.stream()
                .map(User::getUsername)
                .collect(Collectors.toList());

        log.info("批量{}用户: {}", enabled ? "启用" : "禁用", usernames);
    }

    @Override
    @Transactional
    public void batchUpdateUserRole(List<Long> userIds, User.UserRole role) {
        List<User> users = userRepository.findAllById(userIds);

        // 检查是否包含admin
        boolean containsAdmin = users.stream()
                .anyMatch(user -> user.getUsername().equals("admin"));

        if (containsAdmin) {
            throw new RuntimeException("不能修改管理员角色");
        }

        users.forEach(user -> {
            user.setRole(role);
            user.setUpdatedAt(LocalDateTime.now());
        });

        userRepository.saveAll(users);

        List<String> usernames = users.stream()
                .map(User::getUsername)
                .collect(Collectors.toList());

        log.info("批量更新用户角色为 {}: {}", role, usernames);
    }
}