package com.visualbuilder.user.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.user.dto.*;
import com.visualbuilder.user.entity.SysUser;

public interface SysUserService extends BaseService<SysUser> {

    /**
     * 用户注册
     */
    SysUser register(RegisterDTO dto);

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO dto);

    /**
     * 获取当前用户信息
     */
    UserInfoVO getCurrentUserInfo();

    /**
     * 更新个人信息
     */
    void updateProfile(UserUpdateDTO dto);

    /**
     * 修改密码
     */
    void updatePassword(PasswordUpdateDTO dto);

    /**
     * 登出
     */
    void logout(String accessToken);

    /**
     * 刷新Token
     */
    LoginVO refreshToken(String refreshToken);
}
