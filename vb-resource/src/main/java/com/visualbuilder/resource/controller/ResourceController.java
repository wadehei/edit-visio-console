package com.visualbuilder.resource.controller;

import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.common.core.result.R;
import com.visualbuilder.common.security.annotation.OpLog;
import com.visualbuilder.common.security.annotation.RateLimit;
import com.visualbuilder.resource.dto.UploadResultVO;
import com.visualbuilder.resource.entity.Resource;
import com.visualbuilder.resource.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "资源管理")
@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @OpLog(module = "资源管理", action = "上传文件")
    @RateLimit(key = "upload", maxRequests = 30, windowSeconds = 60, message = "上传过于频繁，请稍后再试")
    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public R<UploadResultVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderId", required = false) String folderId) {
        return R.ok(resourceService.upload(file, folderId));
    }

    @Operation(summary = "资源列表（分页）")
    @GetMapping
    public R<PageResult<Resource>> list(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        return R.ok(resourceService.listResources(resourceType, groupName, page, pageSize));
    }

    @OpLog(module = "资源管理", action = "删除资源")
    @Operation(summary = "删除资源")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return R.ok();
    }

    @OpLog(module = "资源管理", action = "批量删除资源")
    @Operation(summary = "批量删除资源")
    @DeleteMapping("/batch")
    public R<Map<String, Object>> batchDelete(@RequestBody java.util.List<Long> ids) {
        return R.ok(resourceService.batchDelete(ids));
    }

    @Operation(summary = "资源回收站")
    @GetMapping("/trash")
    public R<PageResult<Resource>> listTrash(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return R.ok(resourceService.listTrash(page, pageSize));
    }

    @OpLog(module = "资源管理", action = "恢复资源")
    @Operation(summary = "恢复资源")
    @PostMapping("/{id}/restore")
    public R<Void> restore(@PathVariable Long id) {
        resourceService.restoreResource(id);
        return R.ok();
    }

    @OpLog(module = "资源管理", action = "永久删除资源")
    @Operation(summary = "永久删除资源")
    @DeleteMapping("/{id}/permanent")
    public R<Void> permanentDelete(@PathVariable Long id) {
        resourceService.permanentDeleteResource(id);
        return R.ok();
    }
}
