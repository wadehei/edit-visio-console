package com.visualbuilder.user.converter;

import com.visualbuilder.user.dto.UserInfoVO;
import com.visualbuilder.user.entity.SysUser;

import java.util.List;

public final class UserConverter {

    private UserConverter() {}

    public static UserInfoVO toUserInfoVO(SysUser user, List<String> permissions) {
        return UserInfoVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .permissions(permissions)
                .createdAt(user.getCreatedTime())
                .lastLoginAt(user.getLoginDate())
                .build();
    }
}
