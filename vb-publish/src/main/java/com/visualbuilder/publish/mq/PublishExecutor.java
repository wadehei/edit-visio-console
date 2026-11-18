package com.visualbuilder.publish.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visualbuilder.common.core.constant.StatusConst;
import com.visualbuilder.common.storage.StorageService;
import com.visualbuilder.page.entity.Page;
import com.visualbuilder.page.service.PageService;
import com.visualbuilder.publish.config.PublishProperties;
import com.visualbuilder.publish.dto.PublishTaskMessage;
import com.visualbuilder.publish.render.PageRenderer;
import com.visualbuilder.publish.service.PublishService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * 发布任务执行器 — 核心工作：读取 Schema → 渲染 HTML → 上传存储 → 更新状态。
 *
 * <p>与 MQ 消费者解耦，便于单元测试。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublishExecutor {

    private final PageService pageService;
    private final PublishService publishService;
    private final PageRenderer pageRenderer;
    private final StorageService storageService;
    private final PublishProperties publishProperties;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    /**
     * 执行发布任务
     */
    public void execute(PublishTaskMessage msg) {
        Long recordId = msg.getRecordId();
        Long pageId = msg.getPageId();
        Long projectId = msg.getProjectId();
        Integer versionNo = msg.getVersionNo();

        log.info("开始执行发布任务: recordId={}, pageId={}", recordId, pageId);

        try {
            // 1. 幂等性检查：如果记录已非 PENDING 状态，跳过
            var record = publishService.getById(recordId);
            if (record == null) {
                log.warn("发布记录不存在: recordId={}", recordId);
                return;
            }
            if (record.getStatus() != StatusConst.Publish.PENDING) {
                log.info("发布记录已处理，跳过: recordId={}, status={}", recordId, record.getStatus());
                return;
            }

            // 2. 读取页面 Schema
            Page page = pageService.getById(pageId);
            if (page == null) {
                throw new RuntimeException("页面不存在: pageId=" + pageId);
            }
            String schemaContent = page.getSchemaContent();
            if (schemaContent == null || schemaContent.isBlank()) {
                throw new RuntimeException("页面内容为空: pageId=" + pageId);
            }

            // 3. 渲染 HTML
            String html;
            Timer.Sample renderSample = Timer.start(meterRegistry);
            try {
                html = pageRenderer.render(schemaContent);
            } finally {
                renderSample.stop(meterRegistry.timer("vb.publish.render"));
            }
            log.info("HTML 渲染完成: recordId={}, size={} bytes", recordId, html.length());

            // 4. 上传到存储（路径不含日期段：版本号已保证唯一，且便于旧版本产物按路径精确清理）
            String storagePath = buildArtifactPath(projectId, pageId, versionNo);
            ByteArrayInputStream htmlStream = new ByteArrayInputStream(
                    html.getBytes(StandardCharsets.UTF_8));
            String accessUrl = storageService.upload(htmlStream, storagePath, "text/html; charset=utf-8");

            log.info("HTML 上传完成: recordId={}, url={}", recordId, accessUrl);

            // 5+6. 同一事务内：发布记录置成功 + 页面状态置已发布
            publishService.completePublish(recordId, pageId, accessUrl);

            // 7. 清理已被挤出保留窗口的旧版本产物（失败不影响发布结果）
            deleteExpiredArtifact(projectId, pageId, versionNo);

            meterRegistry.counter("vb.publish.task", "result", "success").increment();
            log.info("发布任务完成: recordId={}, pageId={}", recordId, pageId);

        } catch (Exception e) {
            meterRegistry.counter("vb.publish.task", "result", "failure").increment();
            // 只记录并抛出：重试与最终失败标记由消费者（PublishTaskConsumer）统一决策，
            // 避免首次失败即置 FAILED 导致后续重试被幂等检查短路。
            log.error("发布任务执行失败: recordId={}, error={}", recordId, e.getMessage(), e);
            throw new RuntimeException("发布任务执行失败", e);
        }
    }

    /** 发布产物存储路径 — 不含日期段，保证同一版本路径可复现、可清理。 */
    private String buildArtifactPath(Long projectId, Long pageId, Integer versionNo) {
        return publishProperties.getStoragePrefix()
                + projectId + "/" + pageId + "/v" + versionNo + "/index.html";
    }

    /**
     * 删除刚被挤出保留窗口的版本产物（仅一个版本，避免每次发布全量扫描）。
     */
    private void deleteExpiredArtifact(Long projectId, Long pageId, Integer currentVersion) {
        if (currentVersion == null) return;
        int expiredVersion = currentVersion - publishProperties.getRetainArtifactVersions();
        if (expiredVersion < 1) return;

        try {
            storageService.delete(buildArtifactPath(projectId, pageId, expiredVersion));
            log.info("已清理过期发布产物: pageId={}, version={}", pageId, expiredVersion);
        } catch (Exception e) {
            // 发布已成功，清理属旁路动作，失败只记录
            log.warn("清理过期发布产物失败: pageId={}, version={}, error={}",
                    pageId, expiredVersion, e.getMessage());
        }
    }
}
