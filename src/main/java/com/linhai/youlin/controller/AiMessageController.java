package com.linhai.youlin.controller;

import com.linhai.youlin.common.BaseResponse;
import com.linhai.youlin.common.ErrorCode;
import com.linhai.youlin.common.ResultUtils;
import com.linhai.youlin.exception.BusinessException;
import com.linhai.youlin.model.domain.AiMessage;
import com.linhai.youlin.model.domain.User;
import com.linhai.youlin.model.request.AiMessageSendRequest;
import com.linhai.youlin.model.vo.AiConversationVO;
import com.linhai.youlin.service.AiMessageService;
import com.linhai.youlin.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/ai_conversation")
@Slf4j
public class AiMessageController {

    @Resource
    private AiMessageService aiMessageService;

    @Resource
    private UserService userService;

    /**
     * 初始化 AI 会话
     * @param request HttpServletRequest 对象，用于获取当前登录用户
     * @return 包含会话 ID 的 BaseResponse
     */
    @PostMapping("/create")
    public BaseResponse<Long> initAiConversation(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long conversationId = aiMessageService.initAiConversation(loginUser);
        return ResultUtils.success(conversationId);
    }

    /**
     * 获取用户的 AI 会话列表
     * @param request HttpServletRequest 对象，用于获取当前登录用户
     * @return 包含 AI 会话列表的 BaseResponse
     */
    @GetMapping("/list")
    public BaseResponse<List<AiConversationVO>> listAiConversations(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<AiConversationVO> conversations = aiMessageService.listAiConversations(loginUser);
        return ResultUtils.success(conversations);
    }

    /**
     * 获取 AI 会话的消息
     * @param conversationId 会话 ID
     * @param request        HttpServletRequest 对象，用于获取当前登录用户
     * @return 包含 AI 会话消息的 BaseResponse
     */
    @GetMapping("/{conversationId}/message/get")
    public BaseResponse<AiConversationVO> getAiConversationMessages(@PathVariable Long conversationId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        AiConversationVO messages = aiMessageService.getAiConversationMessages(conversationId, loginUser);
        return ResultUtils.success(messages);
    }

    /**
     * 发送 AI 消息
     *
     * @param conversationId 会话 ID
     * @param sendRequest    发送消息的请求体
     * @param request        HttpServletRequest 对象，用于获取当前登录用户
     * @return 包含已发送消息的 BaseResponse
     */
    @PostMapping("/{conversationId}/message/send")
    public BaseResponse<AiMessage> sendAiMessage(@PathVariable Long conversationId, @RequestBody AiMessageSendRequest sendRequest, HttpServletRequest request) {
        // 校验conversationId
        if (conversationId == null || conversationId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        sendRequest.setConversationId(conversationId); // 这行很重要，确保请求体中的 conversationId 和路径中的一致
        AiMessage message = aiMessageService.sendAiMessage(sendRequest, loginUser);
        return ResultUtils.success(message);
    }

    /**
     * 退出 AI 会话
     *
     * @param conversationId 会话 ID
     * @param request        HttpServletRequest 对象，用于获取当前登录用户
     * @return 空的 BaseResponse
     */
    @PostMapping("/{conversationId}/quit")
    public BaseResponse<Void> quitAiConversation(@PathVariable Long conversationId, HttpServletRequest request) {
        // 校验conversationId
        if (conversationId == null || conversationId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        aiMessageService.quitAiConversation(conversationId, loginUser);
        return ResultUtils.success(null);
    }

    /**
     * 清除 AI 会话消息 (软删除)
     *
     * @param conversationId 会话 ID
     * @param request        HttpServletRequest 对象，用于获取当前登录用户
     * @return 空的 BaseResponse
     */
    @PostMapping("/{conversationId}/message/delete")
    public BaseResponse<Void> clearAiConversationMessages(@PathVariable Long conversationId, HttpServletRequest request) {
        // 校验conversationId
        if (conversationId == null || conversationId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        aiMessageService.clearAiConversationMessages(conversationId, loginUser);
        return ResultUtils.success(null);
    }

    /**
     * 清除 AI 会话消息 (硬删除)
     *
     * @param conversationId 会话 ID
     * @param request        HttpServletRequest 对象，用于获取当前登录用户
     * @return 空的 BaseResponse
     */
    @PostMapping("/{conversationId}/message/deleteall")
    public BaseResponse<Void> deleteAllAiConversationMessages(@PathVariable Long conversationId, HttpServletRequest request) {
        // 校验conversationId
        if (conversationId == null || conversationId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        aiMessageService.deleteAllAiConversationMessages(conversationId, loginUser);
        return ResultUtils.success(null);
    }


    /**
     * 设置 AI 会话参数
     *
     * @param conversationId 会话 ID
     * @param prompt         提示词
     * @param request        HttpServletRequest 对象，用于获取当前登录用户
     * @return 空的 BaseResponse
     */
    @PostMapping("/{conversationId}/setting")
    public BaseResponse<Void> setAiConversationSettings(@PathVariable Long conversationId, @RequestParam String prompt, HttpServletRequest request) {
        // 校验conversationId
        if (conversationId == null || conversationId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        aiMessageService.setAiConversationSettings(conversationId, loginUser, prompt);
        return ResultUtils.success(null);
    }
}