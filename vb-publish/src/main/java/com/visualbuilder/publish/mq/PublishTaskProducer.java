package com.visualbuilder.publish.mq;

import com.visualbuilder.publish.config.RabbitConfig;
import com.visualbuilder.publish.dto.PublishTaskMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 发布任务消息生产者 — 将发布任务发送到 RabbitMQ 队列。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublishTaskProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送发布任务消息
     */
    public void send(PublishTaskMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.PUBLISH_EXCHANGE,
                    RabbitConfig.PUBLISH_ROUTING_KEY,
                    message);
            log.info("发布任务已发送: recordId=" + message.getRecordId()
                    + ", pageId=" + message.getPageId());
        } catch (Exception e) {
            log.error("发布任务发送失败: recordId=" + message.getRecordId()
                    + ", error=" + e.getMessage(), e);
            throw new RuntimeException("发布任务发送失败，请稍后重试", e);
        }
    }
}
