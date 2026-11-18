package com.visualbuilder.publish.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 — 声明发布任务队列、重试队列、死信队列与绑定。
 *
 * <p>重试采用「延迟队列」而非阻塞消费线程重试：失败消息被投递到带 TTL 的重试队列，
 * 到期后经死信路由回到主队列重新消费。这样消费线程不会被 sleep 阻塞，
 * 单个慢任务不会拖垮整条队列的吞吐。</p>
 */
@Configuration
public class RabbitConfig {

    public static final String PUBLISH_QUEUE = "publish.task.queue";
    public static final String PUBLISH_RETRY_QUEUE = "publish.task.retry.queue";
    public static final String PUBLISH_EXCHANGE = "publish.task.exchange";
    public static final String PUBLISH_ROUTING_KEY = "publish.task";

    private final PublishProperties publishProperties;

    public RabbitConfig(PublishProperties publishProperties) {
        this.publishProperties = publishProperties;
    }

    @Bean
    public Queue publishTaskQueue() {
        return QueueBuilder.durable(PUBLISH_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", PUBLISH_QUEUE + ".dlq")
                .build();
    }

    /**
     * 重试队列 — 无消费者，仅作为「延迟等待区」：
     * 消息在此停留 {@code business.publish.retry-interval-ms} 后，经死信路由回到主队列。
     */
    @Bean
    public Queue publishTaskRetryQueue() {
        return QueueBuilder.durable(PUBLISH_RETRY_QUEUE)
                .withArgument("x-message-ttl", publishProperties.getRetryIntervalMs())
                .withArgument("x-dead-letter-exchange", PUBLISH_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", PUBLISH_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue publishTaskDlqQueue() {
        return QueueBuilder.durable(PUBLISH_QUEUE + ".dlq").build();
    }

    @Bean
    public DirectExchange publishTaskExchange() {
        return new DirectExchange(PUBLISH_EXCHANGE);
    }

    @Bean
    public Binding publishTaskBinding(Queue publishTaskQueue, DirectExchange publishTaskExchange) {
        return BindingBuilder.bind(publishTaskQueue).to(publishTaskExchange).with(PUBLISH_ROUTING_KEY);
    }

    @Bean
    public Binding publishTaskRetryBinding(Queue publishTaskRetryQueue, DirectExchange publishTaskExchange) {
        return BindingBuilder.bind(publishTaskRetryQueue).to(publishTaskExchange).with(PUBLISH_RETRY_QUEUE);
    }

    @Bean
    public Binding publishTaskDlqBinding(Queue publishTaskDlqQueue, DirectExchange publishTaskExchange) {
        return BindingBuilder.bind(publishTaskDlqQueue).to(publishTaskExchange).with(PUBLISH_QUEUE + ".dlq");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
