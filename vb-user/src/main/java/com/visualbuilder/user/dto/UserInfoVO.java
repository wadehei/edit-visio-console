package com.visualbuilder.user.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserInfoVO {
    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private String role;
    private List<String> permissions;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
