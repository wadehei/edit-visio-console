package com.visualbuilder.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.constant.RedisKeyConst;
import com.visualbuilder.common.core.constant.StatusConst;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.user.dto.AdminUserCreateDTO;
import com.visualbuilder.user.dto.AdminUserUpdateDTO;
import com.visualbuilder.user.entity.SysUser;
import com.visualbuilder.user.entity.SysUserRole;
import com.visualbuilder.user.mapper.SysUserMapper;
import com.visualbuilder.user.mapper.SysUserRoleMapper;
import com.visualbuilder.user.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl extends BaseServiceImpl<SysUserMapper, SysUser>
        implements AdminUserService {

    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final SysUserRoleMapper userRoleMapper;

    /** 禁用标记有效期，取刷新令牌有效期（秒），确保禁用状态覆盖令牌最长生命周期 */
    @Value("${jwt.refresh-token-expire:604800}")
    private long disabledFlagTtlSeconds;

    @Override
    public PageResult<SysUser> listUsers(int page, int pageSize, String keyword, Integer status) {
        Page<SysUser> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(keyword), SysUser::getUsername, keyword)
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getCreatedTime);
        Page<SysUser> result = this.page(pageParam, wrapper);
        // 清除密码字段，避免泄露
        result.getRecords().forEach(u -> u.setPassword(null));
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(AdminUserCreateDTO dto) {
        // 校验用户名唯一
        long count = this.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
        }
        if (dto.getStatus() != null) {
            validateStatus(dto.getStatus());
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setAvatar(dto.getAvatar());
        user.setSex(dto.getSex());
        user.setDeptId(dto.getDeptId());
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : StatusConst.User.NORMAL);
        user.setRemark(dto.getRemark());
        user.setTenantId(0L);
        try {
            this.save(user);
        } catch (DuplicateKeyException e) {
            // 并发创建同一用户名时，依赖数据库唯一约束兜底
            throw new BusinessException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
        }
        log.info("管理员创建用户: username={}", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long userId, AdminUserUpdateDTO dto) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getAvatar() != null) user.setAvatar(dto.getAvatar());
        if (dto.getSex() != null) user.setSex(dto.getSex());
        if (dto.getDeptId() != null) user.setDeptId(dto.getDeptId());
        if (dto.getRemark() != null) user.setRemark(dto.getRemark());
        this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String newPassword) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(user);
        log.info("管理员重置密码: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, Integer status) {
        validateStatus(status);
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        user.setStatus(status);
        this.updateById(user);

        // 同步 Redis 禁用标记，使 JWT Filter 即时生效
        String disabledKey = RedisKeyConst.userDisabled(userId);
        if (status == StatusConst.User.DISABLED) {
            redisTemplate.opsForValue().set(disabledKey, "1", Duration.ofSeconds(disabledFlagTtlSeconds));
        } else {
            redisTemplate.delete(disabledKey);
        }
        log.info("管理员更新用户状态: userId={}, status={}", userId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        // 先清理用户-角色关联，避免删除用户后残留孤儿数据
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        this.removeById(userId);
        log.info("删除用户: userId={}", userId);
    }

    /**
     * 校验用户状态取值合法（仅允许 1=正常 / 0=禁用）。
     */
    private void validateStatus(Integer status) {
        if (status == null
                || (status != StatusConst.User.NORMAL && status != StatusConst.User.DISABLED)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "用户状态取值非法，仅允许 " + StatusConst.User.NORMAL + "(正常) 或 "
                            + StatusConst.User.DISABLED + "(禁用)");
        }
    }
}
