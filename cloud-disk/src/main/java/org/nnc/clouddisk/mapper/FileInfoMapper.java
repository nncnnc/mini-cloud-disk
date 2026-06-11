package org.nnc.clouddisk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.nnc.clouddisk.entity.FileInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 文件信息 Mapper 接口
 */
@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {

    /* ==========================================================
     * 第一部分：BaseMapper 已经帮你默认实现的方法
     * 只要你继承了 BaseMapper，下面这些方法即使不写，你也能直接调用。
     * ========================================================== */

    // int insert(FileInfo entity);                           // 新增文件记录
    // int deleteById(Serializable id);                       // 根据文件ID删除
    // int updateById(FileInfo entity);                       // 根据文件ID更新信息（比如重命名）
    // FileInfo selectById(Serializable id);                  // 根据文件ID查询文件详情
    // List<FileInfo> selectList(Wrapper<FileInfo> wrapper);  // 根据条件批量查询文件
    // ... 还有几十个类似的方法


    /* ==========================================================
     * 第二部分：网盘系统中需要我们【手动补全】的复杂/特有业务方法
     * 虽然 MyBatis-Plus 的 Wrapper 也能实现这些，但用手写 SQL 性能更高、更清晰
     * ========================================================== */

    /**
     * 1. 业务场景：网盘首页，查询某个用户在特定文件夹下的所有正常文件
     * @param userId   用户ID
     * @param parentId 父文件夹ID (比如 0 代表根目录)
     * @return 文件列表
     */
    @Select("SELECT * FROM file_info WHERE user_id = #{userId} AND parent_id = #{parentId} AND del_flag = 0")
    List<FileInfo> selectFilesByFolder(@Param("userId") Long userId, @Param("parentId") String parentId);

    /**
     * 2. 业务场景：计算用户的网盘已用容量 (将所有文件大小加起来)
     * @param userId 用户ID
     * @return 已使用的总字节数
     */
    @Select("SELECT SUM(file_size) FROM file_info WHERE user_id = #{userId} AND del_flag = 0")
    Long calculateTotalUsedSpace(@Param("userId") Long userId);

    /**
     * 3. 业务场景：批量将文件移入回收站 (逻辑删除，将 del_flag 设为 1)
     * 注意：涉及集合遍历的复杂 SQL，通常写在 XML 里，这里用 <script> 标签在注解中演示
     * @param fileIds 需要删除的文件 ID 集合
     * @return 影响的行数
     */
    @Update({
            "<script>",
            "UPDATE file_info SET del_flag = 1 WHERE file_id IN ",
            "<foreach collection='fileIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int batchMoveToRecycleBin(@Param("fileIds") List<String> fileIds);

}