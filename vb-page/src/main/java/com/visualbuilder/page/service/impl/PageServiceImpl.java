package com.visualbuilder.page.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.constant.StatusConst;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.common.security.utils.SecurityUtils;
import com.visualbuilder.page.dto.PageCreateDTO;
import com.visualbuilder.page.dto.PageSchemaDTO;
import com.visualbuilder.page.dto.PageVO;
import com.visualbuilder.page.entity.Page;
import com.visualbuilder.page.entity.PageVersion;
import com.visualbuilder.page.mapper.PageMapper;
import com.visualbuilder.page.mapper.PageVersionMapper;
import com.visualbuilder.page.service.PageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageServiceImpl extends BaseServiceImpl<PageMapper, Page>
        implements PageService {

    private final PageVersionMapper pageVersionMapper;
    private final ObjectMapper objectMapper;

    @Value("${business.page.max-version-count:50}")
    private int maxVersionCount;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageVO createPage(Long projectId, PageCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }

        Page page = new Page();
        page.setProjectId(projectId);
        page.setName(dto.getTitle());
        page.setPagePath(dto.getPath() != null ? dto.getPath() : "/page-" + System.currentTimeMillis());
        page.setPageType("h5");
        page.setSchemaContent(null);
        page.setSchemaVersion(0);
        page.setStatus(StatusConst.Page.EDITING);
        page.setIsHome(Boolean.TRUE.equals(dto.getIsHome()) ? StatusConst.YesNo.YES : StatusConst.YesNo.NO);
        page.setUserId(userId);
        save(page);

        return toVO(page);
    }

    @Override
    public PageVO getPageDetail(Long pageId) {
        Page page = getById(pageId);
        if (page == null) {
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND, "页面不存在");
        }
        return toVO(page);
    }

    @Override
    public PageResult<PageVO> listPages(Long projectId, int page, int pageSize, String keyword) {
        LambdaQueryWrapper<Page> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Page::getProjectId, projectId);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Page::getName, keyword);
        }
        wrapper.orderByDesc(Page::getUpdatedTime);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Page> pageParam =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Page> result = page(pageParam, wrapper);

        List<PageVO> voList = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(voList, result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageVO updatePageInfo(Long pageId, PageCreateDTO dto) {
        Page page = getById(pageId);
        if (page == null) {
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND, "页面不存在");
        }
        checkPageOwner(page);
        if (StringUtils.hasText(dto.getTitle())) page.setName(dto.getTitle());
        if (dto.getPath() != null) page.setPagePath(dto.getPath());
        if (dto.getIsHome() != null) page.setIsHome(dto.getIsHome() ? StatusConst.YesNo.YES : StatusConst.YesNo.NO);
        updateById(page);
        return toVO(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageVO saveSchema(Long pageId, PageSchemaDTO dto) {
        Page page = getById(pageId);
        if (page == null) {
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND, "页面不存在");
        }
        checkPageOwner(page);

        try {
            String schemaJson = objectMapper.writeValueAsString(dto.getSchema());
            page.setSchemaContent(schemaJson);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Schema 序列化失败");
        }

        int nextVersion = (page.getSchemaVersion() != null ? page.getSchemaVersion() : 0) + 1;
        page.setSchemaVersion(nextVersion);
        updateById(page);

        // 每5次保存创建版本快照
        if (nextVersion % 5 == 0) {
            createVersionSnapshot(page, nextVersion, dto.getIsAutoSave());
        }

        return toVO(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePage(Long pageId) {
        Page page = getById(pageId);
        if (page == null) {
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND, "页面不存在");
        }
        checkPageOwner(page);
        removeById(pageId);
    }

    @Override
    public List<PageVersion> getVersionList(Long pageId) {
        LambdaQueryWrapper<PageVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PageVersion::getPageId, pageId)
                .orderByDesc(PageVersion::getVersionNo);
        return pageVersionMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageVO rollbackVersion(Long pageId, Long versionId) {
        Page page = getById(pageId);
        if (page == null) {
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND, "页面不存在");
        }
        checkPageOwner(page);

        PageVersion version = pageVersionMapper.selectById(versionId);
        if (version == null || !version.getPageId().equals(pageId)) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "版本不存在");
        }

        page.setSchemaContent(version.getSchemaSnapshot());
        page.setSchemaVersion(version.getVersionNo());
        updateById(page);

        return toVO(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchDeletePages(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "ID 列表不能为空");
        }
        if (ids.size() > 50) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "单次批量删除不能超过50条");
        }

        Long userId = SecurityUtils.getCurrentUserId();

        // 批量查询，避免 N+1
        List<Page> pages = listByIds(ids);
        Map<Long, Page> pageMap = new LinkedHashMap<>();
        for (Page p : pages) {
            pageMap.put(p.getId(), p);
        }

        int success = 0;
        int skipped = 0;
        List<Long> toDeleteIds = new java.util.ArrayList<>();
        for (Long id : ids) {
            Page page = pageMap.get(id);
            if (page == null) {
                skipped++;
                continue;
            }
            if (userId != null && !page.getUserId().equals(userId)) {
                skipped++;
                continue;
            }
            toDeleteIds.add(id);
            success++;
        }

        if (!toDeleteIds.isEmpty()) {
            removeByIds(toDeleteIds);
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("total", ids.size());
        result.put("success", success);
        result.put("skipped", skipped);
        return result;
    }

    @Override
    public PageResult<PageVO> listTrash(Long projectId, int page, int pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }

        var pageParam = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<Page>(page, pageSize);
        var result = baseMapper.selectTrashPage(pageParam, projectId, userId);

        var voList = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(voList, result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restorePage(Long pageId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }

        Page page = baseMapper.selectByIdIncludeDeleted(pageId);
        if (page == null) {
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND, "页面不存在");
        }
        if (!page.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此页面");
        }

        int rows = baseMapper.restoreById(pageId);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND, "页面不存在或未被删除");
        }
        log.info("恢复页面: pageId={}", pageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void permanentDeletePage(Long pageId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }

        Page page = baseMapper.selectByIdIncludeDeleted(pageId);
        if (page == null) {
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND, "页面不存在");
        }
        if (!page.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此页面");
        }

        int rows = baseMapper.permanentDeleteById(pageId);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND, "页面不存在");
        }
        log.info("永久删除页面: pageId={}", pageId);
    }

    private void createVersionSnapshot(Page page, int versionNo, Boolean isAutoSave) {
        Long userId = SecurityUtils.getCurrentUserId();
        PageVersion snapshot = new PageVersion();
        snapshot.setPageId(page.getId());
        snapshot.setVersionNo(versionNo);
        snapshot.setSchemaSnapshot(page.getSchemaContent());
        snapshot.setChangelog(Boolean.TRUE.equals(isAutoSave) ? "自动保存" : "手动保存");
        snapshot.setUserId(userId);
        snapshot.setCreatedTime(LocalDateTime.now());
        pageVersionMapper.insert(snapshot);
        log.info("创建页面版本快照: pageId={}, version={}", page.getId(), versionNo);

        // 版本数量上限检查 — 超出时删除最早的版本
        long count = pageVersionMapper.selectCount(
                new LambdaQueryWrapper<PageVersion>().eq(PageVersion::getPageId, page.getId()));
        if (count > maxVersionCount) {
            long excess = count - maxVersionCount;
            List<PageVersion> oldest = pageVersionMapper.selectList(
                    new LambdaQueryWrapper<PageVersion>()
                            .eq(PageVersion::getPageId, page.getId())
                            .orderByAsc(PageVersion::getVersionNo)
                            .last("LIMIT " + excess));
            for (PageVersion old : oldest) {
                pageVersionMapper.deleteById(old.getId());
            }
            log.info("清理过期版本: pageId={}, 删除{}个旧版本", page.getId(), oldest.size());
        }
    }

    /**
     * 校验当前用户是否为页面所有者
     */
    private void checkPageOwner(Page page) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }
        if (!page.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此页面");
        }
    }

    private PageVO toVO(Page page) {
        Map<String, Object> schema = null;
        long schemaSize = 0;
        int componentCount = 0;

        if (StringUtils.hasText(page.getSchemaContent())) {
            try {
                schema = objectMapper.readValue(page.getSchemaContent(),
                        new TypeReference<Map<String, Object>>() {});
                schemaSize = page.getSchemaContent().length();
                if (schema.containsKey("children")) {
                    Object children = schema.get("children");
                    if (children instanceof List<?> list) {
                        componentCount = list.size();
                    }
                }
            } catch (JsonProcessingException e) {
                log.warn("Page schema 解析失败: pageId={}", page.getId());
            }
        }

        return PageVO.builder()
                .pageId(page.getId())
                .projectId(page.getProjectId())
                .title(page.getName())
                .path(page.getPagePath())
                .isHome(page.getIsHome() != null && page.getIsHome() == StatusConst.YesNo.YES)
                .status(page.getStatus())
                .version(page.getSchemaVersion())
                .schema(schema)
                .schemaSize(schemaSize)
                .componentCount(componentCount)
                .createdAt(page.getCreatedTime())
                .updatedAt(page.getUpdatedTime())
                .build();
    }
}
