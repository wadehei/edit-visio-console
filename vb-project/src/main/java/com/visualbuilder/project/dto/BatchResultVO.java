package com.visualbuilder.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchResultVO {
    @Schema(description = "总数")
    private int total;
    @Schema(description = "成功数")
    private int success;
    @Schema(description = "跳过数")
    private int skipped;
}
