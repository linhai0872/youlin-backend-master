package com.linhai.youlin.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class AiMessageSendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话 ID
     */
    private Long conversationId;

    /**
     * 消息内容
     */
    private String content;

    // 可以根据需要添加其他字段，如模型 ID、prompt 等
    // private String modelId;
    // private String prompt;
}