package com.visualbuilder.component.controller;

import com.visualbuilder.common.core.result.R;
import com.visualbuilder.common.security.annotation.OpLog;
import com.visualbuilder.common.security.annotation.RequiresRole;
import com.visualbuilder.component.dto.ComponentCreateDTO;
import com.visualbuilder.component.dto.ComponentUpdateDTO;
import com.visualbuilder.component.dto.ComponentVO;
import com.visualbuilder.component.service.ComponentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    @RequiresRole("SUPER_ADMIN")
    @OpLog(module = "组件管理", action = "创建组件")
    @Operation(summary = "创建组件")
    @PostMapping
    public R<Void> create(@Valid @RequestBody ComponentCreateDTO dto) {
        componentService.createComponent(dto);
        return R.ok();
    }

    @RequiresRole("SUPER_ADMIN")
    @OpLog(module = "组件管理", action = "更新组件")
    @Operation(summary = "更新组件")
    @PutMapping("/{componentId}")
    public R<Void> update(@PathVariable Long componentId, @Valid @RequestBody ComponentUpdateDTO dto) {
        componentService.updateComponent(componentId, dto);
        return R.ok();
    }

    @RequiresRole("SUPER_ADMIN")
    @OpLog(module = "组件管理", action = "删除组件")
    @Operation(summary = "删除组件")
    @DeleteMapping("/{componentId}")
    public R<Void> delete(@PathVariable Long componentId) {
        componentService.deleteComponent(componentId);
        return R.ok();
    }
}
