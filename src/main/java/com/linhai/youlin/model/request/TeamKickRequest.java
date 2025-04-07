package com.linhai.youlin.model.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class TeamKickRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long teamId; // 队伍 ID
    private Long kickedUserId; // 被踢出的用户 ID
}