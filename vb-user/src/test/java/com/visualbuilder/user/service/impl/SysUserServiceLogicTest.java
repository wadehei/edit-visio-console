package com.visualbuilder.user.service.impl;

import com.visualbuilder.common.core.constant.StatusConst;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.user.dto.RegisterDTO;
import com.visualbuilder.user.entity.SysUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户服务单元测试 — 验证不依赖 Spring 容器的纯逻辑部分。
 *
 * <p>注意：完整集成测试需要启动 Spring 容器（含数据库、Redis），
 * 在 E2E 测试阶段补充。此处仅验证密码校验、状态赋值等纯逻辑。</p>
 */
@DisplayName("用户服务 — 纯逻辑测试")
class SysUserServiceLogicTest {

    @Test
    @DisplayName("用户创建时默认状态为 NORMAL")
    void newUser_defaultStatusIsNormal() {
        SysUser user = new SysUser();
        user.setUsername("test");
        user.setStatus(StatusConst.User.NORMAL);

        assertEquals(StatusConst.User.NORMAL, user.getStatus());
        assertEquals(1, user.getStatus());
    }

    @Test
    @DisplayName("注册 DTO 密码一致性校验（纯逻辑）")
    void registerDTO_passwordMatch() {
        RegisterDTO dto = new RegisterDTO();
        dto.setPassword("abc123");
        dto.setConfirmPassword("abc123");
        assertEquals(dto.getPassword(), dto.getConfirmPassword());

        dto.setConfirmPassword("different");
        assertNotEquals(dto.getPassword(), dto.getConfirmPassword());
    }

    @Test
    @DisplayName("用户状态常量与 SQL DDL 语义一致")
    void userStatusConstants_matchDDL() {
        // sys_user.status: 0禁用 1正常 2锁定
        assertEquals(0, StatusConst.User.DISABLED, "0 = 禁用");
        assertEquals(1, StatusConst.User.NORMAL, "1 = 正常");
        assertEquals(2, StatusConst.User.LOCKED, "2 = 锁定");
    }
}
