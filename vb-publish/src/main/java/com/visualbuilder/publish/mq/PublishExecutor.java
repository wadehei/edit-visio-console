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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    /**
     * 执行发布任务
     */
    public void execute(PublishTaskMessage msg) {
        Long recordId = msg.getRecordId();
        Long pageId = msg.getPageId();
        Long projectId = msg.getProjectId();
        Integer versionNo = msg.getVersionNo();

        log.info("开始执行发布任务: recordId=" + recordId + ", pageId=" + pageId);

        try {
            // 1. 幂等性检查：如果记录已非 PENDING 状态，跳过
            var record = publishService.getById(recordId);
            if (record == null) {
                log.warn("发布记录不存在: recordId=" + recordId);
                return;
            }
            if (record.getStatus() != StatusConst.Publish.PENDING) {
                log.info("发布记录已处理，跳过: recordId=" + recordId + ", status=" + record.getStatus());
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
            String html = pageRenderer.render(schemaContent);
            log.info("HTML 渲染完成: recordId=" + recordId + ", size=" + html.length() + " bytes");

            // 4. 上传到存储
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String storagePath = publishProperties.getStoragePrefix()
                    + projectId + "/" + pageId + "/v" + versionNo + "/" + datePath + "/index.html";

            ByteArrayInputStream htmlStream = new ByteArrayInputStream(
                    html.getBytes(StandardCharsets.UTF_8));
            String accessUrl = storageService.upload(htmlStream, storagePath, "text/html; charset=utf-8");

            log.info("HTML 上传完成: recordId=" + recordId + ", url=" + accessUrl);

            // 5. 更新发布记录状态为 SUCCESS
            publishService.markSuccess(recordId, accessUrl);

            // 6. 更新页面状态为已发布
            page.setStatus(StatusConst.Page.PUBLISHED);
            pageService.updateById(page);

            log.info("发布任务完成: recordId=" + recordId + ", pageId=" + pageId);

        } catch (Exception e) {
            log.error("发布任务失败: recordId=" + recordId + ", error=" + e.getMessage(), e);
            publishService.markFailed(recordId, truncate(e.getMessage(), 900));
            throw new RuntimeException("发布任务执行失败", e);
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }
}
