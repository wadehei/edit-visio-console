package com.visualbuilder.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.constant.StatusConst;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.security.jwt.JwtUtils;
import com.visualbuilder.common.security.utils.SecurityUtils;
import com.visualbuilder.common.util.IpUtils;
import com.visualbuilder.user.converter.UserConverter;
import com.visualbuilder.user.dto.*;
import com.visualbuilder.user.entity.SysRole;
import com.visualbuilder.user.entity.SysUser;
import com.visualbuilder.user.entity.SysUserRole;
import com.visualbuilder.user.mapper.SysRoleMapper;
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

import java.time.Duration;
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
    private final SysRoleMapper sysRoleMapper;
    private final StringRedisTemplate redisTemplate;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser register(RegisterDTO dto) {
        // 校验密码
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次密码不一致");
        }

        // 检查用户名是否已存在
        long count = this.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
        }

        // 检查邮箱是否已存在
        if (StringUtils.hasText(dto.getEmail())) {
            count = this.count(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getEmail, dto.getEmail()));
            if (count > 0) {
                throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "邮箱已被注册");
            }
        }

        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setNickname(dto.getUsername());
        user.setStatus(StatusConst.User.NORMAL);
        user.setTenantId(0L);
        this.save(user);

        // 分配默认角色 USER
        assignDefaultRole(user.getId());

        log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginDTO dto) {
        String loginKey = dto.getLogin();

        // 检查是否被锁定
        String lockKey = "login:lock:" + loginKey;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            Long ttl = redisTemplate.getExpire(lockKey);
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED,
                    "登录尝试次数过多，请 " + (ttl != null ? ttl / 60 + 1 : 15) + " 分钟后重试");
        }

        // 查找用户（支持用户名/邮箱/手机号登录）
        SysUser user = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, loginKey)
                .or().eq(SysUser::getEmail, loginKey)
                .or().eq(SysUser::getPhone, loginKey));

        if (user == null) {
            recordLoginFailure(loginKey);
            throw new BusinessException(ErrorCode.PASSWORD_WRONG, "用户名或密码错误");
        }

        // 校验密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            recordLoginFailure(loginKey);
            throw new BusinessException(ErrorCode.PASSWORD_WRONG, "用户名或密码错误");
        }

        // 检查状态
        if (user.getStatus() != StatusConst.User.NORMAL) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "账号已被禁用");
        }

        // 登录成功，清除失败计数
        redisTemplate.delete("login:fail:" + loginKey);

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
        user.setLoginIp(IpUtils.getClientIp());
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
                .expiresIn(jwtUtils.getAccessTokenExpire())
                .user(userInfo)
                .build();
    }

    @Override
    public UserInfoVO getCurrentUserInfo() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        List<String> roleKeys = baseMapper.selectRoleKeysByUserId(userId);
        List<String> permissions = baseMapper.selectPermissionKeysByUserId(userId);

        UserInfoVO info = UserConverter.toUserInfoVO(user, permissions);
        info.setRole(roleKeys.isEmpty() ? "USER" : roleKeys.get(0));
        return info;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(UserUpdateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        if (StringUtils.hasText(dto.getNickname())) user.setNickname(dto.getNickname());
        if (StringUtils.hasText(dto.getAvatar())) user.setAvatar(dto.getAvatar());
        if (StringUtils.hasText(dto.getPhone())) user.setPhone(dto.getPhone());
        if (StringUtils.hasText(dto.getEmail())) user.setEmail(dto.getEmail());

        this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(PasswordUpdateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "旧密码错误");
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
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Refresh Token 无效或已过期");
        }

        Long userId = jwtUtils.getUserId(refreshToken);
        SysUser user = this.getById(userId);
        if (user == null || user.getStatus() != StatusConst.User.NORMAL) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "用户不存在或已禁用");
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
                .expiresIn(jwtUtils.getAccessTokenExpire())
                .user(userInfo)
                .build();
    }

    /**
     * 记录登录失败次数，超过阈值则锁定账号
     */
    private void recordLoginFailure(String loginKey) {
        String failKey = "login:fail:" + loginKey;
        Long attempts = redisTemplate.opsForValue().increment(failKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(failKey, LOCK_DURATION);
        }
        if (attempts != null && attempts >= MAX_LOGIN_ATTEMPTS) {
            redisTemplate.opsForValue().set("login:lock:" + loginKey, "LOCKED", LOCK_DURATION);
            log.warn("登录失败次数过多，账号已锁定: loginKey={}", loginKey);
        }
    }

    /**
     * 为新注册用户分配默认角色 USER（优先查缓存，回退查数据库）
     */
    private void assignDefaultRole(Long userId) {
        Long roleId = resolveDefaultRoleId();

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRoleMapper.insert(userRole);
        log.debug("分配默认角色: userId={}, roleId={}", userId, roleId);
    }

    private Long resolveDefaultRoleId() {
        // 优先从 Redis 缓存读取
        String cached = redisTemplate.opsForValue().get("sys:role:id:USER");
        if (StringUtils.hasText(cached)) {
            return Long.parseLong(cached);
        }
        // 回退：从数据库查询
        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, "USER")
                .eq(SysRole::getStatus, StatusConst.Enable.ENABLED)
                .select(SysRole::getId)
                .last("LIMIT 1"));
        if (role != null) {
            // 写入缓存，加速后续查询
            redisTemplate.opsForValue().set("sys:role:id:USER", String.valueOf(role.getId()));
            return role.getId();
        }
        throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "默认角色 USER 不存在");
    }
}
