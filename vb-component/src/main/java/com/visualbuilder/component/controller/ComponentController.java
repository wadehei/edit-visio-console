package com.visualbuilder.component.controller;

import com.visualbuilder.common.core.result.R;
import com.visualbuilder.component.dto.ComponentVO;
import com.visualbuilder.component.service.ComponentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "组件管理")
@RestController
@RequestMapping("/api/v1/components")
@RequiredArgsConstructor
public class ComponentController {

    private final ComponentService componentService;

    @Operation(summary = "组件列表")
    @GetMapping
    public R<List<ComponentVO>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return R.ok(componentService.listComponents(categoryId, keyword));
    }

    @Operation(summary = "组件详情")
    @GetMapping("/{componentId}")
    public R<ComponentVO> detail(@PathVariable Long componentId) {
        return R.ok(componentService.getComponentDetail(componentId));
    }
}
