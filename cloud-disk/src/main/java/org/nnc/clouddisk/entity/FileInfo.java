package org.nnc.clouddisk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("file_info")
public class FileInfo implements Serializable {

    // 强烈建议加上 serialVersionUID，防止以后修改类结构时反序列化报错
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileName;

    private String filePath;

    private Long fileSize;

    /**
     * 文件指纹，核心逻辑：上传前先查 MD5，若存在则直接关联，实现“秒传”
     */
    private String fileMd5;

    private String fileType;

    private Integer isDirectory;

    private Long parentId;

    private Long userId;
    /**
     * 自动填充：插入时自动生成创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 自动填充：插入和更新时自动更新
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：MyBatis-Plus 会自动过滤掉 is_deleted = 1 的记录
     */
    @TableLogic
    private Integer isDeleted;
}