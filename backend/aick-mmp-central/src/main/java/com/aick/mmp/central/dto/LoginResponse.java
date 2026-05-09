package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String token;
    private String refreshToken;
    private UserDTO user;

    public LoginResponse(String token, UserDTO user) {
        this.token = token;
        this.user = user;
    }
}