package com.visualbuilder.publish.entity;

import com.visualbuilder.common.core.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_publish_record")
public class PublishRecord extends BaseEntity {
    private Long pageId;
    private String versionNo;
    private Integer status;
    private String accessUrl;
    private String changelog;
    private Long publishedBy;
    private LocalDateTime publishedAt;
}
