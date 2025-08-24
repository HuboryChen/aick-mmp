package com.aick.mmp.dto;

import com.aick.mmp.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String department;
    private User.UserRole role;
    private User.UserStatus status;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}