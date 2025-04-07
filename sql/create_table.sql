# 数据库初始化
create database if not exists userMatching_update;

use userMatching_update;
--
-- 用户表
CREATE TABLE user (
    userName      VARCHAR(256)                       NULL     COMMENT '用户昵称',
    id            BIGINT AUTO_INCREMENT              NOT NULL COMMENT 'id'
      PRIMARY KEY,
    userAccount   VARCHAR(256)                       NULL     COMMENT '账号',
    avatarUrl     VARCHAR(1024)                      NULL     COMMENT '用户头像',
    gender        TINYINT                            NULL     COMMENT '性别',
    userPassword  VARCHAR(512)                       NOT NULL COMMENT '密码',
    phone         VARCHAR(128)                       NULL     COMMENT '电话',
    email         VARCHAR(512)                       NULL     COMMENT '邮箱',
    bio           VARCHAR(1024)                      NULL     COMMENT '* 用户简介',
    userStatus    INT          DEFAULT 0             NOT NULL COMMENT '可拓展字段 比如用户状态 0 - 可接受消息 1 - 勿扰',
    createTime    DATETIME     DEFAULT CURRENT_TIMESTAMP NULL     COMMENT '创建时间',
    updateTime    DATETIME     DEFAULT CURRENT_TIMESTAMP NULL     ON UPDATE CURRENT_TIMESTAMP,
    lastLoginTime DATETIME                           NULL     COMMENT '最后登录时间',
    isDelete      TINYINT      DEFAULT 0             NOT NULL COMMENT '是否删除',
    userRole      INT          DEFAULT 0             NOT NULL COMMENT '用户角色 0 - 普通用户 1 - 管理员',
    planetCode    VARCHAR(512)                       NULL     COMMENT '平台编号',
    tags          VARCHAR(1024)                      NULL     COMMENT '标签 json 列表'
) COMMENT '用户';

-- 用户表索引
ALTER TABLE `user` ADD UNIQUE INDEX `uk_userAccount` (`userAccount`); -- 用户账户唯一索引
ALTER TABLE `user` ADD UNIQUE INDEX `uk_planetCode` (`planetCode`);   -- 平台编号唯一索引

ALTER TABLE user CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 队伍表
CREATE TABLE team (
    id          BIGINT AUTO_INCREMENT              NOT NULL COMMENT 'id'
      PRIMARY KEY,
    name        VARCHAR(256)                       NOT NULL COMMENT '队伍名称',
    description VARCHAR(1024)                      NULL     COMMENT '描述',
    maxNum      INT          DEFAULT 1             NOT NULL COMMENT '最大人数',
    expireTime  DATETIME                           NULL     COMMENT '过期时间',
    userId      BIGINT                             NULL     COMMENT '用户id（即队长/创建者 id）',
    status      INT          DEFAULT 0             NOT NULL COMMENT '0 - 公开，1 - 私有，2 - 加密',
    password    VARCHAR(512)                       NULL     COMMENT '密码',
    createTime  DATETIME     DEFAULT CURRENT_TIMESTAMP NULL     COMMENT '创建时间',
    updateTime  DATETIME     DEFAULT CURRENT_TIMESTAMP NULL     ON UPDATE CURRENT_TIMESTAMP,
    isDelete    TINYINT      DEFAULT 0             NOT NULL COMMENT '是否删除'
) COMMENT '队伍';

-- 队伍表索引
ALTER TABLE `team` ADD INDEX `idx_userId` (`userId`);             -- 查找队伍队长索引
ALTER TABLE `team` ADD INDEX `idx_expireTime` (`expireTime`); -- 队伍过期时间索引，拓展定期检查清理已过期队伍功能

ALTER TABLE team CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 用户队伍关系表
CREATE TABLE user_team (
    id         BIGINT AUTO_INCREMENT              NOT NULL COMMENT 'id'
      PRIMARY KEY,
    userId     BIGINT                             NULL     COMMENT '用户id',
    teamId     BIGINT                             NULL     COMMENT '队伍id',
    joinTime   DATETIME                           NULL     COMMENT '加入时间',
    createTime DATETIME     DEFAULT CURRENT_TIMESTAMP NULL     COMMENT '创建时间',
    updateTime DATETIME     DEFAULT CURRENT_TIMESTAMP NULL     ON UPDATE CURRENT_TIMESTAMP,
    isDelete   TINYINT      DEFAULT 0             NOT NULL COMMENT '是否删除',
    UNIQUE KEY `uk_userId_teamId` (`userId`, `teamId`) -- 唯一复合索引 同一个用户不能重复加入同一个队伍
) COMMENT '用户队伍关系';

-- 用户队伍关系表索引 (已存在主键索引)
ALTER TABLE `user_team` ADD INDEX `idx_userId` (`userId`); -- 用户id索引 获取某个用户加入的所有队伍
ALTER TABLE `user_team` ADD INDEX `idx_teamId` (`teamId`); -- 队伍id索引 获取某个队伍的所有成员

-- *AI 会话消息表
CREATE TABLE ai_message (
    id              bigint auto_increment comment '主键——消息ID'
       primary key,
    conversationId bigint    not null comment '会话ID', -- 雪花算法生成 一个用户可以有一个/多个AI会话
    userId         bigint    not null comment '用户ID',
    senderType     tinyint   not null comment '发送方类型：0-用户, 1-AI 2-默认消息',
    content         text      not null comment '消息内容', -- 初始化会话插入特殊欢迎语
    modelId        varchar(128) null comment '模型ID',
    prompt          varchar(1024) null comment 'Prompt内容',
    createTime      datetime  not null default CURRENT_TIMESTAMP comment '创建时间',
    updateTime      datetime           default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete        tinyint   not null default 0 comment '是否删除',

    FOREIGN KEY (userId) REFERENCES user (id) -- 外键——创建ai会话消息时用户id必须存在 关联用户表的用户id
) comment 'AI 消息表';

-- 添加索引
ALTER TABLE `ai_message` ADD INDEX `idx_user_conversation_time` (`userId`, `conversationId`, `createTime`); -- 常用查询场景
ALTER TABLE `ai_message` ADD INDEX `idx_conversationId` (`conversationId`);
ALTER TABLE `ai_message` ADD INDEX `idx_userId` (`userId`);
ALTER TABLE `ai_message` ADD INDEX `idx_createTime` (`createTime`);

ALTER TABLE ai_message CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


-- 队伍聊天消息表
CREATE TABLE team_chat_message (
                                   id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
                                   teamId      BIGINT NOT NULL COMMENT '队伍ID',
                                   userId      BIGINT NOT NULL COMMENT '发送用户ID',
                                   content     TEXT   NOT NULL COMMENT '消息内容',
                                   createTime  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '发送时间',
    -- isDelete    TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除 (暂时不用)', -- 为了最简化，先不用软删

    -- 外键约束 (可选，但推荐)
                                   FOREIGN KEY (teamId) REFERENCES team(id) ON DELETE CASCADE, -- 队伍删除时，聊天记录也级联删除 (如果DB层面支持且需要)
                                   FOREIGN KEY (userId) REFERENCES user(id)


) COMMENT '队伍聊天消息表';

ALTER TABLE `team_chat_message` ADD INDEX `idx_teamId_createTime` (`teamId`, `createTime`); -- 查询某队伍消息按时间排序

ALTER TABLE team_chat_message CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;