package com.linhai.youlin.once.importuser;

import com.linhai.youlin.mapper.UserMapper;
import com.linhai.youlin.model.domain.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;

//批量插入用户的一次性任务

/**
 * 导入用户任务
 * @author <a href="https://github.com/linhai0872">林海
 */

@Component
public class InsertUsers {

    @Resource
    private UserMapper userMapper;

    /**
     * 批量插入用户
     */
//    @Scheduled(initialDelay = 5000, fixedRate = Long.MAX_VALUE) //五秒后执行一次任务 间隔long上限时间

    public void doInsertUsers() {
        StopWatch stopWatch = new StopWatch();//spring的任务计时工具
        System.out.println("goodgoodgood");
        stopWatch.start();
        final int INSERT_NUM = 1000;
        for (int i = 0; i < INSERT_NUM; i++) {
            User user = new User();
            user.setUsername("假林海");
            user.setUserAccount("fakelinhai");
//            user.setAvatarUrl("");
            user.setGender(0);
            user.setUserPassword("12345678");
            user.setPhone("123");
            user.setEmail("123@qq.com");
            user.setTags("[]");
            user.setUserStatus(0);
            user.setUserRole(0);
            user.setPlanetCode("11111111");
            userMapper.insert(user);
        }
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }
}
