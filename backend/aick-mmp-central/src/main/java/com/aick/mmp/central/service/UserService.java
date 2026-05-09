package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.ChangePasswordDTO;
import com.aick.mmp.central.dto.CreateUserRequestDTO;
import com.aick.mmp.central.dto.UserDTO;
import com.aick.mmp.shared.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    Page<UserDTO> getAllUsers(Pageable pageable);

    Page<UserDTO> searchUsers(String keyword, User.UserRole role, User.UserStatus status, Pageable pageable);

    UserDTO getUserById(Long id);

    UserDTO createUser(CreateUserRequestDTO userDTO);

    UserDTO updateUser(Long id, UserDTO userDTO);

    void deleteUser(Long id);

    void changePassword(Long userId, ChangePasswordDTO changePasswordDTO);

    void resetPassword(Long userId, String newPassword);

    void batchDeleteUsers(List<Long> userIds);

    void batchEnableUsers(List<Long> userIds, boolean enabled);

    void batchUpdateUserRole(List<Long> userIds, User.UserRole role);

    List<User.UserRole> getAllRoles();
}