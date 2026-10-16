package com.visualbuilder.resource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件资源实体 — SQL 仅有 created_time/deleted，不继承 BaseEntity。
 */
@Data
@TableName("res_file")
public class Resource implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

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
    private LocalDateTime createdTime;

    @TableLogic
    private Integer deleted;
}
