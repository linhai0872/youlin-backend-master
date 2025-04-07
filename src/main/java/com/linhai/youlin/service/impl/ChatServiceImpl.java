package com.linhai.youlin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linhai.youlin.common.ErrorCode;
import com.linhai.youlin.exception.BusinessException;
import com.linhai.youlin.mapper.TeamChatMessageMapper;
import com.linhai.youlin.model.domain.TeamChatMessage;
import com.linhai.youlin.model.domain.User;
import com.linhai.youlin.service.ChatService;
import com.linhai.youlin.service.TeamService;
import com.linhai.youlin.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

@Service
public class ChatServiceImpl extends ServiceImpl<TeamChatMessageMapper, TeamChatMessage> implements ChatService {

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private TeamService teamService; // 引入 TeamService 检查队伍是否存在

    @Resource
    private TeamChatMessageMapper teamChatMessageMapper;

    // 简单实现，每次最多获取的消息数量
    private static final int MESSAGE_FETCH_LIMIT = 100;

    @Override
    public boolean sendMessage(Long teamId, String content, User loginUser) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        if (StringUtils.isBlank(content)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        }
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        Long userId = loginUser.getId();

        // 1. 校验队伍是否存在
        if (teamService.getById(teamId) == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }

        // 2. 校验用户是否在该队伍中 (复用 UserTeamService 的逻辑)
        boolean isUserInTeam = userTeamService.isUserInTeam(userId, teamId);
        if (!isUserInTeam) {
            throw new BusinessException(ErrorCode.NO_AUTH, "您不在该队伍中，无法发送消息");
        }

        // 3. 创建并保存消息
        TeamChatMessage chatMessage = new TeamChatMessage();
        chatMessage.setTeamId(teamId);
        chatMessage.setUserId(userId);
        chatMessage.setContent(content);
        // createTime 会由 MyBatis Plus 自动填充
        return this.save(chatMessage);
    }

    @Override
    public List<TeamChatMessage> listMessages(Long teamId, User loginUser) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long userId = loginUser.getId();

        // 1. 校验队伍是否存在 (可选，如果 sendMessage 保证了队伍存在性，这里可以省略)
        if (teamService.getById(teamId) == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }

        // 2. 校验用户是否在该队伍中
        boolean isUserInTeam = userTeamService.isUserInTeam(userId, teamId);
        if (!isUserInTeam) {
            // 可以选择抛异常或返回空列表，这里返回空列表可能更友好
            // throw new BusinessException(ErrorCode.NO_AUTH, "您不在该队伍中，无法查看消息");
            return Collections.emptyList();
        }

        // 3. 查询消息并关联用户信息 (使用自定义Mapper方法)
        // 注意：listMessagesWithUserInfo 是按时间倒序查的，需要反转列表
        List<TeamChatMessage> messages = teamChatMessageMapper.listMessagesWithUserInfo(teamId, MESSAGE_FETCH_LIMIT);
        Collections.reverse(messages); // 反转列表，使其按时间升序排列

        return messages;
    }

    @Override
    public boolean deleteMessagesByTeamId(Long teamId) {
        if (teamId == null || teamId <= 0) {
            // 可以选择记录日志或静默处理，因为这是内部调用
            log.warn("尝试删除无效队伍ID的聊天记录: {}");
            return false;
        }
        QueryWrapper<TeamChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        // 执行物理删除
        return this.remove(queryWrapper);
    }
}