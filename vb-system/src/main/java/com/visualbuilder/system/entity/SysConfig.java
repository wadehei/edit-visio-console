package com.visualbuilder.system.entity;

import com.visualbuilder.common.core.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseEntity {
    private String configKey;
    private String configValue;
    private String description;
    private Long updatedBy;
}
