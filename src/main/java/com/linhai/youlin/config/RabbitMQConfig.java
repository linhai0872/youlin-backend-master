package com.linhai.youlin.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMQConfig {

    // 定义交换机名称
    public static final String AI_MESSAGE_EXCHANGE = "ai_message_exchange";
    // 定义队列名称
    public static final String AI_MESSAGE_QUEUE = "ai_message_queue";
    // 定义路由键
    public static final String AI_MESSAGE_ROUTING_KEY = "ai_message_routing_key";

    // 创建队列
    @Bean
    public Queue aiMessageQueue() {
        // 使用 Queue 的构造函数，并指定队列名称和持久化选项
        return new Queue(AI_MESSAGE_QUEUE, true); // 第二个参数 true 表示持久化队列
    }

    // 创建交换机（Direct类型）
    @Bean
    public DirectExchange aiMessageExchange() {
        return new DirectExchange(AI_MESSAGE_EXCHANGE);
    }

    // 将队列绑定到交换机，并指定路由键
    @Bean
    public Binding aiMessageBinding(Queue aiMessageQueue, DirectExchange aiMessageExchange) {
        return BindingBuilder.bind(aiMessageQueue).to(aiMessageExchange).with(AI_MESSAGE_ROUTING_KEY);
    }
}