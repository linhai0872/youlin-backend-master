package com.linhai.youlin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动类
 * @author <a href="https://github.com/linhai0872">林海
 */

@SpringBootApplication
@MapperScan("com.linhai.youlin.mapper")
@EnableScheduling //开启定时任务
public class MyApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }

}