package com.visualbuilder.resource.entity;

import com.visualbuilder.common.core.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_resource")
public class Resource extends BaseEntity {
    private Long userId;
    private String resourceType;
    private String fileName;
    private String storagePath;
    private String url;
    private Long fileSize;
    private String mimeType;
    private Integer width;
    private Integer height;
    private String md5Hash;
    private String groupName;
}
