package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.BatchOperationDTO;
import com.aick.mmp.central.dto.ChangePasswordDTO;
import com.aick.mmp.central.dto.CreateUserRequestDTO;
import com.aick.mmp.central.dto.UserDTO;
import com.aick.mmp.central.service.UserService;
import com.aick.mmp.shared.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Page<UserDTO>> getAllUsers(Pageable pageable) {
        Page<UserDTO> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Page<UserDTO>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) User.UserRole role,
            @RequestParam(required = false) User.UserStatus status,
            Pageable pageable) {
        Page<UserDTO> users = userService.searchUsers(keyword, role, status, pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> createUser(@RequestBody CreateUserRequestDTO userDTO) {
        UserDTO createdUser = userService.createUser(userDTO);
        return ResponseEntity.ok(createdUser);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        UserDTO updatedUser = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordDTO changePasswordDTO) {
        userService.changePassword(changePasswordDTO.getUserId(), changePasswordDTO);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetPassword(@PathVariable Long userId, @RequestBody String newPassword) {
        userService.resetPassword(userId, newPassword);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch-operation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> batchOperation(@RequestBody BatchOperationDTO batchOperationDTO) {
        switch (batchOperationDTO.getOperation()) {
            case DELETE:
                userService.batchDeleteUsers(batchOperationDTO.getUserIds());
                break;
            case ENABLE:
                userService.batchEnableUsers(batchOperationDTO.getUserIds(), true);
                break;
            case DISABLE:
                userService.batchEnableUsers(batchOperationDTO.getUserIds(), false);
                break;
            case UPDATE_ROLE:
                User.UserRole role = User.UserRole.valueOf(batchOperationDTO.getRole());
                userService.batchUpdateUserRole(batchOperationDTO.getUserIds(), role);
                break;
            default:
                break;
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<List<User.UserRole>> getAllRoles() {
        List<User.UserRole> roles = userService.getAllRoles();
        return ResponseEntity.ok(roles);
    }
}