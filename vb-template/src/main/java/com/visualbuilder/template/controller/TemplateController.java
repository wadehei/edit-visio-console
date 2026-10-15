package com.visualbuilder.template.controller;

import com.visualbuilder.common.core.result.R;
import com.visualbuilder.template.entity.Template;
import com.visualbuilder.template.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "模板管理")
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @Operation(summary = "模板列表")
    @GetMapping
    public R<List<Template>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag) {
        return R.ok(templateService.listTemplates(categoryId, keyword, tag));
    }

    @Operation(summary = "模板详情")
    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(templateService.getTemplateDetail(id));
    }

    @Operation(summary = "应用模板")
    @PostMapping("/{id}/apply")
    public R<Map<String, Object>> apply(
            @PathVariable Long id,
            @RequestParam Long targetProjectId) {
        return R.ok(templateService.applyTemplate(id, targetProjectId));
    }
}
