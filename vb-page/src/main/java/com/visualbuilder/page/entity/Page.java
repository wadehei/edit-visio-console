package com.visualbuilder.page.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.visualbuilder.common.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_page")
public class Page extends BaseEntity {
    private Long projectId;
    private String name;
    private String pagePath;
    private String pageType;
    private String schemaContent;
    private Integer schemaVersion;
    private String coverUrl;
    private Integer status;
    private Integer isHome;
    private Long userId;
}
