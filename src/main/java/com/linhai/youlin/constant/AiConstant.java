package com.linhai.youlin.constant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConstant {
    /**
     * 默认 AI 模型 ID, 可以在配置文件中配置
     */
    @Value("${ai.default-model-id}")
    public static String DEFAULT_MODEL_ID = "ep-20250312200345-xp8h9";

    /**
     * 默认欢迎消息
     */
    @Value("${ai.default-welcome-message}")
    public static String DEFAULT_WELCOME_MESSAGE = "你好，这里是有林AI助手，请问有什么可以帮助您的？";

    /**
     * AI 消息在 Redis 中的 key 前缀
     */
    @Value("${ai.message-cache-key-prefix}")
    public static String AI_MESSAGE_REDIS_KEY_PREFIX = "ai:message:";

    /**
     * AI 会话在 Redis 中的 key 前缀
     */
    @Value("${ai.conversation-cache-key-prefix}")
    public static String AI_CONVERSATION_REDIS_KEY_PREFIX = "ai:conversation:";

    /**
     * Redis中AI会话最后一条消息时间 的key后缀
     */
    public static final String AI_LAST_MESSAGE_TIME = "last_message_time";

    /**
     * 发送方类型：用户
     */
    public static final Integer SENDER_TYPE_USER = 0;

    /**
     * 发送方类型：AI
     */
    public static final Integer SENDER_TYPE_AI = 1;
    /**
     * 发送方类型：默认消息
     */
    public static final Integer SENDER_TYPE_DEFAULT = 2;
}