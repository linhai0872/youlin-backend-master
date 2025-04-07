package com.linhai.youlin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linhai.youlin.model.domain.TeamChatMessage;
import com.linhai.youlin.model.domain.User;

import java.util.List;

/**
 * 队伍聊天服务接口
 */
public interface ChatService extends IService<TeamChatMessage> {

    /**
     * 发送消息
     * @param teamId 队伍ID
     * @param content 消息内容
     * @param loginUser 当前登录用户
     * @return 是否发送成功
     */
    boolean sendMessage(Long teamId, String content, User loginUser);

    /**
     * 获取队伍聊天记录 (包含发送者信息)
     * @param teamId 队伍ID
     * @param loginUser 当前登录用户 (用于鉴权)
     * @return 消息列表 (按时间升序)
     */
    List<TeamChatMessage> listMessages(Long teamId, User loginUser);

    /**
     * 删除指定队伍的所有聊天记录 (物理删除)
     * @param teamId 队伍ID
     * @return 是否删除成功
     */
    boolean deleteMessagesByTeamId(Long teamId);
}