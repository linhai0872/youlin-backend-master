package com.linhai.youlin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linhai.youlin.model.domain.AiMessage;
import org.apache.ibatis.annotations.Param;

/**
* @author linhai
* @description 针对表【ai_message(AI 消息表)】的数据库操作Mapper
* @createDate 2025-03-13 18:08:34
* @Entity com.linhai.youlin.domain.AiMessage
*/
// AiMessageMapper.java
public interface AiMessageMapper extends BaseMapper<AiMessage> {
    int deleteAiMessagesByConversationId(@Param("conversationId") Long conversationId,
                                         @Param("userId") Long userId,
                                         @Param("senderType") int senderType);
}




