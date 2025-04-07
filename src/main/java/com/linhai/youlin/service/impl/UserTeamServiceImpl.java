package com.linhai.youlin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linhai.youlin.model.domain.UserTeam;
import com.linhai.youlin.service.UserTeamService;
import com.linhai.youlin.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List; // 导入 List
import java.util.stream.Collectors; // 导入 Collectors

/**
 * @author lzd
 * @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
 * @createDate 2024-03-07 16:13:48
 */
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
        implements UserTeamService{

    @Override
    public boolean isUserInTeam(Long userId, Long teamId) {
        if (userId == null || teamId == null) {
            return false;
        }
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("teamId", teamId);
        // isDelete = 0 的条件 Mybatis Plus 逻辑删除会自动处理 (如果配置了)
        // 如果没有配置全局逻辑删除，需要手动加上 .eq("isDelete", 0);
        return this.count(queryWrapper) > 0;
    }

    @Override
    public List<Long> listUserJoinedTeamIds(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.select("teamId"); // 只查询 teamId 字段
        // isDelete = 0 的条件
        List<UserTeam> userTeamList = this.list(queryWrapper);
        return userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toList());
    }
}