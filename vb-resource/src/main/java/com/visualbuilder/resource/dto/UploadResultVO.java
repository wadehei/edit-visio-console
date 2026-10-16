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
    private Long fileSize;
}
