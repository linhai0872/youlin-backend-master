package com.linhai.youlin.model.request;

import lombok.Data;
import java.io.Serializable;

/**
 * 聊天消息发送请求
 */

@Data
public class ChatMessageSendRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 消息内容
     */
    private String content;
}