package com.visualbuilder.publish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.constant.RedisKeyConst;
import com.visualbuilder.common.core.constant.StatusConst;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.common.security.utils.SecurityUtils;
import com.visualbuilder.common.util.PageUtils;
import com.visualbuilder.page.service.PageService;
import com.visualbuilder.project.service.ProjectMemberService;
import com.visualbuilder.publish.dto.PublishResultVO;
import com.visualbuilder.publish.dto.PublishTaskMessage;
import com.visualbuilder.publish.entity.PublishRecord;
import com.visualbuilder.publish.mapper.PublishRecordMapper;
import com.visualbuilder.publish.mq.PublishTaskProducer;
import com.visualbuilder.publish.service.PublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishServiceImpl extends BaseServiceImpl<PublishRecordMapper, PublishRecord>
        implements PublishService {

    private final PageService pageService;
    private final ProjectMemberService projectMemberService;
    private final PublishTaskProducer publishTaskProducer;
    private final StringRedisTemplate redisTemplate;

    /**
     * 校验当前用户是否有权访问该项目（所有者或项目成员）；未登录一律拒绝。
     */
    private void checkProjectAccess(Long projectId) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        if (!projectMemberService.isProjectOwnerOrMember(projectId, currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该项目");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PublishResultVO publish(Long projectId, Long pageId, String changelog) {
        // 1. 校验页面存在且属于该项目
        var page = pageService.getById(pageId);
        if (page == null || !page.getProjectId().equals(projectId)) {
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND, "页面不存在或不属于当前项目");
        }
        if (!StringUtils.hasText(page.getSchemaContent())) {
            throw new BusinessException(ErrorCode.PUBLISH_FAILED, "页面内容为空，无法发布");
        }

        // 权限校验：仅项目所有者或成员可发布（未登录一律拒绝）
        checkProjectAccess(projectId);
        Long userId = SecurityUtils.getCurrentUserId();

        // 2. 检查是否已有进行中的发布任务（防并发，使用 Redis 分布式锁）
        String lockKey = RedisKeyConst.publishLock(pageId);
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofMinutes(5));
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException(ErrorCode.PUBLISH_FAILED, "当前有正在发布中的任务，请稍后再试");
        }

        try {
            return doPublish(projectId, pageId, changelog, userId);
        } finally {
            // 必须在事务提交/回滚之后再释放锁，否则会出现"锁已释放但数据未提交"的并发漏洞
            releaseLockAfterTransaction(lockKey, lockValue);
        }
    }

    /**
     * 在当前事务完成后（提交或回滚）释放锁，并校验锁归属，避免误删他人持有的锁。
     * 若不在事务上下文中，则立即释放。
     */
    private void releaseLockAfterTransaction(String lockKey, String lockValue) {
        Runnable release = () -> {
            String current = redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(current)) {
                redisTemplate.delete(lockKey);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    release.run();
                }
            });
        } else {
            release.run();
        }
    }

    private PublishResultVO doPublish(Long projectId, Long pageId, String changelog, Long userId) {
        // 二次检查 PENDING 状态（锁内校验）
        long pendingCount = this.count(new LambdaQueryWrapper<PublishRecord>()
                .eq(PublishRecord::getPageId, pageId)
                .eq(PublishRecord::getStatus, StatusConst.Publish.PENDING));
        if (pendingCount > 0) {
            throw new BusinessException(ErrorCode.PUBLISH_FAILED, "当前有正在发布中的任务，请稍后再试");
        }

        // 3. 计算下一个版本号
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
        // 未登录一律拒绝；仅项目所有者或成员可查看发布历史
        checkProjectAccess(projectId);

        int pn = PageUtils.normalizePage(page);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<PublishRecord> pageParam = new Page<>(pn, ps);
        LambdaQueryWrapper<PublishRecord> wrapper = new LambdaQueryWrapper<PublishRecord>()
                .eq(PublishRecord::getProjectId, projectId)
                .orderByDesc(PublishRecord::getPublishedAt);

        Page<PublishRecord> result = this.page(pageParam, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), pn, ps);
    }

    @Override
    public PublishRecord getPublishStatus(Long recordId) {
        PublishRecord record = this.getById(recordId);
        if (record == null) {
            return null;
        }
        // 未登录一律拒绝；仅项目所有者或成员可查看发布状态
        checkProjectAccess(record.getProjectId());
        return record;
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
