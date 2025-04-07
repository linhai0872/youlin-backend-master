package com.linhai.youlin.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

import java.util.Date;

/**
 * AI 消息表
 * @TableName ai_message
 */
@TableName(value ="ai_message")
@Data
public class AiMessage implements Serializable {

    private static final long serialVersionUID = 1L; // 建议添加 serialVersionUID

    /**
     * 主键——消息ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 发送方类型：0-用户, 1-AI 2-默认消息
     */
    private Integer senderType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 模型ID
     */
    private String modelId;

    /**
     * Prompt内容
     */
    private String prompt;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;
}