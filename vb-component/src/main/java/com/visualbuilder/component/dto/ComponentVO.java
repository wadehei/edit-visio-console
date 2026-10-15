package com.visualbuilder.component.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class ComponentVO {
    private Long componentId;
    private String name;
    private String type;
    private String categoryName;
    private String description;
    private String thumbnail;
    private Map<String, Object> propsSchema;
    private Map<String, Object> defaultProps;
    private Map<String, Object> defaultStyles;
    private Boolean isSystem;
    private Integer sortOrder;
}
