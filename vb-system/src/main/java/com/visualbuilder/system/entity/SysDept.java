package com.visualbuilder.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部门实体。
 */
@Data
@TableName("sys_dept")
public class SysDept implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long parentId;
    private String ancestors;
    private String deptName;
    private Integer sortOrder;
    private String leader;
    private String phone;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
