package com.linhai.youlin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linhai.youlin.model.domain.Team;
import com.linhai.youlin.model.domain.User;
import com.linhai.youlin.model.dto.TeamQuery;
import com.linhai.youlin.model.request.TeamJoinRequest;
import com.linhai.youlin.model.request.TeamKickRequest;
import com.linhai.youlin.model.request.TeamQuitRequest;
import com.linhai.youlin.model.request.TeamUpdateRequest;
import com.linhai.youlin.model.vo.TeamMemberVO;
import com.linhai.youlin.model.vo.TeamUserVO;

import java.util.List;

/**
 * 队伍服务
 *
 * @author <a href="https://github.com/linhai0872">林海

 */
public interface TeamService extends IService<Team> {

    /**
     * 创建队伍
     *
     * @param team
     * @param loginUser
     * @return
     */
    long addTeam(Team team, User loginUser);

    /**
     * 搜索队伍
     *
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 更新队伍
     *
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 加入队伍
     *
     * @param teamJoinRequest
     * @return
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    /**
     * 退出队伍
     *
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    /**
     * 删除（解散）队伍
     *
     * @param id
     * @param loginUser
     * @return
     */
    boolean deleteTeam(long id, User loginUser);

    /**
     * 获取队伍成员列表
     * @param teamId 队伍 ID
     * @param loginUser 当前登录用户
     * @return 成员列表
     */
    List<TeamMemberVO> listTeamMembers(Long teamId, User loginUser);

    /**
     * 队长踢出成员
     * @param kickRequest 踢人请求
     * @param loginUser 操作用户 (必须是队长)
     * @return 是否成功
     */
    boolean kickMember(TeamKickRequest kickRequest, User loginUser);
}
