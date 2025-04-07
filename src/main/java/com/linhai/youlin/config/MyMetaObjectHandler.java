package com.linhai.youlin.config; // 或者你的配置包

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * MyBatis Plus 自动填充处理器
 */
@Slf4j
@Component // 必须加 @Component 或 @Configuration 注解，让 Spring 扫描到
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("start insert fill ....");
        // 检查是否有 createTime 属性，如果有，则设置值为 new Date()
        if (metaObject.hasSetter("createTime")) {
            this.strictInsertFill(metaObject, "createTime", Date.class, new Date()); // 严格模式填充
            // 或者使用 setFieldValByName
            // this.setFieldValByName("createTime", new Date(), metaObject);
        }
        // 检查是否有 updateTime 属性 (通常 insert 时也需要填充)
        if (metaObject.hasSetter("updateTime")) {
            this.strictInsertFill(metaObject, "updateTime", Date.class, new Date());
        }
        // 你可以在这里添加其他需要自动填充的字段，比如 isDelete 的默认值等
        // if (metaObject.hasSetter("isDelete")) {
        //    this.strictInsertFill(metaObject, "isDelete", Integer.class, 0); // 假设 isDelete 是 Integer
        // }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("start update fill ....");
        // 检查是否有 updateTime 属性，如果有，则设置值为 new Date()
        if (metaObject.hasSetter("updateTime")) {
            this.strictUpdateFill(metaObject, "updateTime", Date.class, new Date());
            // 或者使用 setFieldValByName
            // this.setFieldValByName("updateTime", new Date(), metaObject);
        }
    }
}