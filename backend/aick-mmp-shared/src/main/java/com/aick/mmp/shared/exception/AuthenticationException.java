package com.aick.mmp.shared.exception;

import lombok.Getter;

/**
 * 认证异常类
 */
@Getter
public class AuthenticationException extends RuntimeException {
    
    private final String errorCode;
    
    public AuthenticationException(String message) {
        super(message);
        this.errorCode = null;
    }
    
    public AuthenticationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }
}
