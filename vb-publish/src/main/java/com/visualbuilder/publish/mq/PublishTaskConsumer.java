package com.visualbuilder.publish.mq;

import com.rabbitmq.client.Channel;
import com.visualbuilder.publish.config.PublishProperties;
import com.visualbuilder.publish.config.RabbitConfig;
import com.visualbuilder.publish.dto.PublishTaskMessage;
import com.visualbuilder.publish.service.PublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * RabbitMQ 发布任务消费者 — 接收消息并调用 PublishExecutor 执行。
 *
 * <p>失败采用<b>有界重试</b>：首次执行失败后按 {@code business.publish.retry-interval-ms}
 * 间隔最多重试 {@code business.publish.max-retry} 次（阻塞当前消费线程，发布为低频操作）。
 * 重试耗尽后置发布记录为失败，并 {@code basicNack(requeue=false)} 投递至死信队列，
 * 由 {@link #onDeadLetter} 记录，供人工排查或后续补偿。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublishTaskConsumer {

    private static final int FAILED_REASON_MAX_LENGTH = 900;

    private final PublishExecutor publishExecutor;
    private final PublishService publishService;
    private final PublishProperties publishProperties;

    @RabbitListener(queues = RabbitConfig.PUBLISH_QUEUE)
    public void onMessage(PublishTaskMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("收到发布任务消息: recordId=" + message.getRecordId()
                + ", pageId=" + message.getPageId());

        int attempt = 0;
        int maxRetry = publishProperties.getMaxRetry();
        while (true) {
            try {
                publishExecutor.execute(message);
            } catch (Exception e) {
                attempt++;
                if (attempt > maxRetry) {
                    log.error("发布任务重试耗尽，标记失败并进入死信队列: recordId=" + message.getRecordId()
                            + ", attempts=" + attempt + ", error=" + e.getMessage(), e);
                    publishService.markFailed(message.getRecordId(), truncate(e.getMessage()));
                    channel.basicNack(deliveryTag, false, false);
                    return;
                }
                log.warn("发布任务执行失败，准备第 {}/{} 次重试: recordId=" + message.getRecordId()
                        + ", error=" + e.getMessage(), attempt, maxRetry);
                sleepQuietly(publishProperties.getRetryIntervalMs());
                continue;
            }
            // 处理成功，手动 ACK
            channel.basicAck(deliveryTag, false);
            log.info("发布任务消息已确认: recordId=" + message.getRecordId());
            return;
        }
    }

    /**
     * 死信队列消费者 — 重试耗尽的发布任务最终落在此处。
     * 这里只做错误级别记录，作为告警钩子与人工排查入口（消息已被 {@code markFailed} 标记）。
     */
    @RabbitListener(queues = RabbitConfig.PUBLISH_QUEUE + ".dlq")
    public void onDeadLetter(PublishTaskMessage message, Channel channel,
                             @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.error("[发布死信] 发布任务重试耗尽: recordId=" + message.getRecordId()
                + ", pageId=" + message.getPageId() + ", version=" + message.getVersionNo());
        channel.basicAck(deliveryTag, false);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > FAILED_REASON_MAX_LENGTH ? s.substring(0, FAILED_REASON_MAX_LENGTH) : s;
    }
}
