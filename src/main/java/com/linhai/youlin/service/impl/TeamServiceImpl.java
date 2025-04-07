package com.linhai.youlin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linhai.youlin.common.ErrorCode;
import com.linhai.youlin.exception.BusinessException;
import com.linhai.youlin.model.domain.User;
import com.linhai.youlin.model.domain.UserTeam;
import com.linhai.youlin.model.dto.TeamQuery;
import com.linhai.youlin.model.enums.TeamStatusEnum;
import com.linhai.youlin.model.request.TeamJoinRequest;
import com.linhai.youlin.model.request.TeamKickRequest;
import com.linhai.youlin.model.request.TeamQuitRequest;
import com.linhai.youlin.model.request.TeamUpdateRequest;
import com.linhai.youlin.model.vo.TeamMemberVO;
import com.linhai.youlin.model.vo.TeamUserVO;
import com.linhai.youlin.model.vo.UserVO;
import com.linhai.youlin.service.TeamService;
import com.linhai.youlin.model.domain.Team;
import com.linhai.youlin.mapper.TeamMapper;
import com.linhai.youlin.service.UserService;
import com.linhai.youlin.service.UserTeamService;
import com.linhai.youlin.service.ChatService; // 导入 ChatService
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 队伍服务实现类
 * @author <a href="https://github.com/linhai0872">林海
 */
//多次操作数据库的方法需要加上 事务注解 避免中断产生脏数据 要么都执行 要么都不执行（回滚）

@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    @Lazy
    private ChatService chatService;

    @Override
    @Transactional(rollbackFor = Exception.class)//事务注解：要么都成功 要么都不成功
    public long addTeam(Team team, User loginUser) {
        // 1. 请求参数是否为空？
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2. 是否登录，未登录不允许创建
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        final long userId = loginUser.getId();
        // 3. 校验信息
        //   1. 队伍人数 > 1 且 <= 20
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum < 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不满足要求");
        }
        //   2. 队伍标题 <= 20
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题不满足要求");
        }
        //   3. 描述 <= 512
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述过长");
        }
        //   4. status 是否公开（int）不传默认为 0（公开）
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不满足要求");
        }
        //   5. 如果 status 是加密状态，一定要有密码，且密码 <= 32
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码设置不正确");
            }
        }
        // 6. 超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if (new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间 > 当前时间");
        }
        // 7. 校验用户最多创建 5 个队伍
        // todo 有 bug，可能同时创建 100 个队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        long hasTeamNum = this.count(queryWrapper);
        if (hasTeamNum >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建 5 个队伍");
        }
        //事务：要么都成功 要么都不成功
        // 8. 插入队伍信息到队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if (!result || teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        // 9. 插入用户  => 队伍关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        return teamId;
    }

    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        // 组合查询条件
        if (teamQuery != null) {
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            List<Long> idList = teamQuery.getIdList();
            if (CollectionUtils.isNotEmpty(idList)) {
                queryWrapper.in("id", idList);
            }
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            Integer maxNum = teamQuery.getMaxNum();
            // 查询最大人数相等的
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }
            Long userId = teamQuery.getUserId();
            // 根据创建人来查询
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }
            // 根据状态来查询
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            if (statusEnum == null) {
                statusEnum = TeamStatusEnum.PUBLIC;
            }
            if (!isAdmin && statusEnum.equals(TeamStatusEnum.PRIVATE)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
            queryWrapper.eq("status", statusEnum.getValue());
        }
        // 不展示已过期的队伍
        // expireTime is null or expireTime > now()
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        List<Team> teamList = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        // 关联查询创建人的用户信息
        for (Team team : teamList) {
            Long userId = team.getUserId();
            if (userId == null) {
                continue;
            }
            User user = userService.getById(userId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            // 脱敏用户信息
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;
    }

    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = teamUpdateRequest.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team oldTeam = this.getById(id);
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        // 只有管理员或者队伍的创建者可以修改
        if (oldTeam.getUserId() != loginUser.getId() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        Integer newMaxNum = teamUpdateRequest.getMaxNum();
        if (newMaxNum != null && newMaxNum > 0) {
            if (newMaxNum < 1 || newMaxNum > 20) { // 与创建时保持一致或根据需要调整
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不满足要求");
            }
            // 检查更新后的 maxNum 是否小于当前已加入人数
            long currentMemberCount = this.countTeamUserByTeamId(id);
            if (newMaxNum < currentMemberCount) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "最大人数不能小于当前已有人数");
            }
        }
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus());
        if (statusEnum.equals(TeamStatusEnum.SECRET)) {
            if (StringUtils.isBlank(teamUpdateRequest.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密房间必须要设置密码");
            }
        }

        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, updateTeam);
        return this.updateById(updateTeam);
    }

    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamJoinRequest.getTeamId();
        Team team = getTeamById(teamId);
        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有队伍");
        }
        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }
        // 该用户已加入的队伍数量
        long userId = loginUser.getId();
        // 只有一个线程能获取到锁
        RLock lock = redissonClient.getLock("youlin:join_team");
        try {
            // 抢到锁并执行
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    System.out.println("getLock: " + Thread.currentThread().getId());
                    QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", userId);
                    long hasJoinNum = userTeamService.count(userTeamQueryWrapper);
                    if (hasJoinNum > 5) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建和加入 5 个队伍");
                    }
                    // 不能重复加入已加入的队伍
                    userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", userId);
                    userTeamQueryWrapper.eq("teamId", teamId);
                    long hasUserJoinTeam = userTeamService.count(userTeamQueryWrapper);
                    if (hasUserJoinTeam > 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入该队伍");
                    }
                    // 已加入队伍的人数
                    long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
                    if (teamHasJoinNum >= team.getMaxNum()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
                    }
                    // 修改队伍信息
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam);
                }
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error", e);
            return false;
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamQuitRequest.getTeamId();
        Team team = getTeamById(teamId);
        long userId = loginUser.getId();
        UserTeam queryUserTeam = new UserTeam();
        queryUserTeam.setTeamId(teamId);
        queryUserTeam.setUserId(userId);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>(queryUserTeam);
        long count = userTeamService.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未加入队伍");
        }
        long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
        // 队伍只剩一人，解散
        if (teamHasJoinNum == 1) {
            // 删除队伍
            this.removeById(teamId);
        } else {
            // 队伍还剩至少两人
            // 是队长
            if (team.getUserId() == userId) {
                // 把队伍转移给最早加入的用户
                // 1. 查询已加入队伍的所有用户和加入时间
                QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.eq("teamId", teamId);
                userTeamQueryWrapper.last("order by id asc limit 2");//只用按照加入时间查最早的2个
                List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
                if (CollectionUtils.isEmpty(userTeamList) || userTeamList.size() <= 1) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                UserTeam nextUserTeam = userTeamList.get(1);
                Long nextTeamLeaderId = nextUserTeam.getUserId();
                // 更新当前队伍的队长
                Team updateTeam = new Team();
                updateTeam.setId(teamId);
                updateTeam.setUserId(nextTeamLeaderId);
                boolean result = this.updateById(updateTeam);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍队长失败");
                }
            }
        }
        // 移除关系
        return userTeamService.remove(queryWrapper);
    }

    // 修改 deleteTeam 方法:
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(long id, User loginUser) {
        // 校验队伍是否存在
        Team team = getTeamById(id);
        long teamId = team.getId();
        // 校验你是不是队伍的队长
        if (!team.getUserId().equals(loginUser.getId())) { // 使用 equals 比较 Long
            throw new BusinessException(ErrorCode.NO_AUTH, "无访问权限");
        }
        // 1. 移除所有加入队伍的关联信息 (UserTeam)
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        boolean removeUserTeamResult = userTeamService.remove(userTeamQueryWrapper);
        if (!removeUserTeamResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍用户关系失败");
        }

        // 2. 删除队伍的聊天记录 (调用 ChatService)
        boolean removeChatResult = chatService.deleteMessagesByTeamId(teamId);
        if (!removeChatResult) {
            // 这里可以根据业务决定是否要抛异常回滚，如果聊天记录删除失败不影响核心队伍删除，可以只记录日志
            log.error("删除队伍 {} 的聊天记录失败");
            // throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍聊天记录失败"); // 如果需要强一致性
        }

        // 3. 删除队伍本身 (Team)
        boolean removeTeamResult = this.removeById(teamId);
        if (!removeTeamResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍失败");
        }

        return true; // 所有操作（或关键操作）成功
    }

    @Override
    public List<TeamMemberVO> listTeamMembers(Long teamId, User loginUser) {
        // 1. 校验队伍是否存在
        Team team = this.getTeamById(teamId); // 复用内部方法进行校验

        // 2. 校验权限：只有队长或已加入该队伍的成员才能查看
        long userId = loginUser.getId();
        boolean isCaptain = team.getUserId().equals(userId);
        QueryWrapper<UserTeam> userTeamCheckWrapper = new QueryWrapper<>();
        userTeamCheckWrapper.eq("teamId", teamId);
        userTeamCheckWrapper.eq("userId", userId);
        long userJoinCount = userTeamService.count(userTeamCheckWrapper);
        boolean isMember = userJoinCount > 0;

        // 如果既不是队长也不是成员 (并且不是管理员，如果需要管理员也能看的话)
        // if (!isCaptain && !isMember && !userService.isAdmin(loginUser)) {
        if (!isCaptain && !isMember) { // 简化：只有队长和成员能看
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限查看队伍成员");
        }

        // 3. 查询队伍的所有 UserTeam 关系
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
        if (CollectionUtils.isEmpty(userTeamList)) {
            return new ArrayList<>(); // 没有成员或队伍刚被解散的情况
        }

        // 4. 获取所有成员的 userId
        List<Long> memberUserIds = userTeamList.stream()
                .map(UserTeam::getUserId)
                .collect(Collectors.toList());

        // 5. 根据 userId 查询用户信息 (注意脱敏)
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", memberUserIds);
        // Select 只选择需要的字段，提高效率
        userQueryWrapper.select("id", "username", "userAccount", "avatarUrl");
        List<User> memberUsers = userService.list(userQueryWrapper);
        // 将用户信息转为 Map<userId, User> 以便快速查找
        Map<Long, User> userMap = memberUsers.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 6. 组装 TeamMemberVO 列表
        List<TeamMemberVO> teamMemberVOList = userTeamList.stream().map(userTeam -> {
            TeamMemberVO vo = new TeamMemberVO();
            Long memberUserId = userTeam.getUserId();
            User memberInfo = userMap.get(memberUserId);

            if (memberInfo != null) { // 确保用户信息查到了
                vo.setUserId(memberUserId);
                vo.setUsername(memberInfo.getUsername());
                vo.setUserAccount(memberInfo.getUserAccount());
                vo.setAvatarUrl(memberInfo.getAvatarUrl());
            }
            vo.setJoinTime(userTeam.getJoinTime());
            // 判断角色
            if (team.getUserId().equals(memberUserId)) {
                vo.setRole("队长");
            } else {
                vo.setRole("队员");
            }
            return vo;
        }).collect(Collectors.toList());

        teamMemberVOList.sort(Comparator.comparing(TeamMemberVO::getJoinTime));

        return teamMemberVOList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 涉及数据库修改，加事务
    public boolean kickMember(TeamKickRequest kickRequest, User loginUser) {
        Long teamId = kickRequest.getTeamId();
        Long kickedUserId = kickRequest.getKickedUserId();
        long operatorUserId = loginUser.getId();

        // 1. 校验队伍是否存在
        Team team = this.getTeamById(teamId);

        // 2. 校验操作者是否为队长
        if (!team.getUserId().equals(operatorUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "只有队长才能踢人");
        }

        // 3. 校验被踢者是否在队伍中
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        userTeamQueryWrapper.eq("userId", kickedUserId);
        long count = userTeamService.count(userTeamQueryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户不在队伍中");
        }

        // 4. 不能踢出队长自己 (虽然理论上队长不会踢自己，加个保险)
        if (kickedUserId.equals(operatorUserId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能踢出自己");
        }

        // 5. 执行踢人操作 (删除 UserTeam 记录)
        // 注意：这里直接用了之前的 QueryWrapper，因为它正好是定位到要删除的记录
        boolean removeResult = userTeamService.remove(userTeamQueryWrapper);
        if (!removeResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "踢出成员失败");
        }

        // 6. 可选：更新队伍的已加入人数信息（如果前端依赖这个实时性，或者有缓存需要更新）
        // 但通常在下次查询列表时会重新计算，所以这里可能不需要

        return true;
    }

    /**
     * 根据 id 获取队伍信息
     *
     * @param teamId
     * @return
     */
    private Team getTeamById(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        return team;
    }

    /**
     * 获取某队伍当前人数
     *
     * @param teamId
     * @return
     */
    private long countTeamUserByTeamId(long teamId) {
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        return userTeamService.count(userTeamQueryWrapper);
    }
}




