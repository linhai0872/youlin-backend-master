package com.linhai.youlin.model.vo;

import com.linhai.youlin.model.domain.AiMessage;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class AiConversationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话 ID
     */
    private Long conversationId;

    /**
     * 最后一条消息的时间
     */
    private Date lastMessageTime;

    /**
     *  最后一条消息
     */
    private  String lastMessage;

    /**
     * 消息列表
     */
    private List<AiMessage> messages;
}