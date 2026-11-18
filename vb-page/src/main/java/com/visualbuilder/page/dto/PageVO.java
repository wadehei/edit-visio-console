package com.visualbuilder.page.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class PageVO {
    private Long pageId;
    private Long projectId;
    private String title;
    private String path;
    private Boolean isHome;
    private Integer status;
    private Integer version;
    private Map<String, Object> schema;
    /** Schema 字节大小；使用基本类型以免被全局 Long→String 序列化影响前端数值展示 */
    private long schemaSize;
    private Integer componentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
