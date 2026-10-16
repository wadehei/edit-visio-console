package com.visualbuilder.publish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.constant.StatusConst;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.common.security.utils.SecurityUtils;
import com.visualbuilder.publish.dto.PublishResultVO;
import com.visualbuilder.publish.entity.PublishRecord;
import com.visualbuilder.publish.mapper.PublishRecordMapper;
import com.visualbuilder.publish.service.PublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishServiceImpl extends BaseServiceImpl<PublishRecordMapper, PublishRecord>
        implements PublishService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PublishResultVO publish(Long projectId, Long pageId, String changelog) {
        Long userId = SecurityUtils.getCurrentUserId();

        // 查询当前页面最大版本号并递增
        PublishRecord latest = this.getOne(new LambdaQueryWrapper<PublishRecord>()
                .eq(PublishRecord::getPageId, pageId)
                .orderByDesc(PublishRecord::getVersionNo)
                .last("LIMIT 1"));
        int nextVersion = (latest != null && latest.getVersionNo() != null) ? latest.getVersionNo() + 1 : 1;

        PublishRecord record = new PublishRecord();
        record.setPageId(pageId);
        record.setVersionNo(nextVersion);
        record.setStatus(StatusConst.Publish.SUCCESS);
        record.setChangelog(changelog);
        record.setPublishedBy(userId);
        record.setPublishedAt(LocalDateTime.now());
        record.setAccessUrl("/p/" + projectId + "/" + pageId);
        this.save(record);

        log.info("发布成功: projectId={}, pageId={}, version={}", projectId, pageId, nextVersion);

        return PublishResultVO.builder()
                .id(record.getId())
                .versionNo(nextVersion)
                .accessUrl(record.getAccessUrl())
                .publishedAt(record.getPublishedAt())
                .build();
    }

    @Override
    public PageResult<PublishRecord> getPublishHistory(Long projectId, int page, int pageSize) {
        Page<PublishRecord> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<PublishRecord> wrapper = new LambdaQueryWrapper<PublishRecord>()
                .like(PublishRecord::getAccessUrl, "/p/" + projectId + "/")
                .orderByDesc(PublishRecord::getPublishedAt);

        Page<PublishRecord> result = this.page(pageParam, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }
}
