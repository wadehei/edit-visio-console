package com.visualbuilder.publish.controller;

import com.visualbuilder.common.core.result.R;
import com.visualbuilder.publish.entity.PublishRecord;
import com.visualbuilder.publish.service.PublishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "发布管理")
@RestController
@RequiredArgsConstructor
public class PublishController {

    private final PublishService publishService;

    @Operation(summary = "发布页面")
    @PostMapping("/api/v1/projects/{projectId}/pages/{pageId}/publish")
    public R<Map<String, Object>> publish(
            @PathVariable Long projectId,
            @PathVariable Long pageId,
            @RequestParam(required = false) String changelog) {
        return R.ok(publishService.publish(projectId, pageId, changelog));
    }

    @Operation(summary = "发布历史")
    @GetMapping("/api/v1/projects/{projectId}/publish-history")
    public R<List<PublishRecord>> publishHistory(@PathVariable Long projectId) {
        return R.ok(publishService.getPublishHistory(projectId));
    }
}
