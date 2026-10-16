package com.visualbuilder.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.constant.StatusConst;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.user.entity.SysUser;
import com.visualbuilder.user.mapper.SysUserMapper;
import com.visualbuilder.user.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl extends BaseServiceImpl<SysUserMapper, SysUser>
        implements AdminUserService {

    private final PasswordEncoder passwordEncoder;

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
    public void createUser(SysUser user) {
        // 校验用户名唯一
        long count = this.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
        }

        if (!StringUtils.hasText(user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码不能为空");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(StatusConst.User.NORMAL);
        }
        if (user.getNickname() == null) {
            user.setNickname(user.getUsername());
        }
        if (user.getTenantId() == null) {
            user.setTenantId(0L);
        }
        this.save(user);
        log.info("管理员创建用户: username={}", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long userId, SysUser updates) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        if (StringUtils.hasText(updates.getNickname())) user.setNickname(updates.getNickname());
        if (updates.getEmail() != null) user.setEmail(updates.getEmail());
        if (updates.getPhone() != null) user.setPhone(updates.getPhone());
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
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        user.setStatus(status);
        this.updateById(user);
        log.info("管理员更新用户状态: userId={}, status={}", userId, status);
    }
}
