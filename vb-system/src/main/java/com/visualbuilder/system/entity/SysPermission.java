package com.visualbuilder.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统权限/菜单实体。
 */
@Data
@TableName("sys_permission")
public class SysPermission implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;
    private String permissionName;
    private String permissionKey;
    private Integer permissionType;
    private String path;
    private String component;
    private String icon;
    private Integer sortOrder;
    private Integer visible;
    private Integer status;
    private String apiMethod;
    private String apiPath;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;

    /** 子权限节点（树形结构返回时填充，非数据库字段） */
    @TableField(exist = false)
    private List<SysPermission> children;
}
