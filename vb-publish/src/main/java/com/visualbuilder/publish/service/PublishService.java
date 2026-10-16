package com.visualbuilder.publish.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.publish.dto.PublishResultVO;
import com.visualbuilder.publish.entity.PublishRecord;

public interface PublishService extends BaseService<PublishRecord> {
    PublishResultVO publish(Long projectId, Long pageId, String changelog);
    PageResult<PublishRecord> getPublishHistory(Long projectId, int page, int pageSize);
}
