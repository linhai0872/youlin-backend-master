package com.linhai.youlin.model.dto;

import com.linhai.youlin.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


/**
 * 队伍查询封装类
 * @author <a href="https://github.com/linhai0872">林海
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class TeamQuery extends PageRequest {//继承分页方法
    /**
     * id
     */
    private Long id;

    /**
     * id 列表
     */
    private List<Long> idList;

    /**
     * 搜索关键词（同时对队伍名称和描述搜索）
     */
    private String searchText;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;
}
