package com.visualbuilder.page.controller;

import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.common.core.result.R;
import com.visualbuilder.common.security.annotation.OpLog;
import com.visualbuilder.page.dto.PageCreateDTO;
import com.visualbuilder.page.dto.PageSchemaDTO;
import com.visualbuilder.page.dto.PageVO;
import com.visualbuilder.page.entity.PageVersion;
import com.visualbuilder.page.service.PageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/pages")
@RequiredArgsConstructor
@Tag(name = "页面管理")
public class PageController {

    private final PageService pageService;

    @OpLog(module = "页面管理", action = "创建页面")
    @Operation(summary = "创建页面")
    @PostMapping
    public R<PageVO> createPage(@PathVariable Long projectId,
                                @Valid @RequestBody PageCreateDTO dto) {
        return R.ok(pageService.createPage(projectId, dto));
    }

    @Operation(summary = "页面详情")
    @GetMapping("/{pageId}")
    public R<PageVO> getPageDetail(@PathVariable Long projectId,
                                   @PathVariable Long pageId) {
        return R.ok(pageService.getPageDetail(pageId));
    }

    @Operation(summary = "页面列表")
    @GetMapping
    public R<PageResult<PageVO>> listPages(@PathVariable Long projectId,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int pageSize,
                                           @RequestParam(required = false) String keyword) {
        return R.ok(pageService.listPages(projectId, page, pageSize, keyword));
    }

    @OpLog(module = "页面管理", action = "更新页面信息")
    @Operation(summary = "更新页面信息")
    @PutMapping("/{pageId}")
    public R<PageVO> updatePageInfo(@PathVariable Long projectId,
                                    @PathVariable Long pageId,
                                    @Valid @RequestBody PageCreateDTO dto) {
        return R.ok(pageService.updatePageInfo(pageId, dto));
    }

    @OpLog(module = "页面管理", action = "保存页面Schema")
    @Operation(summary = "保存页面Schema")
    @PutMapping("/{pageId}/schema")
    public R<PageVO> saveSchema(@PathVariable Long projectId,
                                @PathVariable Long pageId,
                                @Valid @RequestBody PageSchemaDTO dto) {
        return R.ok(pageService.saveSchema(pageId, dto));
    }

    @OpLog(module = "页面管理", action = "删除页面")
    @Operation(summary = "删除页面")
    @DeleteMapping("/{pageId}")
    public R<Void> deletePage(@PathVariable Long projectId,
                              @PathVariable Long pageId) {
        pageService.deletePage(pageId);
        return R.ok();
    }

    @OpLog(module = "页面管理", action = "批量删除页面")
    @Operation(summary = "批量删除页面")
    @DeleteMapping("/batch")
    public R<Map<String, Object>> batchDeletePages(@PathVariable Long projectId,
                                                   @RequestBody List<Long> ids) {
        return R.ok(pageService.batchDeletePages(ids));
    }

    @Operation(summary = "版本列表")
    @GetMapping("/{pageId}/versions")
    public R<List<PageVersion>> getVersionList(@PathVariable Long projectId,
                                               @PathVariable Long pageId) {
        return R.ok(pageService.getVersionList(pageId));
    }

    @OpLog(module = "页面管理", action = "回滚版本")
    @Operation(summary = "回滚版本")
    @PostMapping("/{pageId}/versions/{versionId}/rollback")
    public R<PageVO> rollbackVersion(@PathVariable Long projectId,
                                     @PathVariable Long pageId,
                                     @PathVariable Long versionId) {
        return R.ok(pageService.rollbackVersion(pageId, versionId));
    }

    @Operation(summary = "页面回收站")
    @GetMapping("/trash")
    public R<PageResult<PageVO>> listTrash(@PathVariable Long projectId,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(pageService.listTrash(projectId, page, pageSize));
    }

    @OpLog(module = "页面管理", action = "恢复页面")
    @Operation(summary = "恢复页面")
    @PostMapping("/{pageId}/restore")
    public R<Void> restorePage(@PathVariable Long projectId,
                               @PathVariable Long pageId) {
        pageService.restorePage(pageId);
        return R.ok();
    }

    @OpLog(module = "页面管理", action = "永久删除页面")
    @Operation(summary = "永久删除页面")
    @DeleteMapping("/{pageId}/permanent")
    public R<Void> permanentDeletePage(@PathVariable Long projectId,
                                       @PathVariable Long pageId) {
        pageService.permanentDeletePage(pageId);
        return R.ok();
    }
}
