package com.linhai.youlin.job;

import com.linhai.youlin.service.AiMessageService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class CleanUpAiMessageJob {

    @Resource
    private AiMessageService aiMessageService;

    /**
     * 每天凌晨 3 点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanUpExpiredAiMessages() {
        aiMessageService.cleanUpExpiredAiMessages();
    }
}