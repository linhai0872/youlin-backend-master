package com.linhai.youlin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * RedisTemplate 配置
 *
 * @author <a href="https://github.com/linhai0872">林海

 */

//自定义RedisTemplate
@Configuration //说明是自定义配置
public class RedisTemplateConfig {


    //逻辑 原来的类不满足需求 修改为符合需求的

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();//对象数据结构设定
        redisTemplate.setConnectionFactory(connectionFactory);//指定redis连接器 外层注入
        redisTemplate.setKeySerializer(RedisSerializer.string());//指定序列化器
        return redisTemplate;
    }
}
