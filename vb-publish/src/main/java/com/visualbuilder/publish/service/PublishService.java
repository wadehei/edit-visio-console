package com.visualbuilder.publish.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.publish.entity.PublishRecord;
import java.util.List;
import java.util.Map;

public interface PublishService extends BaseService<PublishRecord> {
    Map<String, Object> publish(Long projectId, Long pageId, String changelog);
    List<PublishRecord> getPublishHistory(Long projectId);
}
