package com.visualbuilder.template.controller;

import com.visualbuilder.common.core.result.R;
import com.visualbuilder.common.security.annotation.OpLog;
import com.visualbuilder.common.security.annotation.RequiresRole;
import com.visualbuilder.template.dto.TemplateApplyVO;
import com.visualbuilder.template.dto.TemplateDetailVO;
import com.visualbuilder.template.entity.Template;
import com.visualbuilder.template.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public R<TemplateDetailVO> detail(@PathVariable Long id) {
        return R.ok(templateService.getTemplateDetail(id));
    }

    @OpLog(module = "模板管理", action = "应用模板")
    @Operation(summary = "应用模板")
    @PostMapping("/{id}/apply")
    public R<TemplateApplyVO> apply(
            @PathVariable Long id,
            @RequestParam Long targetProjectId) {
        return R.ok(templateService.applyTemplate(id, targetProjectId));
    }

    @RequiresRole("SUPER_ADMIN")
    @OpLog(module = "模板管理", action = "创建模板")
    @Operation(summary = "创建模板")
    @PostMapping
    public R<Void> create(@RequestBody Template template) {
        templateService.createTemplate(template);
        return R.ok();
    }

    @RequiresRole("SUPER_ADMIN")
    @OpLog(module = "模板管理", action = "更新模板")
    @Operation(summary = "更新模板")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Template template) {
        templateService.updateTemplate(id, template);
        return R.ok();
    }

    @RequiresRole("SUPER_ADMIN")
    @OpLog(module = "模板管理", action = "删除模板")
    @Operation(summary = "删除模板")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return R.ok();
    }
}
