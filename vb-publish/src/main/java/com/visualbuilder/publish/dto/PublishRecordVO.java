package com.visualbuilder.publish.dto;

import com.visualbuilder.publish.entity.PublishRecord;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发布记录对外 VO — 与持久化实体 {@link PublishRecord} 解耦，
 * 避免实体字段（含内部列）随表结构变更直接泄漏到 API 响应契约中。
 */
@Data
public class PublishRecordVO {

    private Long id;
    private Long projectId;
    private Long pageId;
    private Integer versionNo;
    private Integer status;
    private String accessUrl;
    private String changelog;
    private String failedReason;
    private Long publishedBy;
    private LocalDateTime publishedAt;
    private LocalDateTime createdTime;

    public static PublishRecordVO from(PublishRecord record) {
        if (record == null) {
            return null;
        }
        PublishRecordVO vo = new PublishRecordVO();
        vo.setId(record.getId());
        vo.setProjectId(record.getProjectId());
        vo.setPageId(record.getPageId());
        vo.setVersionNo(record.getVersionNo());
        vo.setStatus(record.getStatus());
        vo.setAccessUrl(record.getAccessUrl());
        vo.setChangelog(record.getChangelog());
        vo.setFailedReason(record.getFailedReason());
        vo.setPublishedBy(record.getPublishedBy());
        vo.setPublishedAt(record.getPublishedAt());
        vo.setCreatedTime(record.getCreatedTime());
        return vo;
    }
}
