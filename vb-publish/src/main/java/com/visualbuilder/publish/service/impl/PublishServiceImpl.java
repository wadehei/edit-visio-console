package com.visualbuilder.publish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visualbuilder.common.core.base.BaseServiceImpl;
import com.visualbuilder.common.core.constant.LimitsConst;
import com.visualbuilder.common.core.constant.RedisKeyConst;
import com.visualbuilder.common.core.constant.StatusConst;
import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.core.result.PageResult;
import com.visualbuilder.common.security.utils.SecurityUtils;
import com.visualbuilder.common.util.PageUtils;
import com.visualbuilder.common.util.TransactionUtils;
import com.visualbuilder.page.service.PageService;
import com.visualbuilder.common.core.constant.ProjectRole;
import com.visualbuilder.project.service.ProjectMemberService;
import com.visualbuilder.publish.dto.PublishRecordVO;
import com.visualbuilder.publish.dto.PublishResultVO;
import com.visualbuilder.publish.dto.PublishTaskMessage;
import com.visualbuilder.publish.entity.PublishRecord;
import com.visualbuilder.publish.mapper.PublishRecordMapper;
import com.visualbuilder.publish.mq.PublishTaskProducer;
import com.visualbuilder.publish.service.PublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishServiceImpl extends BaseServiceImpl<PublishRecordMapper, PublishRecord>
        implements PublishService {

    /** PENDING 记录超过该分钟数视为陈旧（发送丢失/消费崩溃），不再阻塞新发布。 */
    private static final long PENDING_STALE_MINUTES = 10;

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

        // 权限校验：发布要求编辑权限（页面创建者或项目 EDIT 角色），与页面写权限模型一致——
        // 只读（VIEW）协作者不得把页面内容发布到公网
        Long userId = SecurityUtils.requireCurrentUserId();
        if (!page.getUserId().equals(userId)
                && !projectMemberService.hasProjectRole(projectId, userId, ProjectRole.EDIT)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权发布此页面，需要编辑权限");
        }

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
     *
     * <p>释放采用 Lua 脚本「比对 + 删除」原子完成，替代 GET + compare + DEL 三步，
     * 消除「比对通过后锁过期、再 DEL 误删他人新锁」的竞态。</p>
     */
    private void releaseLockAfterTransaction(String lockKey, String lockValue) {
        Runnable release = () -> redisTemplate.execute(
                RELEASE_LOCK_SCRIPT, java.util.Collections.singletonList(lockKey), lockValue);
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

    /** 仅当锁值为自己持有时才删除，保证「校验 + 删除」在 Redis 端原子执行。 */
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private PublishResultVO doPublish(Long projectId, Long pageId, String changelog, Long userId) {
        // 二次检查 PENDING 状态（锁内校验）。
        // 只统计「未超时」的 PENDING:afterCommit 发送失败或应用崩溃会留下无法推进的
        // 陈旧 PENDING(消费者幂等条件 status=PENDING 保证旧消息不会误推进),若不加时间
        // 维度,该页面将被永久卡死无法再次发布。
        long pendingCount = this.count(new LambdaQueryWrapper<PublishRecord>()
                .eq(PublishRecord::getPageId, pageId)
                .eq(PublishRecord::getStatus, StatusConst.Publish.PENDING)
                .gt(PublishRecord::getCreatedTime,
                        LocalDateTime.now().minusMinutes(PENDING_STALE_MINUTES)));
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
        record.setChangelog(truncateChangelog(changelog));
        record.setPublishedBy(userId);
        try {
            this.save(record);
        } catch (DuplicateKeyException e) {
            // (page_id, version_no) 唯一约束兜底：极端并发下版本号被抢占
            throw new BusinessException(ErrorCode.PUBLISH_FAILED, "版本号冲突，请稍后重试");
        }

        // 5. 发送异步任务消息 — 必须等事务提交后再发送，否则事务回滚会出现"消息已发、记录不存在"
        PublishTaskMessage msg = new PublishTaskMessage(
                record.getId(), projectId, pageId, nextVersion, 0);
        TransactionUtils.executeAfterCommit(() -> {
            try {
                publishTaskProducer.send(msg);
            } catch (Exception e) {
                // 记录已在事务中提交,发送失败无法回滚——立即把 PENDING 置为 FAILED,
                // 否则这条记录会一直占用「进行中发布」名额,页面从此无法再发布
                log.error("发布任务消息发送失败,标记记录为 FAILED: recordId={}", record.getId(), e);
                this.lambdaUpdate()
                        .eq(PublishRecord::getId, record.getId())
                        .eq(PublishRecord::getStatus, StatusConst.Publish.PENDING)
                        .set(PublishRecord::getStatus, StatusConst.Publish.FAILED)
                        .update();
            }
        });

        log.info("发布任务已创建: recordId={}, projectId={}, pageId={}, version={}",
                record.getId(), projectId, pageId, nextVersion);

        return PublishResultVO.builder()
                .id(record.getId())
                .versionNo(nextVersion)
                .status(StatusConst.Publish.PENDING)
                .build();
    }

    @Override
    public PageResult<PublishRecordVO> getPublishHistory(Long projectId, int page, int pageSize) {
        // 未登录一律拒绝；仅项目所有者或成员可查看发布历史
        checkProjectAccess(projectId);

        int pn = PageUtils.normalizePage(page);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<PublishRecord> pageParam = new Page<>(pn, ps);
        LambdaQueryWrapper<PublishRecord> wrapper = new LambdaQueryWrapper<PublishRecord>()
                .eq(PublishRecord::getProjectId, projectId)
                .orderByDesc(PublishRecord::getPublishedAt);

        Page<PublishRecord> result = this.page(pageParam, wrapper);
        List<PublishRecordVO> records = result.getRecords().stream()
                .map(PublishRecordVO::from)
                .toList();
        return PageResult.of(records, result.getTotal(), pn, ps);
    }

    @Override
    public PublishRecordVO getPublishStatus(Long recordId) {
        PublishRecord record = this.getById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "发布记录不存在");
        }
        // 未登录一律拒绝；仅项目所有者或成员可查看发布状态
        checkProjectAccess(record.getProjectId());
        return PublishRecordVO.from(record);
    }

    /**
     * 发布备注长度上限（字符），超出部分截断，避免超长文本写入数据库。
     * 与 Controller 上的 {@code @Size} 校验共用 {@link LimitsConst#MAX_TEXT_LENGTH}，避免口径漂移。
     */
    private static final int CHANGELOG_MAX_LENGTH = LimitsConst.MAX_TEXT_LENGTH;

    private static String truncateChangelog(String changelog) {
        if (changelog == null || changelog.length() <= CHANGELOG_MAX_LENGTH) {
            return changelog;
        }
        return changelog.substring(0, CHANGELOG_MAX_LENGTH);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completePublish(Long recordId, Long pageId, String accessUrl) {
        // 直接按 id 更新，避免与执行器中的 getById 重复查询同一条记录。
        // 附加 status=PENDING 条件：把幂等下沉为数据库原子条件更新，
        // 并发重复投递时只有一个消费者能把 PENDING 推进为 SUCCESS。
        boolean updated = this.lambdaUpdate()
                .eq(PublishRecord::getId, recordId)
                .eq(PublishRecord::getStatus, StatusConst.Publish.PENDING)
                .set(PublishRecord::getStatus, StatusConst.Publish.SUCCESS)
                .set(PublishRecord::getAccessUrl, accessUrl)
                .set(PublishRecord::getPublishedAt, LocalDateTime.now())
                .update();
        if (!updated) {
            log.info("发布记录非 PENDING（已处理或不存在），跳过发布成功标记: recordId={}", recordId);
            return;
        }

        // 与发布记录状态在同一事务内更新页面状态，避免「记录已成功、页面仍编辑中」的跨事务不一致
        pageService.lambdaUpdate()
                .eq(com.visualbuilder.page.entity.Page::getId, pageId)
                .set(com.visualbuilder.page.entity.Page::getStatus, StatusConst.Page.PUBLISHED)
                .update();
        log.info("发布成功: recordId={}, accessUrl={}", recordId, accessUrl);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markFailed(Long recordId, String reason) {
        // 直接按 id 更新，省去一次 getById 查询
        boolean updated = this.lambdaUpdate()
                .eq(PublishRecord::getId, recordId)
                .set(PublishRecord::getStatus, StatusConst.Publish.FAILED)
                .set(PublishRecord::getFailedReason, reason)
                .set(PublishRecord::getPublishedAt, LocalDateTime.now())
                .update();
        if (!updated) {
            log.info("发布记录不存在，跳过失败标记: recordId={}", recordId);
            return;
        }
        log.info("发布失败: recordId={}, reason={}", recordId, reason);
    }
}
