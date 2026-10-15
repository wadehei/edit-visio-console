package com.visualbuilder.page.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.exception.BusinessException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageServiceImpl extends BaseServiceImpl<PageMapper, Page>
        implements PageService {

    private final PageVersionMapper pageVersionMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageVO createPage(Long projectId, PageCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(40001, "用户未登录");
        }

        Page page = new Page();
        page.setProjectId(projectId);
        page.setName(dto.getTitle());
        page.setPagePath(dto.getPath() != null ? dto.getPath() : "/page-" + System.currentTimeMillis());
        page.setPageType("h5");
        page.setSchemaContent(null);
        page.setSchemaVersion(0);
        page.setStatus(0);
        page.setIsHome(Boolean.TRUE.equals(dto.getIsHome()) ? 1 : 0);
        page.setUserId(userId);
        save(page);

        return toVO(page);
    }

    @Override
    public PageVO getPageDetail(Long pageId) {
        Page page = getById(pageId);
        if (page == null) {
            throw new BusinessException(40005, "页面不存在");
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
            throw new BusinessException(40005, "页面不存在");
        }
        if (StringUtils.hasText(dto.getTitle())) page.setName(dto.getTitle());
        if (dto.getPath() != null) page.setPagePath(dto.getPath());
        if (dto.getIsHome() != null) page.setIsHome(dto.getIsHome() ? 1 : 0);
        updateById(page);
        return toVO(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageVO saveSchema(Long pageId, PageSchemaDTO dto) {
        Page page = getById(pageId);
        if (page == null) {
            throw new BusinessException(40005, "页面不存在");
        }

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
            throw new BusinessException(40005, "页面不存在");
        }
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
            throw new BusinessException(40005, "页面不存在");
        }

        PageVersion version = pageVersionMapper.selectById(versionId);
        if (version == null || !version.getPageId().equals(pageId)) {
            throw new BusinessException(40005, "版本不存在");
        }

        page.setSchemaContent(version.getSchemaSnapshot());
        page.setSchemaVersion(version.getVersionNo());
        updateById(page);

        return toVO(page);
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
                .isHome(page.getIsHome() != null && page.getIsHome() == 1)
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
