package com.visualbuilder.project.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ProjectVO {

    private Long projectId;

    private String name;

    private String description;

    private String coverImage;

    private Integer status;

    private Map<String, Object> settings;

    private Integer pageCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
