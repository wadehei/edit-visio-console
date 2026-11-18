package com.visualbuilder.user.dto;

import com.visualbuilder.user.entity.SysUser;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台用户视图对象 — 仅暴露必要的展示字段，
 * 避免直接返回实体导致内部字段（密码、租户、审计字段等）外泄。
 */
@Data
public class AdminUserVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Integer sex;
    private Long deptId;
    private Integer status;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime loginDate;

    public static AdminUserVO of(SysUser user) {
        if (user == null) {
            return null;
        }
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setSex(user.getSex());
        vo.setDeptId(user.getDeptId());
        vo.setStatus(user.getStatus());
        vo.setRemark(user.getRemark());
        vo.setCreatedTime(user.getCreatedTime());
        vo.setLoginDate(user.getLoginDate());
        return vo;
    }
}
