package com.visualbuilder.publish.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PublishResultVO {
    private Long id;
    private Integer versionNo;
    private String accessUrl;
    private LocalDateTime publishedAt;
}
