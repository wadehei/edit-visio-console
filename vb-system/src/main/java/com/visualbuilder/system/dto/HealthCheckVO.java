package com.visualbuilder.system.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HealthCheckVO {
    private String status;
    private String service;
    private long timestamp;
}
