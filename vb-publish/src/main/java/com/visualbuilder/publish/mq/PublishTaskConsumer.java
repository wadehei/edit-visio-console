package com.visualbuilder.publish.mq;

import com.rabbitmq.client.Channel;
import com.visualbuilder.publish.config.RabbitConfig;
import com.visualbuilder.publish.dto.PublishTaskMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 发布任务消费者 — 接收消息并调用 PublishExecutor 执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublishTaskConsumer {

    private final PublishExecutor publishExecutor;

    @RabbitListener(queues = RabbitConfig.PUBLISH_QUEUE)
    public void onMessage(PublishTaskMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("收到发布任务消息: recordId=" + message.getRecordId()
                    + ", pageId=" + message.getPageId());

            publishExecutor.execute(message);

            // 处理成功，手动 ACK
            channel.basicAck(deliveryTag, false);
            log.info("发布任务消息已确认: recordId=" + message.getRecordId());

        } catch (Exception e) {
            log.error("发布任务消费失败: recordId=" + message.getRecordId()
                    + ", error=" + e.getMessage(), e);
            try {
                // 处理失败，不重入队列（由 RabbitMQ retry 配置控制重试次数）
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ackEx) {
                log.error("消息确认失败: " + ackEx.getMessage(), ackEx);
            }
        }
    }
}
