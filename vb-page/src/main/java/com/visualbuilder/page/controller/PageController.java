package com.visualbuilder.page.controller;

import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.common.core.result.R;
import com.visualbuilder.page.dto.PageCreateDTO;
import com.visualbuilder.page.dto.PageSchemaDTO;
import com.visualbuilder.page.dto.PageVO;
import com.visualbuilder.page.entity.PageVersion;
import com.visualbuilder.page.service.PageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/pages")
@RequiredArgsConstructor
public class PageController {

    private final PageService pageService;

    @PostMapping
    public R<PageVO> createPage(@PathVariable Long projectId,
                                @Valid @RequestBody PageCreateDTO dto) {
        return R.ok(pageService.createPage(projectId, dto));
    }

    @GetMapping("/{pageId}")
    public R<PageVO> getPageDetail(@PathVariable Long projectId,
                                   @PathVariable Long pageId) {
        return R.ok(pageService.getPageDetail(pageId));
    }

    @GetMapping
    public R<PageResult<PageVO>> listPages(@PathVariable Long projectId,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int pageSize,
                                           @RequestParam(required = false) String keyword) {
        return R.ok(pageService.listPages(projectId, page, pageSize, keyword));
    }

    @PutMapping("/{pageId}")
    public R<PageVO> updatePageInfo(@PathVariable Long projectId,
                                    @PathVariable Long pageId,
                                    @Valid @RequestBody PageCreateDTO dto) {
        return R.ok(pageService.updatePageInfo(pageId, dto));
    }

    @PutMapping("/{pageId}/schema")
    public R<PageVO> saveSchema(@PathVariable Long projectId,
                                @PathVariable Long pageId,
                                @Valid @RequestBody PageSchemaDTO dto) {
        return R.ok(pageService.saveSchema(pageId, dto));
    }

    @DeleteMapping("/{pageId}")
    public R<Void> deletePage(@PathVariable Long projectId,
                              @PathVariable Long pageId) {
        pageService.deletePage(pageId);
        return R.ok();
    }

    @GetMapping("/{pageId}/versions")
    public R<List<PageVersion>> getVersionList(@PathVariable Long projectId,
                                               @PathVariable Long pageId) {
        return R.ok(pageService.getVersionList(pageId));
    }

    @PostMapping("/{pageId}/versions/{versionId}/rollback")
    public R<PageVO> rollbackVersion(@PathVariable Long projectId,
                                     @PathVariable Long pageId,
                                     @PathVariable Long versionId) {
        return R.ok(pageService.rollbackVersion(pageId, versionId));
    }
}
