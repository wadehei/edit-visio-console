package com.visualbuilder.publish.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 发布记录实体 — SQL 无 created_by/updated_by/updated_time/deleted，不继承 BaseEntity。
 */
@Data
@TableName("biz_publish_record")
public class PublishRecord implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long pageId;
    private Integer versionNo;
    private Integer status;
    private String accessUrl;
    private String changelog;
    private Long publishedBy;
    private LocalDateTime publishedAt;
    private LocalDateTime createdTime;
}
