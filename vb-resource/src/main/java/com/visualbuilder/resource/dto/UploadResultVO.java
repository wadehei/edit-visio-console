package com.visualbuilder.resource.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadResultVO {
    private Long id;
    private String url;
    private String fileName;
    private String resourceType;
    /** 文件字节大小；使用基本类型以免被全局 Long→String 序列化影响前端数值展示 */
    private long fileSize;
}
