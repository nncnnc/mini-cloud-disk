package org.nnc.clouddisk.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.nnc.clouddisk.entity.FileInfo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件业务层接口
 */
public interface IFileService extends IService<FileInfo> {

    /**
     * 上传文件（包含秒传逻辑）
     *
     * @param file 前端传来的文件对象
     * @return 返回保存到数据库的文件信息对象
     */
    FileInfo uploadFile(MultipartFile file,Long parentId) throws Exception;

    /**
     * 下载文件
     *
     * @param fileId   数据库中的文件主键ID
     * @param response HTTP 响应对象（用来把文件流写给浏览器）
     */
    void downloadFile(Long fileId, jakarta.servlet.http.HttpServletResponse response) throws Exception;


    /**
     * 分页获取文件列表
     *
     * @param pageNo   当前页码
     * @param pageSize 每页显示条数
     * @param parentId 父目录ID（0代表根目录）
     * @return 包含文件列表和总条数的分页对象
     */
    Page<FileInfo> getFileList(Long pageNo, Long pageSize, Long parentId);

    /**
     * 删除文件或文件夹（逻辑删除）
     * @param id 文件主键ID
     */
    void deleteFile(Long id);

    /**
     * 新建文件夹
     * @param folderName 文件夹名称
     * @param parentId   父级目录ID (0代表根目录)
     */
    FileInfo createDirectory(String folderName, Long parentId);

    /**
     * 文件/文件夹重命名
     *
     * @param id      文件主键ID
     * @param newName 新名称
     * @return 修改后的文件信息
     */
    FileInfo renameFile(Long id, String newName);

    /**
     * 移动文件/文件夹到指定目录
     *
     * @param id             文件主键ID
     * @param targetParentId 目标父目录ID
     * @return 修改后的文件信息
     */
    FileInfo moveFile(Long id, Long targetParentId);

    /**
     * 文件预览（与下载不同：不强制弹出下载框，浏览器可直接渲染）
     *
     * @param fileId   数据库中的文件主键ID
     * @param response HTTP 响应对象
     */
    void previewFile(Long fileId, jakarta.servlet.http.HttpServletResponse response) throws Exception;

    /**
     * 生成文件分享链接
     *
     * @param id          文件ID
     * @param expireHours 过期时间（小时）
     * @return 预签名 URL
     */
    String generateShareLink(Long id, Integer expireHours);
}