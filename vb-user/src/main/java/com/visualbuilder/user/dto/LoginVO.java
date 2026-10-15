package com.visualbuilder.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginVO {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private UserInfoVO user;
}
