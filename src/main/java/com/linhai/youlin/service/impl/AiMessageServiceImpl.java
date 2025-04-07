package com.linhai.youlin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linhai.youlin.common.ErrorCode;
import com.linhai.youlin.config.RabbitMQConfig;
import com.linhai.youlin.constant.AiConstant;
import com.linhai.youlin.exception.BusinessException;
import com.linhai.youlin.mapper.AiMessageMapper;
import com.linhai.youlin.model.domain.AiMessage;
import com.linhai.youlin.model.domain.User;
import com.linhai.youlin.model.request.AiMessageSendRequest;
import com.linhai.youlin.model.vo.AiConversationVO;
import com.linhai.youlin.service.AiMessageService;
import com.linhai.youlin.service.UserService;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChoice;
import com.volcengine.ark.runtime.service.ArkService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiMessageServiceImpl extends ServiceImpl<AiMessageMapper, AiMessage> implements AiMessageService {

    @Resource
    private UserService userService;

    @Resource
    private AiMessageMapper aiMessageMapper; // 注入 AiMessageMapper

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private ArkService arkService;

//    @Resource
//    private JavaMailSender mailSender;

//    // 邮箱配置
//    @Value("${spring.mail.username}")
//    private String from;

    // todo 默认的AI会话过期时间
    @Value("${ai.defaultConversationExpireTime:60}")
    private int defaultConversationExpireTime;
    // todo AI会话消息数量阈值
    @Value("${ai.cleanUpMessageThreshold:100}")
    private int cleanUpMessageThreshold;
    // todo  AI会话不活跃时间阈值
    @Value("${ai.cleanUpInactiveHours:72}") // 默认三天
    private int cleanUpInactiveHours;

    @Override
    public Long initAiConversation(User loginUser) {
        // 检查用户是否登录（已登录应该在controller检验）
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        // 检查用户是否有 AI 会话  只查询 conversationId
        QueryWrapper<AiMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("conversationId"); // 只选择 conversationId
        queryWrapper.eq("userId", loginUser.getId());
        queryWrapper.groupBy("conversationId");
        queryWrapper.last("limit 1");
        AiMessage aiMessage = this.getOne(queryWrapper);

        if (aiMessage != null) {
            // 用户已有 AI 会话
            return aiMessage.getConversationId();
        }

        // 用户没有 AI 会话，创建新的 AI 会话
        long conversationId = IdUtil.getSnowflakeNextId();
        // 添加默认欢迎消息
        AiMessage welcomeMessage = new AiMessage();
        welcomeMessage.setConversationId(conversationId);
        welcomeMessage.setUserId(loginUser.getId());
        welcomeMessage.setSenderType(AiConstant.SENDER_TYPE_DEFAULT);
        welcomeMessage.setContent(AiConstant.DEFAULT_WELCOME_MESSAGE);
        welcomeMessage.setModelId(AiConstant.DEFAULT_MODEL_ID); // 设置默认模型 ID
        boolean saveResult = this.save(welcomeMessage);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建默认 AI 会话失败");
        }
        // 放入Redis缓存
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        try {
            String redisKey = AiConstant.AI_MESSAGE_REDIS_KEY_PREFIX + conversationId;
            List<AiMessage> messages = new ArrayList<>();
            messages.add(welcomeMessage);
            valueOperations.set(redisKey, messages, defaultConversationExpireTime, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
        // 设置最后消息时间
        String redisKey = AiConstant.AI_CONVERSATION_REDIS_KEY_PREFIX + loginUser.getId();
        try {
            redisTemplate.opsForHash().put(redisKey, String.valueOf(conversationId), welcomeMessage.getCreateTime());
        } catch (Exception e) {
            log.error("redis set key error", e);
        }

        return conversationId;
    }

    /**
     * 构建子查询的完整 SQL 语句 (手动构建, 包含参数值)
     *
     * @param userId 用户 ID
     * @return 完整的子查询 SQL 语句
     */
    private String buildSubQuerySql(Long userId) {
        return "SELECT maxId FROM (SELECT conversationId, MAX(id) as maxId FROM ai_message WHERE userId = " + userId + " AND isDelete = 0 GROUP BY conversationId) as sub";
    }


    @Override
    public List<AiConversationVO> listAiConversations(User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        // 优化：使用更简洁的查询方式，直接在主查询中获取最后一条消息的内容和时间
        QueryWrapper<AiMessage> mainQueryWrapper = new QueryWrapper<>();
        mainQueryWrapper.inSql("id", buildSubQuerySql(loginUser.getId()));
        mainQueryWrapper.orderByDesc("createTime"); // 添加排序

        List<AiMessage> aiMessageList = this.list(mainQueryWrapper);

        // 3. 将查询结果转换为 AiConversationVO 列表, 并按时间排序 (优化：在查询时已经排序，这里不需要再排序)
        List<AiConversationVO> conversationVOList = aiMessageList.stream().map(aiMessage -> {
            AiConversationVO vo = new AiConversationVO();
            vo.setConversationId(aiMessage.getConversationId());
            vo.setLastMessage(aiMessage.getContent());
            vo.setLastMessageTime(aiMessage.getCreateTime()); // 使用消息的创建时间
            return vo;
        }).collect(Collectors.toList());
        log.info("用户ID:{}获取AI会话列表成功",loginUser.getId());
        return conversationVOList;
    }

    /**
     * 获取某个会话的最后一条消息
     *
     * @param conversationId
     * @return
     */
    private AiMessage getLastMessage(long conversationId) {
        QueryWrapper<AiMessage> lastMessageQueryWrapper = new QueryWrapper<>();
        lastMessageQueryWrapper.eq("conversationId", conversationId);
        lastMessageQueryWrapper.eq("isDelete", false);
        lastMessageQueryWrapper.orderByDesc("createTime");
        lastMessageQueryWrapper.last("limit 1");
        return this.getOne(lastMessageQueryWrapper);
    }

    /**
     * 获取某个会话的所有有效消息
     *
     * @param conversationId
     * @param loginUser
     * @return
     */
    @Override
    public AiConversationVO getAiConversationMessages(Long conversationId, User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        // 1. 校验该会话属于当前用户
        QueryWrapper<AiMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversationId", conversationId);
        queryWrapper.eq("userId", loginUser.getId());
        queryWrapper.last("limit 1"); // 只需要判断是否存在记录
        long count = this.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话不存在或不属于当前用户");
        }

        // 2. 尝试从 Redis 获取消息
        String redisKey = AiConstant.AI_MESSAGE_REDIS_KEY_PREFIX + conversationId;
        List<AiMessage> messages = (List<AiMessage>) redisTemplate.opsForValue().get(redisKey);

        // 3. 判断是否需要从数据库加载
        if (messages == null || messages.isEmpty()) {
            log.info("Redis 中没有消息，从数据库加载");
            messages = loadMessagesFromDatabase(conversationId, loginUser);
        } else {
            // Redis 中有消息，检查最后一条消息的时间
            AiMessage redisLastMessage = messages.get(messages.size() - 1);
            AiMessage mysqlLastMessage = getLastMessage(conversationId);

            if (mysqlLastMessage == null) {
                // 数据库中没有消息（可能是数据错误，或者会话刚创建），理论上不应该出现这种情况
                log.warn("数据库中没有找到会话 {} 的消息", conversationId);
                // redisTemplate.delete(redisKey); // 清空 Redis 缓存
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据库中没有找到会话的消息");
            } else if (redisLastMessage.getCreateTime() == null) {
                // Redis中最后一条消息创建时间为空 重新加载
                log.warn("Redis中最后一条消息创建时间为空");
                messages = loadMessagesFromDatabase(conversationId, loginUser);
            } else if (redisLastMessage.getCreateTime().before(mysqlLastMessage.getCreateTime())
                    || messages.size() != getValidMessageCount(conversationId)) { // 新增数量比较
            } else if (redisLastMessage.getCreateTime().before(mysqlLastMessage.getCreateTime())) {
                // Redis 中的最后一条消息时间早于数据库中的最后一条消息时间，从数据库加载
                log.info("Redis 中的最后一条消息时间早于数据库，从数据库加载");
                messages = loadMessagesFromDatabase(conversationId, loginUser);
            } else {
                // Redis 中的最后一条消息时间等于或晚于数据库中的最后一条消息时间，直接使用 Redis 中的消息
                log.info("Redis 中的消息是最新的，直接使用");
            }
        }

        // 4. 更新 Redis 缓存（只有当从数据库加载了消息时才需要更新）
        if (messages != null && !messages.equals(redisTemplate.opsForValue().get(redisKey))) { // 使用.equals()方法进行比较
            try {
                redisTemplate.opsForValue().set(redisKey, messages, defaultConversationExpireTime, TimeUnit.MINUTES);
                log.info("已更新 Redis 缓存：{}", redisKey); // 添加日志
            } catch (Exception e) {
                log.error("更新 Redis 缓存失败", e); // 添加日志
            }
        }

        // 5. 构造并返回 AiConversationVO
        AiConversationVO aiConversationVO = new AiConversationVO();
        aiConversationVO.setConversationId(conversationId);
        aiConversationVO.setMessages(messages);
        // 设置最后消息时间 (仅当 messages 列表不为空时)
        if (messages != null && !messages.isEmpty()) {
            String redisKey2 = AiConstant.AI_CONVERSATION_REDIS_KEY_PREFIX + loginUser.getId();
            try {
                redisTemplate.opsForHash().put(redisKey2, String.valueOf(conversationId), messages.get(messages.size() - 1).getCreateTime());
            } catch (Exception e) {
                log.error("redis set key error", e);
            }
        }
        return aiConversationVO;
    }

    /**
     * 获取某个会话的有效消息数量 (isDelete = 0)
     *
     * @param conversationId 会话 ID
     * @return 有效消息数量
     */
    private long getValidMessageCount(Long conversationId) {
        QueryWrapper<AiMessage> countWrapper = new QueryWrapper<>();
        countWrapper.eq("conversationId", conversationId);
        countWrapper.eq("isDelete", false);
        return this.count(countWrapper);
    }

    /**
     * 从数据库加载消息并更新到 Redis
     *
     * @param conversationId
     * @param loginUser
     * @return
     */
    private List<AiMessage> loadMessagesFromDatabase(Long conversationId, User loginUser) {
        QueryWrapper<AiMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversationId", conversationId);
        queryWrapper.eq("userId", loginUser.getId());
        queryWrapper.orderByAsc("createTime");
        queryWrapper.eq("isDelete", false);
        List<AiMessage> messages = this.list(queryWrapper);

        // 放入 Redis 缓存
        String redisKey = AiConstant.AI_MESSAGE_REDIS_KEY_PREFIX + conversationId;
        try {
            if (messages != null) { // 判空
                redisTemplate.opsForValue().set(redisKey, messages, defaultConversationExpireTime, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
        return messages;
    }

    @Override
    public AiMessage sendAiMessage(AiMessageSendRequest sendRequest, User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        Long conversationId = sendRequest.getConversationId();
        String content = sendRequest.getContent();

        if (StringUtils.isBlank(content)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        }

        // 校验该会话属于当前用户
        QueryWrapper<AiMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversationId", conversationId);
        queryWrapper.eq("userId", loginUser.getId());
        queryWrapper.last("limit 1");
        long count = this.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话不存在或不属于当前用户");
        }
        // 获取当前会话的消息列表
        List<AiMessage> messages = getAiConversationMessages(conversationId, loginUser).getMessages();

        // 构造用户消息
        AiMessage userMessage = new AiMessage();
        userMessage.setConversationId(conversationId);
        userMessage.setUserId(loginUser.getId());
        userMessage.setSenderType(AiConstant.SENDER_TYPE_USER);
        userMessage.setContent(content);
        // 获取当前会话的模型ID
        AiMessage defaultMessage = messages.stream()
                .filter(message -> message.getSenderType() == AiConstant.SENDER_TYPE_DEFAULT)
                .findFirst()
                .orElse(null);

        if (defaultMessage == null) {
            // 从数据库中加载默认消息
            QueryWrapper<AiMessage> defaultMessageQueryWrapper = new QueryWrapper<>();
            defaultMessageQueryWrapper.eq("conversationId", conversationId);
            defaultMessageQueryWrapper.eq("userId", loginUser.getId());
            defaultMessageQueryWrapper.eq("senderType", AiConstant.SENDER_TYPE_DEFAULT);
            defaultMessageQueryWrapper.last("limit 1");
            defaultMessage = this.getOne(defaultMessageQueryWrapper);

            if (defaultMessage == null) {
                // 如果数据库中也没有默认消息，抛出异常
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未找到当前AI会话的模型ID");
            } else {
                // 将默认消息添加到 messages 列表中 (确保添加到列表的最前面)
                messages.add(0, defaultMessage);
            }
        }

        userMessage.setModelId(defaultMessage.getModelId()); // 使用已有的模型 ID

        // 构造发送给火山引擎的消息列表
        List<ChatMessage> chatMessages = new ArrayList<>();
        // 添加默认的prompt
        String prompt = defaultMessage.getPrompt();
        if (prompt != null) {
            final ChatMessage systemMessage = ChatMessage.builder().role(ChatMessageRole.SYSTEM).content(prompt).build();
            chatMessages.add(systemMessage);
        }
        // 遍历历史消息，将历史消息传递给火山引擎
        messages.forEach(message -> {
            ChatMessage chatMessage;
            if (message.getSenderType() == AiConstant.SENDER_TYPE_USER) {
                chatMessage = ChatMessage.builder().role(ChatMessageRole.USER).content(message.getContent()).build();
            } else {
                chatMessage = ChatMessage.builder().role(ChatMessageRole.ASSISTANT).content(message.getContent()).build();
            }
            chatMessages.add(chatMessage);
        });
        // 将当前用户发送的消息添加到上下文中
        chatMessages.add(ChatMessage.builder().role(ChatMessageRole.USER).content(content).build());

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(userMessage.getModelId())  // 使用已有的模型ID
                .messages(chatMessages)
                .build();

        // 调用火山引擎 AI
        List<ChatCompletionChoice> choiceList = arkService.createChatCompletion(chatCompletionRequest).getChoices();
        if (CollUtil.isEmpty(choiceList)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI 没有返回任何内容");
        }

        // 获取第一个 choice
        ChatCompletionChoice firstChoice = choiceList.get(0);

        // 检查 message 是否为空
        if (firstChoice.getMessage() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 返回的消息为空");
        }
        String aiResponse = firstChoice.getMessage().stringContent();

        // 构造 AI 返回的消息
        AiMessage aiMessage = new AiMessage();
        aiMessage.setConversationId(conversationId);
        aiMessage.setUserId(loginUser.getId());
        aiMessage.setSenderType(AiConstant.SENDER_TYPE_AI);
        aiMessage.setContent(aiResponse);
        aiMessage.setModelId(userMessage.getModelId()); // 使用已有的模型ID

        // 将消息实时保存到 Redis
        String redisKey = AiConstant.AI_MESSAGE_REDIS_KEY_PREFIX + conversationId;
        try {
            messages.add(userMessage);
            messages.add(aiMessage);
            redisTemplate.opsForValue().set(redisKey, messages, defaultConversationExpireTime, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
        // 设置最后消息时间
        String redisKey2 = AiConstant.AI_CONVERSATION_REDIS_KEY_PREFIX + loginUser.getId();
        try {
            redisTemplate.opsForHash().put(redisKey2, String.valueOf(conversationId), aiMessage.getCreateTime());
        } catch (Exception e) {
            log.error("redis set key error", e);
        }

        // 通过 RabbitMQ 异步保存消息到数据库
        rabbitTemplate.convertAndSend(RabbitMQConfig.AI_MESSAGE_EXCHANGE, RabbitMQConfig.AI_MESSAGE_ROUTING_KEY, userMessage);
        rabbitTemplate.convertAndSend(RabbitMQConfig.AI_MESSAGE_EXCHANGE, RabbitMQConfig.AI_MESSAGE_ROUTING_KEY, aiMessage);
        return userMessage;
    }

    @Override
    public void quitAiConversation(Long conversationId, User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        // 校验该会话属于当前用户
        QueryWrapper<AiMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversationId", conversationId);
        queryWrapper.eq("userId", loginUser.getId());
        queryWrapper.last("limit 1");
        long count = this.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话不存在或不属于当前用户");
        }

        // 更新 Redis 中的过期时间
        String redisKey = AiConstant.AI_MESSAGE_REDIS_KEY_PREFIX + conversationId;
        try {
            redisTemplate.expire(redisKey, defaultConversationExpireTime, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("redis expire key error", e);
        }
    }

    @Override
    public void clearAiConversationMessages(Long conversationId, User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        // 校验该会话属于当前用户
        QueryWrapper<AiMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversationId", conversationId);
        queryWrapper.eq("userId", loginUser.getId());
        queryWrapper.last("limit 1"); // 只需要判断是否存在记录
        long count = this.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话不存在或不属于当前用户");
        }

        // 2. 软删除 MySQL 中的非默认消息
        // 使用 UpdateWrapper 构建 SET 子句和 WHERE 子句
        UpdateWrapper<AiMessage> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("conversationId", conversationId);
        updateWrapper.eq("userId", loginUser.getId());
        updateWrapper.ne("senderType", AiConstant.SENDER_TYPE_DEFAULT); // 排除默认消息
        updateWrapper.set("isDelete", 1); // 设置 isDelete = 1

        this.update(updateWrapper); // 使用 updateWrapper 执行更新
        log.info("已软删除会话ID为:{}的所有非默认消息", conversationId);

        // 3. 删除 Redis 中的消息，删除后，重新加载默认消息
        String redisKey = AiConstant.AI_MESSAGE_REDIS_KEY_PREFIX + conversationId;
        try {
            redisTemplate.delete(redisKey);
            log.info("已删除 Redis 中会话ID为:{}的消息", conversationId);

            // 查询 MySQL 中的默认消息
            QueryWrapper<AiMessage> defaultMessageQueryWrapper = new QueryWrapper<>();
            defaultMessageQueryWrapper.eq("conversationId", conversationId);
            defaultMessageQueryWrapper.eq("userId", loginUser.getId());
            defaultMessageQueryWrapper.eq("senderType", AiConstant.SENDER_TYPE_DEFAULT);
            AiMessage defaultMessage = this.getOne(defaultMessageQueryWrapper); // 获取一条

            if (defaultMessage != null) {
                List<AiMessage> messages = new ArrayList<>();
                messages.add(defaultMessage);
                redisTemplate.opsForValue().set(redisKey, messages, defaultConversationExpireTime, TimeUnit.MINUTES);
                log.info("已将默认消息重新加载到 Redis, 会话ID: {}", conversationId);
            }

        } catch (Exception e) {
            log.error("redis delete or set key error", e);
        }

        // 删除最后消息时间
        String redisKey2 = AiConstant.AI_CONVERSATION_REDIS_KEY_PREFIX + loginUser.getId();
        try {
            redisTemplate.opsForHash().delete(redisKey2, String.valueOf(conversationId));
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
    }

    @Override
    public void deleteAllAiConversationMessages(Long conversationId, User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        // 1. 校验该会话属于当前用户
        QueryWrapper<AiMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversationId", conversationId);
        queryWrapper.eq("userId", loginUser.getId());
        queryWrapper.last("limit 1"); // 只需要判断是否存在记录
        long count = this.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话不存在或不属于当前用户");
        }

        // 2. 硬删除 MySQL 中的非默认消息
        aiMessageMapper.deleteAiMessagesByConversationId(conversationId, loginUser.getId(),  AiConstant.SENDER_TYPE_DEFAULT);
        log.info("已硬删除 MySQL 中会话ID为:{}的非默认消息", conversationId);

        // 3. 删除 Redis 中的消息 (优化：删除后，重新加载默认消息)
        String redisKey = AiConstant.AI_MESSAGE_REDIS_KEY_PREFIX + conversationId;
        try {
            redisTemplate.delete(redisKey);
            log.info("已删除 Redis 中会话ID为:{}的消息", conversationId);
            // 查询 MySQL 中的默认消息
            QueryWrapper<AiMessage> defaultMessageQueryWrapper = new QueryWrapper<>();
            defaultMessageQueryWrapper.eq("conversationId", conversationId);
            defaultMessageQueryWrapper.eq("userId", loginUser.getId());
            defaultMessageQueryWrapper.eq("senderType", AiConstant.SENDER_TYPE_DEFAULT);
            AiMessage defaultMessage = this.getOne(defaultMessageQueryWrapper); // 获取一条

            if (defaultMessage != null) {
                List<AiMessage> messages = new ArrayList<>();
                messages.add(defaultMessage);
                redisTemplate.opsForValue().set(redisKey, messages, defaultConversationExpireTime, TimeUnit.MINUTES);
                log.info("已将默认消息重新加载到 Redis, 会话ID: {}", conversationId);
            }
        } catch (Exception e) {
            log.error("Redis 操作失败", e);
        }

        // 删除最后消息时间
        String redisKey2 = AiConstant.AI_CONVERSATION_REDIS_KEY_PREFIX + loginUser.getId();
        try {
            redisTemplate.opsForHash().delete(redisKey2, String.valueOf(conversationId));
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
    }

//    //todo 导出AI会话聊天记录到邮箱
//    @Override
//    public String exportAiConversationMessages(Long conversationId, User loginUser) {
//        // 检查用户是否登录
//        if (loginUser == null) {
//            throw new BusinessException(ErrorCode.NOT_LOGIN);
//        }
//        // 校验该会话属于当前用户
//        QueryWrapper<AiMessage> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("conversationId", conversationId);
//        queryWrapper.eq("userId", loginUser.getId());
//        queryWrapper.last("limit 1"); // 只需要判断是否存在记录
//        long count = this.count(queryWrapper);
//        if (count == 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话不存在或不属于当前用户");
//        }
//        // 获取所有消息
//        List<AiMessage> messages = getAiConversationMessages(conversationId, loginUser).getMessages();
//        if (messages.isEmpty()) {
//            throw new BusinessException(ErrorCode.NULL_ERROR, "当前会话无消息");
//        }
//        // 转换为 JSON 格式
//        Gson gson = new Gson();
//        String messagesJson = gson.toJson(messages);
//
//        // 发送邮件
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setFrom(from); // 发件人
//        message.setTo(loginUser.getEmail()); // 收件人
//        message.setSubject("您的AI会话消息记录"); // 邮件主题
//        message.setText("您好，以下是您的AI会话消息记录：\n" + messagesJson); // 邮件内容
//
//        try {
//            mailSender.send(message);
//            log.info("邮件发送成功");
//        } catch (Exception e) {
//            log.error("邮件发送失败", e);
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邮件发送失败");
//        }
//
//        return messagesJson;
//    }

    @Override
    public void setAiConversationSettings(Long conversationId, User loginUser, String prompt) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        // 1. 校验该会话属于当前用户 (这部分逻辑可以提取成一个私有方法)
        QueryWrapper<AiMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversationId", conversationId);
        queryWrapper.eq("userId", loginUser.getId());
        queryWrapper.last("limit 1");
        long count = this.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话不存在或不属于当前用户");
        }

        // 2. 获取该会话的系统消息（senderType 为 SENDER_TYPE_DEFAULT）
        QueryWrapper<AiMessage> systemMessageQueryWrapper = new QueryWrapper<>();
        systemMessageQueryWrapper.eq("conversationId", conversationId);
        systemMessageQueryWrapper.eq("userId", loginUser.getId());
        systemMessageQueryWrapper.eq("senderType", AiConstant.SENDER_TYPE_DEFAULT);
        systemMessageQueryWrapper.last("limit 1");

        AiMessage systemMessage = this.getOne(systemMessageQueryWrapper);
        if (systemMessage == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未找到当前 AI 会话的默认消息");
        }

        // 3. 更新 prompt
        systemMessage.setPrompt(prompt);
        boolean updateResult = this.updateById(systemMessage);
        if (!updateResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新 AI 会话设置失败");
        }

        // 4. 软删除该会话的所有非默认消息 (调用之前写过的 clearAiConversationMessages 方法)
        clearAiConversationMessages(conversationId, loginUser);

        // 5. 重新加载包含新 prompt 的默认消息到 Redis (clearAiConversationMessages 方法已经做了这一步)

        // 6. 更新最后消息时间（Redis Hash）
        String redisKey2 = AiConstant.AI_CONVERSATION_REDIS_KEY_PREFIX + loginUser.getId();
        try {
            // 注意：这里应该使用 systemMessage.getUpdateTime()，因为我们更新了 systemMessage
            redisTemplate.opsForHash().put(redisKey2, String.valueOf(conversationId), systemMessage.getUpdateTime());
            log.info("已更新 Redis 中会话ID为:{}的最后消息时间", conversationId);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
    }

    @Override
    public void cleanUpExpiredAiMessages() {
        // 查询所有超过 inactiveHours 小时未活跃的会话
        QueryWrapper<AiMessage> queryWrapper = new QueryWrapper<>();

        queryWrapper.groupBy("conversationId");

        // 使用 having 子句筛选出符合条件的 conversationId
        queryWrapper.having("TIMESTAMPDIFF(HOUR, MAX(createTime), NOW()) > {0}", cleanUpInactiveHours)
                .having("COUNT(*) > {0}", cleanUpMessageThreshold);
        List<AiMessage> inactiveConversations = this.list(queryWrapper);
        if (inactiveConversations.isEmpty()) {
            log.info("没有需要清理的不活跃 AI 会话。"); // 优化：更简洁的日志
            return;
        }
        // 提取 conversationId 列表
        List<Long> conversationIds = inactiveConversations.stream()
                .map(AiMessage::getConversationId)
                .distinct() // 去重
                .collect(Collectors.toList());

        // 删除非默认消息
        QueryWrapper<AiMessage> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.in("conversationId", conversationIds);
        deleteWrapper.ne("senderType", AiConstant.SENDER_TYPE_DEFAULT); // 不删除系统消息
        boolean remove = this.remove(deleteWrapper);
        // 删除缓存中的所有消息
        if (remove) {
            for (Long conversationId : conversationIds) {
                String redisKey = AiConstant.AI_MESSAGE_REDIS_KEY_PREFIX + conversationId;
                try {
                    redisTemplate.delete(redisKey);
                    log.info("已删除 Redis 中会话ID为:{}的消息", conversationId); // 优化：添加日志
                } catch (Exception e) {
                    log.error("redis delete key error", e);
                }
            }
        }
    }
}