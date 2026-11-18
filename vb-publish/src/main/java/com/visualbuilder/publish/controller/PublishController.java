package com.visualbuilder.publish.controller;

import com.visualbuilder.common.core.constant.LimitsConst;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.common.core.result.R;
import com.visualbuilder.common.security.annotation.OpLog;
import com.visualbuilder.common.security.annotation.RateLimit;
import com.visualbuilder.publish.dto.PublishRecordVO;
import com.visualbuilder.publish.dto.PublishResultVO;
import com.visualbuilder.publish.service.PublishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "发布管理")
@RestController
@Validated
@RequiredArgsConstructor
public class PublishController {

    private final PublishService publishService;

    @Operation(summary = "发布页面")
    @OpLog(module = "发布管理", action = "发布页面")
    @RateLimit(key = "publish", maxRequests = 10, windowSeconds = 60,
            message = "发布过于频繁，请稍后再试")
    @PostMapping("/api/v1/projects/{projectId}/pages/{pageId}/publish")
    public R<PublishResultVO> publish(
            @PathVariable Long projectId,
            @PathVariable Long pageId,
            @RequestParam(required = false)
            @Size(max = LimitsConst.MAX_TEXT_LENGTH,
                    message = "发布备注最多 " + LimitsConst.MAX_TEXT_LENGTH + " 个字符") String changelog) {
        return R.ok(publishService.publish(projectId, pageId, changelog));
    }

    @Operation(summary = "查询发布状态")
    @GetMapping("/api/v1/publish-records/{recordId}/status")
    public R<PublishRecordVO> publishStatus(@PathVariable Long recordId) {
        return R.ok(publishService.getPublishStatus(recordId));
    }

    @Operation(summary = "发布历史（分页）")
    @GetMapping("/api/v1/projects/{projectId}/publish-history")
    public R<PageResult<PublishRecordVO>> publishHistory(
            @PathVariable Long projectId,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        return R.ok(publishService.getPublishHistory(projectId, page, pageSize));
    }
}
