package com.linhai.youlin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linhai.youlin.model.domain.UserTeam;

import java.util.List; // 导入 List

/**
 * @author 林海
 */

public interface UserTeamService extends IService<UserTeam> {

    /**
     * 检查用户是否在指定队伍中 (未删除的关系)
     * @param userId 用户ID
     * @param teamId 队伍ID
     * @return true 如果用户在队伍中, false 否则
     */
    boolean isUserInTeam(Long userId, Long teamId); // 新增方法声明

    // --- 新增方法声明 (如果 ChatServiceImpl 需要) ---
    /**
     * 获取用户加入的所有队伍 ID 列表 (未删除的关系)
     * @param userId 用户 ID
     * @return 队伍 ID 列表
     */
    List<Long> listUserJoinedTeamIds(Long userId); // 新增方法声明
}