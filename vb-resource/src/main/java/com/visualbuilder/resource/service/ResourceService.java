package com.visualbuilder.resource.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.resource.entity.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

public interface ResourceService extends BaseService<Resource> {
    Map<String, Object> upload(MultipartFile file, String folderId);
    void deleteResource(Long resourceId);
}
