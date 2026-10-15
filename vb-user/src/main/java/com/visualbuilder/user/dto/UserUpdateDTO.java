package com.visualbuilder.user.dto;

import lombok.Data;

@Data
public class UserUpdateDTO {
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
}
