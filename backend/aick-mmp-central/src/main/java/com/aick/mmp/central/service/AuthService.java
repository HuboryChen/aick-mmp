package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.LoginRequest;
import com.aick.mmp.central.dto.LoginResponse;
import com.aick.mmp.central.dto.UserDTO;

public interface AuthService {

    LoginResponse login(LoginRequest loginRequest);

    UserDTO getCurrentUser(String username);

    void logout(String token);

    boolean validateToken(String token);

    LoginResponse refreshToken(String refreshToken);

    boolean checkSessionTimeout(String username);
}