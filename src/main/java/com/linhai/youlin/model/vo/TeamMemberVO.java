package com.linhai.youlin.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 队伍成员视图对象 (VO)
 * 用于在前端展示队伍成员列表时，封装成员的必要信息。
 */

@Data // Lombok 注解，自动生成 getter, setter, toString, equals, hashCode 方法
public class TeamMemberVO implements Serializable {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 成员用户 ID
     */
    private Long userId;

    /**
     * 成员用户昵称
     */
    private String username;

    /**
     * 成员用户账号
     */
    private String userAccount;

    /**
     * 成员用户头像 URL
     */
    private String avatarUrl;

    /**
     * 成员加入队伍的时间
     */
    private Date joinTime;

    /**
     * 成员在队伍中的角色 ("队长" 或 "队员")
     */
    private String role;

}