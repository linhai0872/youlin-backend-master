package com.linhai.youlin.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 队伍聊天消息表
 * @TableName team_chat_message
 */

@TableName(value ="team_chat_message")
@Data
public class TeamChatMessage implements Serializable {
    /**
     * 消息ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 队伍ID
     */
    private Long teamId;

    /**
     * 发送用户ID
     */
    private Long userId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 发送时间
     */
    @TableField(fill = FieldFill.INSERT) // MyBatis Plus 自动填充创建时间
    private Date createTime;

    // 暂时不需要 isDelete 字段

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    // --- 以下为关联查询需要的用户信息 (VO中使用) ---
    /**
     * 发送者昵称 (用于VO)
     */
    @TableField(exist = false)
    private String username;

    /**
     * 发送者头像 (用于VO)
     */
    @TableField(exist = false)
    private String avatarUrl;
}