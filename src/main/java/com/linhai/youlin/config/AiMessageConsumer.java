package com.linhai.youlin.config;

import com.linhai.youlin.model.domain.AiMessage;
import com.linhai.youlin.service.AiMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class AiMessageConsumer {

    @Resource
    private AiMessageService aiMessageService;

    @RabbitListener(queues = RabbitMQConfig.AI_MESSAGE_QUEUE)
    public void receiveAiMessage(AiMessage aiMessage) {
        try {
            // 持久化 AI 消息到数据库
            boolean saveResult = aiMessageService.save(aiMessage);
            if (!saveResult) {
                log.error("Failed to save AI message to database: {}", aiMessage);
            }
        } catch (Exception e) {
            log.error("Error while processing AI message from queue", e);
            // 处理异常，例如重试、记录日志、发送告警等
        }
    }
}