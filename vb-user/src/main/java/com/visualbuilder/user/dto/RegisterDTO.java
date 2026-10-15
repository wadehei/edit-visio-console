package com.visualbuilder.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度3-32位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度8-32位")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;
}
