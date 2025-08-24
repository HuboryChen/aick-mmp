package com.aick.mmp.service;

import com.aick.mmp.dto.LoginRequest;
import com.aick.mmp.dto.LoginResponse;
import com.aick.mmp.dto.UserDTO;

public interface AuthService {
    
    LoginResponse login(LoginRequest loginRequest);
    
    UserDTO getCurrentUser(String username);
    
    void logout(String token);
    
    boolean validateToken(String token);
}