package com.visualbuilder.page.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.page.dto.PageCreateDTO;
import com.visualbuilder.page.dto.PageSchemaDTO;
import com.visualbuilder.page.dto.PageVO;
import com.visualbuilder.page.entity.Page;
import com.visualbuilder.page.entity.PageVersion;

import java.util.List;
import java.util.Map;

public interface PageService extends BaseService<Page> {

    PageVO createPage(Long projectId, PageCreateDTO dto);

    PageVO getPageDetail(Long pageId);

    PageResult<PageVO> listPages(Long projectId, int page, int pageSize, String keyword);

    PageVO updatePageInfo(Long pageId, PageCreateDTO dto);

    PageVO saveSchema(Long pageId, PageSchemaDTO dto);

    void deletePage(Long pageId);

    List<PageVersion> getVersionList(Long pageId);

    PageVO rollbackVersion(Long pageId, Long versionId);

    Map<String, Object> batchDeletePages(List<Long> ids);

    PageResult<PageVO> listTrash(Long projectId, int page, int pageSize);

    void restorePage(Long pageId);

    void permanentDeletePage(Long pageId);
}
