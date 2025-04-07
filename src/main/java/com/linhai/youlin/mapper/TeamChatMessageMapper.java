package com.linhai.youlin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linhai.youlin.model.domain.TeamChatMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author linhai
 * @description 针对表【team_chat_message(队伍聊天消息表)】的数据库操作Mapper
 * @createDate 2024-07-28 10:00:00
 * @Entity com.linhai.youlin.model.domain.TeamChatMessage
 */
public interface TeamChatMessageMapper extends BaseMapper<TeamChatMessage> {

    /**
     * 获取队伍消息并关联发送者信息
     * @param teamId 队伍ID
     * @param limit 获取条数 (为了简单，可以先获取最近N条)
     * @return
     */
    List<TeamChatMessage> listMessagesWithUserInfo(@Param("teamId") Long teamId, @Param("limit") int limit);

}