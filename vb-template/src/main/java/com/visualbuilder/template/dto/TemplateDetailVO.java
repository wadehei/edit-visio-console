package com.visualbuilder.template.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class TemplateDetailVO {
    private Long id;
    private String name;
    private String description;
    private String coverUrl;
    private String pageSchema;
    private String tags;
    private Integer useCount;
    private Integer favoriteCount;
    private Integer isOfficial;
    private LocalDateTime createdTime;
}
