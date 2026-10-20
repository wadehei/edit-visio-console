package com.visualbuilder.publish.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 — 声明发布任务队列、交换机和绑定。
 */
@Configuration
public class RabbitConfig {

    public static final String PUBLISH_QUEUE = "publish.task.queue";
    public static final String PUBLISH_EXCHANGE = "publish.task.exchange";
    public static final String PUBLISH_ROUTING_KEY = "publish.task";

    @Bean
    public Queue publishTaskQueue() {
        return QueueBuilder.durable(PUBLISH_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", PUBLISH_QUEUE + ".dlq")
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
    public Binding publishTaskDlqBinding(Queue publishTaskDlqQueue, DirectExchange publishTaskExchange) {
        return BindingBuilder.bind(publishTaskDlqQueue).to(publishTaskExchange).with(PUBLISH_QUEUE + ".dlq");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
