package com.linhai.youlin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linhai.youlin.model.domain.AiMessage;
import com.linhai.youlin.model.domain.User;
import com.linhai.youlin.model.request.AiMessageSendRequest;
import com.linhai.youlin.model.vo.AiConversationVO;

import java.util.List;

public interface AiMessageService extends IService<AiMessage> {

    /**
     * 初始化 AI 会话
     * @param loginUser 登录用户
     * @return 会话 ID
     */
    Long initAiConversation(User loginUser);

    /**
     * 获取用户的 AI 会话列表
     * @param loginUser 登录用户
     * @return AI 会话列表
     */
    List<AiConversationVO> listAiConversations(User loginUser);

    /**
     * 获取 AI 会话的消息
     * @param conversationId 会话 ID
     * @param loginUser      登录用户
     * @return AI 会话 VO
     */
    AiConversationVO getAiConversationMessages(Long conversationId, User loginUser);

    /**
     * 发送 AI 消息
     *
     * @param sendRequest
     * @param loginUser   登录用户
     * @return
     */
    AiMessage sendAiMessage(AiMessageSendRequest sendRequest, User loginUser);

    /**
     * 退出 AI 会话
     *
     * @param conversationId 会话 ID
     * @param loginUser      登录用户
     */
    void quitAiConversation(Long conversationId, User loginUser);

    /**
     * 清除 AI 会话消息
     *
     * @param conversationId 会话 ID
     * @param loginUser      登录用户
     */
    void clearAiConversationMessages(Long conversationId, User loginUser);

//    /**
//     * 导出AI会话消息(JSON)
//     *
//     * @param conversationId 会话 ID
//     * @param loginUser      登录用户
//     */
//    String exportAiConversationMessages(Long conversationId, User loginUser);

    void deleteAllAiConversationMessages(Long conversationId, User loginUser);

    /**
     * 设置 AI 会话参数
     *
     * @param conversationId 会话 ID
     * @param loginUser      登录用户
     * @param prompt         prompt 内容
     */
    void setAiConversationSettings(Long conversationId, User loginUser, String prompt);

    /**
     * 清理超时的 AI 会话消息
     */
    void cleanUpExpiredAiMessages();
}