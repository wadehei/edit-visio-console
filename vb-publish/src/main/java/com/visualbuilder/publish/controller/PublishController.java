package com.visualbuilder.publish.controller;

import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.common.core.result.R;
import com.visualbuilder.common.security.annotation.OpLog;
import com.visualbuilder.publish.dto.PublishResultVO;
import com.visualbuilder.publish.entity.PublishRecord;
import com.visualbuilder.publish.service.PublishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "发布管理")
@RestController
@RequiredArgsConstructor
public class PublishController {

    private final PublishService publishService;

    @Operation(summary = "发布页面")
    @OpLog(module = "发布管理", action = "发布页面")
    @PostMapping("/api/v1/projects/{projectId}/pages/{pageId}/publish")
    public R<PublishResultVO> publish(
            @PathVariable Long projectId,
            @PathVariable Long pageId,
            @RequestParam(required = false) String changelog) {
        return R.ok(publishService.publish(projectId, pageId, changelog));
    }

    @Operation(summary = "发布历史（分页）")
    @GetMapping("/api/v1/projects/{projectId}/publish-history")
    public R<PageResult<PublishRecord>> publishHistory(
            @PathVariable Long projectId,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        return R.ok(publishService.getPublishHistory(projectId, page, pageSize));
    }
}
