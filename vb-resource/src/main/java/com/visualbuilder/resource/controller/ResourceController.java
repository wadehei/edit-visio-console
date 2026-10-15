package com.visualbuilder.resource.controller;

import com.visualbuilder.common.core.result.R;
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

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public R<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderId", required = false) String folderId) {
        return R.ok(resourceService.upload(file, folderId));
    }

    @Operation(summary = "资源列表")
    @GetMapping
    public R<List<Resource>> list(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String groupName) {
        List<Resource> result = resourceService.list();
        return R.ok(result);
    }

    @Operation(summary = "删除资源")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return R.ok();
    }
}
