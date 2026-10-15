package com.visualbuilder.page.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_page_version")
public class PageVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pageId;
    private Integer versionNo;
    private String schemaSnapshot;
    private String changelog;
    private Long userId;
    private LocalDateTime createdTime;
}
