package com.visualbuilder.publish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.constant.StatusConst;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.common.security.utils.SecurityUtils;
import com.visualbuilder.page.service.PageService;
import com.visualbuilder.publish.dto.PublishResultVO;
import com.visualbuilder.publish.dto.PublishTaskMessage;
import com.visualbuilder.publish.entity.PublishRecord;
import com.visualbuilder.publish.mapper.PublishRecordMapper;
import com.visualbuilder.publish.mq.PublishTaskProducer;
import com.visualbuilder.publish.service.PublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishServiceImpl extends BaseServiceImpl<PublishRecordMapper, PublishRecord>
        implements PublishService {

    private final PageService pageService;
    private final PublishTaskProducer publishTaskProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PublishResultVO publish(Long projectId, Long pageId, String changelog) {
        // 1. 校验页面存在且属于该项目
        com.visualbuilder.page.entity.Page page = pageService.getById(pageId);
        if (page == null || !page.getProjectId().equals(projectId)) {
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND, "页面不存在或不属于当前项目");
        }
        if (!StringUtils.hasText(page.getSchemaContent())) {
            throw new BusinessException(ErrorCode.PUBLISH_FAILED, "页面内容为空，无法发布");
        }

        // 2. 检查是否已有进行中的发布任务（防并发）
        long pendingCount = this.count(new LambdaQueryWrapper<PublishRecord>()
                .eq(PublishRecord::getPageId, pageId)
                .eq(PublishRecord::getStatus, StatusConst.Publish.PENDING));
        if (pendingCount > 0) {
            throw new BusinessException(ErrorCode.PUBLISH_FAILED, "当前有正在发布中的任务，请稍后再试");
        }

        // 3. 计算下一个版本号
        Long userId = SecurityUtils.getCurrentUserId();
        PublishRecord latest = this.getOne(new LambdaQueryWrapper<PublishRecord>()
                .eq(PublishRecord::getPageId, pageId)
                .orderByDesc(PublishRecord::getVersionNo)
                .last("LIMIT 1"));
        int nextVersion = (latest != null && latest.getVersionNo() != null) ? latest.getVersionNo() + 1 : 1;

        // 4. 创建 PENDING 状态的发布记录
        PublishRecord record = new PublishRecord();
        record.setProjectId(projectId);
        record.setPageId(pageId);
        record.setVersionNo(nextVersion);
        record.setStatus(StatusConst.Publish.PENDING);
        record.setChangelog(changelog);
        record.setPublishedBy(userId);
        this.save(record);

        // 5. 发送异步任务消息
        PublishTaskMessage msg = new PublishTaskMessage(
                record.getId(), projectId, pageId, nextVersion);
        publishTaskProducer.send(msg);

        log.info("发布任务已创建: recordId=" + record.getId()
                + ", projectId=" + projectId + ", pageId=" + pageId + ", version=" + nextVersion);

        return PublishResultVO.builder()
                .id(record.getId())
                .versionNo(nextVersion)
                .status(StatusConst.Publish.PENDING)
                .build();
    }

    @Override
    public PageResult<PublishRecord> getPublishHistory(Long projectId, int page, int pageSize) {
        Page<PublishRecord> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<PublishRecord> wrapper = new LambdaQueryWrapper<PublishRecord>()
                .eq(PublishRecord::getProjectId, projectId)
                .orderByDesc(PublishRecord::getPublishedAt);

        Page<PublishRecord> result = this.page(pageParam, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    public PublishRecord getPublishStatus(Long recordId) {
        return this.getById(recordId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSuccess(Long recordId, String accessUrl) {
        PublishRecord record = this.getById(recordId);
        if (record == null) return;
        record.setStatus(StatusConst.Publish.SUCCESS);
        record.setAccessUrl(accessUrl);
        record.setPublishedAt(LocalDateTime.now());
        this.updateById(record);
        log.info("发布成功: recordId=" + recordId + ", accessUrl=" + accessUrl);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markFailed(Long recordId, String reason) {
        PublishRecord record = this.getById(recordId);
        if (record == null) return;
        record.setStatus(StatusConst.Publish.FAILED);
        record.setFailedReason(reason);
        record.setPublishedAt(LocalDateTime.now());
        this.updateById(record);
        log.info("发布失败: recordId=" + recordId + ", reason=" + reason);
    }
}
