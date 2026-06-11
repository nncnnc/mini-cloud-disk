package org.nnc.clouddisk.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 * 负责在插入或更新数据库表时，自动赋予时间字段默认值
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 当执行 insert 语句时触发
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // 参数说明：(元对象, "实体类中的字段名", 字段的类型, 填充的值)
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 当执行 update 语句时触发
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}