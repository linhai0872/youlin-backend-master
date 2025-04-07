package com.linhai.youlin.controller;

import org.apache.commons.lang3.StringUtils;
import com.linhai.youlin.common.BaseResponse;
import com.linhai.youlin.common.ErrorCode;
import com.linhai.youlin.common.ResultUtils;
import com.linhai.youlin.exception.BusinessException;
import com.linhai.youlin.model.domain.TeamChatMessage;
import com.linhai.youlin.model.domain.User;
import com.linhai.youlin.model.request.ChatMessageSendRequest; // 需要创建这个请求类
import com.linhai.youlin.service.ChatService;
import com.linhai.youlin.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/chat") // 基础路径 /api/chat
@Slf4j
// 如果需要跨域，可以在类或方法上添加 @CrossOrigin 注解，但通常在 WebMvcConfig 中全局配置
public class ChatController {

    @Resource
    private ChatService chatService;

    @Resource
    private UserService userService;

    /**
     * 发送队伍消息
     * @param teamId 队伍ID (路径参数)
     * @param sendRequest 包含消息内容的请求体
     * @param request HttpServletRequest 用于获取当前用户
     * @return
     */
    @PostMapping("/team/{teamId}/message")
    public BaseResponse<Boolean> sendMessage(
            @PathVariable Long teamId,
            @RequestBody ChatMessageSendRequest sendRequest, // 创建这个类
            HttpServletRequest request) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (sendRequest == null || StringUtils.isBlank(sendRequest.getContent())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        }

        User loginUser = userService.getLoginUser(request);
        boolean success = chatService.sendMessage(teamId, sendRequest.getContent(), loginUser);
        return ResultUtils.success(success);
    }

    /**
     * 获取队伍消息列表
     * @param teamId 队伍ID (路径参数)
     * @param request HttpServletRequest 用于获取当前用户
     * @return
     */
    @GetMapping("/team/{teamId}/messages")
    public BaseResponse<List<TeamChatMessage>> listMessages(
            @PathVariable Long teamId,
            HttpServletRequest request) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        List<TeamChatMessage> messages = chatService.listMessages(teamId, loginUser);
        // 注意：这里返回的 TeamChatMessage 包含了 username 和 avatarUrl (通过Mapper查询得到)
        return ResultUtils.success(messages);
    }
}