package com.visualbuilder.publish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.security.utils.SecurityUtils;
import com.visualbuilder.publish.entity.PublishRecord;
import com.visualbuilder.publish.mapper.PublishRecordMapper;
import com.visualbuilder.publish.service.PublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishServiceImpl extends BaseServiceImpl<PublishRecordMapper, PublishRecord>
        implements PublishService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> publish(Long projectId, Long pageId, String changelog) {
        Long userId = SecurityUtils.getCurrentUserId();
        String versionNo = "v" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        PublishRecord record = new PublishRecord();
        record.setPageId(pageId);
        record.setVersionNo(versionNo);
        record.setStatus(1);
        record.setChangelog(changelog);
        record.setPublishedBy(userId);
        record.setPublishedAt(LocalDateTime.now());
        record.setAccessUrl("/p/" + projectId + "/" + pageId);
        this.save(record);

        log.info("发布成功: projectId={}, pageId={}, version={}", projectId, pageId, versionNo);

        Map<String, Object> result = new HashMap<>();
        result.put("id", record.getId());
        result.put("versionNo", versionNo);
        result.put("accessUrl", record.getAccessUrl());
        result.put("publishedAt", record.getPublishedAt());
        return result;
    }

    @Override
    public List<PublishRecord> getPublishHistory(Long projectId) {
        return this.list(new LambdaQueryWrapper<PublishRecord>()
                .eq(PublishRecord::getPageId, projectId)
                .orderByDesc(PublishRecord::getPublishedAt));
    }
}
