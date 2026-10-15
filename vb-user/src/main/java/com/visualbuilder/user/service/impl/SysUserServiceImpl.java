package com.visualbuilder.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.security.jwt.JwtUtils;
import com.visualbuilder.common.security.utils.SecurityUtils;
import com.visualbuilder.user.converter.UserConverter;
import com.visualbuilder.user.dto.*;
import com.visualbuilder.user.entity.SysUser;
import com.visualbuilder.user.entity.SysUserRole;
import com.visualbuilder.user.mapper.SysUserMapper;
import com.visualbuilder.user.mapper.SysUserRoleMapper;
import com.visualbuilder.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends BaseServiceImpl<SysUserMapper, SysUser>
        implements SysUserService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final SysUserRoleMapper userRoleMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser register(RegisterDTO dto) {
        // 校验密码
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(41001, "两次密码不一致");
        }

        // 检查用户名是否已存在
        long count = this.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(40006, "用户名已存在");
        }

        // 检查邮箱是否已存在
        if (StringUtils.hasText(dto.getEmail())) {
            count = this.count(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getEmail, dto.getEmail()));
            if (count > 0) {
                throw new BusinessException(40006, "邮箱已被注册");
            }
        }

        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setNickname(dto.getUsername());
        user.setStatus(1);
        user.setTenantId(0L);
        this.save(user);

        // TODO: 分配默认角色 (USER)
        log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());
        return user;
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // 查找用户（支持用户名/邮箱/手机号登录）
        SysUser user = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getLogin())
                .or().eq(SysUser::getEmail, dto.getLogin())
                .or().eq(SysUser::getPhone, dto.getLogin()));

        if (user == null) {
            throw new BusinessException(40001, "用户名或密码错误");
        }

        // 校验密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(40001, "用户名或密码错误");
        }

        // 检查状态
        if (user.getStatus() != 1) {
            throw new BusinessException(40001, "账号已被禁用");
        }

        // 查询角色和权限
        List<String> roleKeys = baseMapper.selectRoleKeysByUserId(user.getId());
        List<String> permissions = baseMapper.selectPermissionKeysByUserId(user.getId());

        // 签发Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roleKeys);
        claims.put("permissions", permissions);

        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), claims);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        // 更新登录信息
        user.setLoginIp(""); // TODO: 从 request 获取
        user.setLoginDate(LocalDateTime.now());
        this.updateById(user);

        // 构建返回
        String role = roleKeys.isEmpty() ? "USER" : roleKeys.get(0);
        UserInfoVO userInfo = UserConverter.toUserInfoVO(user, permissions);
        userInfo.setRole(role);

        return LoginVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(1800)
                .user(userInfo)
                .build();
    }

    @Override
    public UserInfoVO getCurrentUserInfo() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(40001, "未登录");
        }

        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(40005, "用户不存在");
        }

        List<String> roleKeys = baseMapper.selectRoleKeysByUserId(userId);
        List<String> permissions = baseMapper.selectPermissionKeysByUserId(userId);

        UserInfoVO info = UserConverter.toUserInfoVO(user, permissions);
        info.setRole(roleKeys.isEmpty() ? "USER" : roleKeys.get(0));
        return info;
    }

    @Override
    public void updateProfile(UserUpdateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(40005, "用户不存在");
        }

        if (StringUtils.hasText(dto.getNickname())) user.setNickname(dto.getNickname());
        if (StringUtils.hasText(dto.getAvatar())) user.setAvatar(dto.getAvatar());
        if (StringUtils.hasText(dto.getPhone())) user.setPhone(dto.getPhone());
        if (StringUtils.hasText(dto.getEmail())) user.setEmail(dto.getEmail());

        this.updateById(user);
    }

    @Override
    public void updatePassword(PasswordUpdateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(40005, "用户不存在");
        }

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(41001, "旧密码错误");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setPwdUpdateDate(LocalDateTime.now());
        this.updateById(user);
    }

    @Override
    public void logout(String accessToken) {
        if (!StringUtils.hasText(accessToken)) return;

        try {
            String jti = jwtUtils.getJti(accessToken);
            long ttl = jwtUtils.getRemainingTtl(accessToken);
            if (ttl > 0) {
                redisTemplate.opsForValue().set("token:blacklist:" + jti, "LOGOUT",
                        java.time.Duration.ofMillis(ttl));
            }
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId != null) {
                redisTemplate.delete("token:refresh:" + userId);
            }
        } catch (Exception e) {
            log.warn("登出处理异常: {}", e.getMessage());
        }
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new BusinessException(40002, "Refresh Token 无效或已过期");
        }

        Long userId = jwtUtils.getUserId(refreshToken);
        SysUser user = this.getById(userId);
        if (user == null || user.getStatus() != 1) {
            throw new BusinessException(40001, "用户不存在或已禁用");
        }

        List<String> roleKeys = baseMapper.selectRoleKeysByUserId(userId);
        List<String> permissions = baseMapper.selectPermissionKeysByUserId(userId);

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roleKeys);
        claims.put("permissions", permissions);

        String newAccessToken = jwtUtils.generateAccessToken(userId, user.getUsername(), claims);
        String newRefreshToken = jwtUtils.generateRefreshToken(userId);

        String role = roleKeys.isEmpty() ? "USER" : roleKeys.get(0);
        UserInfoVO userInfo = UserConverter.toUserInfoVO(user, permissions);
        userInfo.setRole(role);

        return LoginVO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(1800)
                .user(userInfo)
                .build();
    }
}
