package com.visualbuilder.resource.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.resource.dto.UploadResultVO;
import com.visualbuilder.resource.entity.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ResourceService extends BaseService<Resource> {
    UploadResultVO upload(MultipartFile file, String folderId);
    void deleteResource(Long resourceId);
    Map<String, Object> batchDelete(List<Long> ids);
    PageResult<Resource> listResources(String resourceType, String groupName, int page, int pageSize);

    PageResult<Resource> listTrash(int page, int pageSize);

    void restoreResource(Long resourceId);

    void permanentDeleteResource(Long resourceId);
}
