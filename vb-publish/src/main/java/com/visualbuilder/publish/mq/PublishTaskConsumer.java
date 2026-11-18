package com.visualbuilder.publish.mq;

import com.rabbitmq.client.Channel;
import com.visualbuilder.publish.config.PublishProperties;
import com.visualbuilder.publish.config.RabbitConfig;
import com.visualbuilder.publish.dto.PublishTaskMessage;
import com.visualbuilder.publish.service.PublishService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * RabbitMQ 发布任务消费者 — 接收消息并调用 PublishExecutor 执行。
 *
 * <p>失败采用<b>非阻塞重试</b>：失败消息被投递到带 TTL 的重试队列，
 * 延迟 {@code business.publish.retry-interval-ms} 后经死信路由自动回到主队列重新消费，
 * 最多重试 {@code business.publish.max-retry} 次。消费线程不会被 sleep 阻塞，
 * 单个慢任务不会拖垮整条队列的吞吐。</p>
 *
 * <p>重试耗尽后置发布记录为失败，并 {@code basicNack(requeue=false)} 投递至死信队列，
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
    private final RabbitTemplate rabbitTemplate;
    private final MeterRegistry meterRegistry;

    @RabbitListener(queues = RabbitConfig.PUBLISH_QUEUE)
    public void onMessage(PublishTaskMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("收到发布任务消息: recordId={}, pageId={}", message.getRecordId(), message.getPageId());

        try {
            publishExecutor.execute(message);
        } catch (Exception e) {
            int attempt = message.getRetryCount() + 1;
            if (attempt <= publishProperties.getMaxRetry() && scheduleRetry(message, attempt, e)) {
                // 已投递到延迟重试队列，确认原消息
                channel.basicAck(deliveryTag, false);
                return;
            }
            // 重试耗尽或重试投递失败：标记失败并拒绝消息（路由至死信队列）
            log.error("发布任务重试耗尽，标记失败并进入死信队列: recordId={}, attempts={}, error={}",
                    message.getRecordId(), attempt, e.getMessage(), e);
            publishService.markFailed(message.getRecordId(), truncate(e.getMessage()));
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        // 处理成功，手动 ACK
        channel.basicAck(deliveryTag, false);
        log.info("发布任务消息已确认: recordId={}", message.getRecordId());
    }

    /**
     * 将失败消息投递到延迟重试队列；投递成功返回 {@code true}。
     * 投递失败（如 broker 不可用）返回 {@code false}，由调用方转入死信处理。
     */
    private boolean scheduleRetry(PublishTaskMessage message, int attempt, Exception cause) {
        message.setRetryCount(attempt);
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.PUBLISH_EXCHANGE,
                    RabbitConfig.PUBLISH_RETRY_QUEUE, message);
            log.warn("发布任务执行失败，{}ms 后第 {}/{} 次重试: recordId={}, error={}",
                    publishProperties.getRetryIntervalMs(), attempt, publishProperties.getMaxRetry(),
                    message.getRecordId(), cause.getMessage());
            return true;
        } catch (Exception sendError) {
            log.error("投递重试队列失败: recordId={}, error={}",
                    message.getRecordId(), sendError.getMessage(), sendError);
            return false;
        }
    }

    /**
     * 死信队列消费者 — 重试耗尽的发布任务最终落在此处。
     * 这里只做错误级别记录与指标埋点，作为告警钩子与人工排查入口（消息已被 {@code markFailed} 标记）。
     */
    @RabbitListener(queues = RabbitConfig.PUBLISH_QUEUE + ".dlq")
    public void onDeadLetter(PublishTaskMessage message, Channel channel,
                             @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.error("[发布死信] 发布任务重试耗尽: recordId={}, pageId={}, version={}",
                message.getRecordId(), message.getPageId(), message.getVersionNo());
        meterRegistry.counter("vb.publish.dlq").increment();
        channel.basicAck(deliveryTag, false);
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > FAILED_REASON_MAX_LENGTH ? s.substring(0, FAILED_REASON_MAX_LENGTH) : s;
    }
}
