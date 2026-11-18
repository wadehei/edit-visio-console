package com.visualbuilder.publish.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.publish.dto.PublishRecordVO;
import com.visualbuilder.publish.dto.PublishResultVO;
import com.visualbuilder.publish.entity.PublishRecord;

public interface PublishService extends BaseService<PublishRecord> {
    PublishResultVO publish(Long projectId, Long pageId, String changelog);
    PageResult<PublishRecordVO> getPublishHistory(Long projectId, int page, int pageSize);
    PublishRecordVO getPublishStatus(Long recordId);
    /**
     * 发布成功收尾 — 在同一事务内将发布记录置为成功并同步页面状态为已发布，
     * 避免「记录成功但页面仍为编辑中」的跨事务不一致。
     */
    void completePublish(Long recordId, Long pageId, String accessUrl);

    void markFailed(Long recordId, String reason);
}
