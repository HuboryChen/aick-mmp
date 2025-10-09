package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.CreateUserRequestDTO;
import com.aick.mmp.central.dto.UserDTO;
import com.aick.mmp.shared.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    
    Page<UserDTO> getAllUsers(Pageable pageable);
    
    UserDTO getUserById(Long id);
    
    UserDTO createUser(CreateUserRequestDTO userDTO);
    
    UserDTO updateUser(Long id, UserDTO userDTO);
    
    void deleteUser(Long id);
    
    List<User.UserRole> getAllRoles();
}