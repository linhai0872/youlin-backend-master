package com.linhai.youlin.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class AiConversationSettingUpdateRequest implements Serializable {

    private static final long serialVersionUID = -5497825642274277932L;//序列化版本号

    private Long conversationId;

    private String modelId;

    private String prompt;
}