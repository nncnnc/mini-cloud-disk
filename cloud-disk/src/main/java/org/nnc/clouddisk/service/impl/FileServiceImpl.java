package org.nnc.clouddisk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.nnc.clouddisk.common.exception.FileExistException;
import org.nnc.clouddisk.entity.FileInfo;
import org.nnc.clouddisk.mapper.FileInfoMapper;
import org.nnc.clouddisk.service.IFileService;
import org.nnc.clouddisk.utils.BaseContext;
import org.nnc.clouddisk.utils.Md5Util;
import org.nnc.clouddisk.utils.MinioUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.UUID;

/**
 * 文件业务层实现类
 */
@Slf4j
@Service
public class FileServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements IFileService {

    @Autowired
    private MinioUtil minioUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "fileListCache", allEntries = true)
    public FileInfo uploadFile(MultipartFile file,Long parentId) throws Exception {
        // 获取当前登录用户 ID
        Long currentUserId = BaseContext.getCurrentId();

        // 2. 【新增】核心校验逻辑：判断目标文件夹是否合法
        // 如果 parentId 为空或者为 0，说明是上传到根目录，直接放行
        if (parentId == null) {
            parentId = 0L;
        }
        if (parentId != 0L) {
            // 去数据库查一下这个父级 ID 对应的记录
            FileInfo parentFolder = this.getById(parentId);

            if (parentFolder == null) {
                throw new RuntimeException("目标文件夹不存在！");
            }
            if (parentFolder.getIsDirectory() != 1) { // 假设 1 代表文件夹，0 代表文件
                throw new RuntimeException("目标路径不是一个文件夹，无法上传！");
            }
            if (!parentFolder.getUserId().equals(currentUserId)) {
                throw new RuntimeException("非法操作，您无权上传到该文件夹！");
            }
        }
        // 1. 获取原始文件名和文件大小
        String originalFilename = file.getOriginalFilename();
        long fileSize = file.getSize();

        // 2. 计算文件的 MD5 指纹
        String fileMd5 = Md5Util.getMd5(file);

        // 3. 提取文件后缀名 (例如: .jpg, .mp4)
        String fileType = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileType = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 4. 去数据库查询这个 MD5 是否已经存在
        // 相当于 SQL: SELECT * FROM file_info WHERE file_md5 = 'xxx' LIMIT 1
        FileInfo existFile = this.getOne(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getFileMd5, fileMd5)
                .last("LIMIT 1"));

        // 5. 准备要插入数据库的新文件记录
        FileInfo newFileRecord = new FileInfo();
        newFileRecord.setFileName(originalFilename);
        newFileRecord.setFileSize(fileSize);
        newFileRecord.setFileMd5(fileMd5);
        newFileRecord.setFileType(fileType);
        newFileRecord.setIsDirectory(0);
        newFileRecord.setParentId(parentId);
        newFileRecord.setUserId(currentUserId);

        if (existFile != null) {
            // 既然文件已经在 MinIO 里存过一份了，我们就不需要再上传了。
            // 只需要把数据库里新记录的 filePath，指向老文件的 filePath 即可。
            newFileRecord.setFilePath(existFile.getFilePath());
            log.info("触发秒传！MD5: {}", fileMd5);
            
        } else {

            // 这是一个全新的文件。为了防止不同用户上传同名文件导致互相覆盖，
            // 我们在 MinIO 里存储的文件名不能用原名，必须用 UUID 重新生成。
            String objectName = UUID.randomUUID().toString().replace("-", "") + fileType;
            
            // 调用工具类，把文件推送到 MinIO
            minioUtil.uploadFile(file, objectName);
            
            // 把 MinIO 里的路径赋值给数据库记录
            newFileRecord.setFilePath(objectName);
            log.info("真实上传成功！目标路径: {}", objectName);
        }

        // 6. 将记录保存到数据库 (this.save 是 ServiceImpl 提供的自带方法)
        this.save(newFileRecord);

        // 返回文件信息给前端
        return newFileRecord;
    }

    @Override
    public void downloadFile(Long fileId, HttpServletResponse response) throws Exception {
        FileInfo fileInfo = this.getById(fileId);
        if (fileInfo == null || fileInfo.getIsDeleted() == 1) {
            throw new RuntimeException("文件不存在或已经删除");
        }
        // 🔒 安全检查：验证文件所有权（防止越权下载）
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null || !fileInfo.getUserId().equals(currentUserId)) {
            throw new RuntimeException("非法操作：你没有权限下载此文件！");
        }
        // 先设置响应头，再获取流，防止中间异常导致流泄漏
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileInfo.getFileName(), "UTF-8"));
        response.setContentType("application/octet-stream");
        try (InputStream inputStream = minioUtil.downloadFile(fileInfo.getFilePath());
             ServletOutputStream outputStream = response.getOutputStream()) {
            StreamUtils.copy(inputStream, outputStream);
            outputStream.flush();
        }
    }

    @Override
    public void previewFile(Long fileId, HttpServletResponse response) throws Exception {
        FileInfo fileInfo = this.getById(fileId);
        if (fileInfo == null || fileInfo.getIsDeleted() == 1) {
            throw new RuntimeException("文件不存在或已经删除");
        }
        // 🔒 安全检查
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null || !fileInfo.getUserId().equals(currentUserId)) {
            throw new RuntimeException("非法操作：你没有权限预览此文件！");
        }
        if (fileInfo.getIsDirectory() == 1) {
            throw new RuntimeException("暂不支持预览文件夹！");
        }
        // 根据文件类型设置 Content-Type，让浏览器可以直接渲染
        String contentType = getContentType(fileInfo.getFileType());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(contentType);
        // 不设置 Content-Disposition: attachment，浏览器会尝试 inline 预览
        response.setHeader("Content-Disposition", "inline;filename=" + URLEncoder.encode(fileInfo.getFileName(), "UTF-8"));
        try (InputStream inputStream = minioUtil.downloadFile(fileInfo.getFilePath());
             ServletOutputStream outputStream = response.getOutputStream()) {
            StreamUtils.copy(inputStream, outputStream);
            outputStream.flush();
        }
    }

    /**
     * 根据文件后缀名获取 MIME 类型
     */
    private String getContentType(String fileType) {
        if (fileType == null) return "application/octet-stream";
        return switch (fileType.toLowerCase()) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".svg" -> "image/svg+xml";
            case ".bmp" -> "image/bmp";
            case ".ico" -> "image/x-icon";
            case ".pdf" -> "application/pdf";
            case ".txt", ".log", ".java", ".py", ".js", ".ts", ".vue", ".css", ".html", ".json", ".xml", ".md", ".yml", ".yaml" -> "text/plain;charset=UTF-8";
            case ".mp4" -> "video/mp4";
            case ".webm" -> "video/webm";
            case ".mp3" -> "audio/mpeg";
            case ".wav" -> "audio/wav";
            case ".doc", ".docx" -> "application/msword";
            case ".xls", ".xlsx" -> "application/vnd.ms-excel";
            case ".ppt", ".pptx" -> "application/vnd.ms-powerpoint";
            default -> "application/octet-stream";
        };
    }

    @Cacheable(
            value = "fileListCache",
            key = "T(org.nnc.clouddisk.utils.BaseContext).getCurrentId() + ':' + #parentId + ':' + #pageNo + ':' + #pageSize"
    )
    @Override
    public Page<FileInfo> getFileList(Long pageNo, Long pageSize, Long parentId) {
        // 1. 获取当前登录用户 ID（非常重要，绝对不能查出别人的文件）
        Long currentUserId = BaseContext.getCurrentId();

        // 2. 初始化 MyBatis-Plus 的分页对象
        Page<FileInfo> pageParam = new Page<>(pageNo, pageSize);

        // 3. 构建查询条件
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getUserId, currentUserId) // 必须是自己的文件
                .eq(FileInfo::getParentId, parentId)    // 指定文件夹层级
                .eq(FileInfo::getIsDeleted, 0);           // 0 代表正常文件，未放入回收站

        // 4. 网盘排序规则：
        // 第一优先级：文件夹排在最前面 (假设 is_directory 字段：1是文件夹，0是文件)
        // 第二优先级：按最后更新时间倒序排列 (最新的在前面)
        queryWrapper.orderByDesc(FileInfo::getIsDirectory)
                .orderByDesc(FileInfo::getUpdateTime);

        // 5. 执行分页查询 (this.page 是 ServiceImpl 提供的自带方法)
        return this.page(pageParam, queryWrapper);
    }
    @Override
    @Transactional(rollbackFor = Exception.class) // ⚠️ 非常重要：删除文件夹可能涉及多条SQL，必须加事务！
    @CacheEvict(value = "fileListCache", allEntries = true)
    public void deleteFile(Long id) {
        // 1. 查询当前要删除的文件/文件夹
        FileInfo fileInfo = this.getById(id);
        if (fileInfo == null) {
            return; // 已经不存在了，直接返回
        }

        // 2. 安全校验：防止黑客越权删除别人的文件
        if (!fileInfo.getUserId().equals(BaseContext.getCurrentId())) {
            throw new RuntimeException("非法操作：你没有权限删除此文件！");
        }

        // 3. 核心逻辑分支
        if (fileInfo.getIsDirectory() == 1) {
            // 如果是文件夹，触发递归删除大法
            deleteFolderRecursively(id);
        } else {
            // 如果是普通文件，直接删除自己即可
            this.removeById(id);
        }
    }

    /**
     * 私有辅助方法：递归删除文件夹及其所有子节点
     * 导师解析：这个方法会像剥洋葱一样，一层一层往里找，直到把最深处的文件删掉，再一层层退出来删文件夹。
     */
    private void deleteFolderRecursively(Long folderId) {
        // 1. 查出当前文件夹下所有的直系子节点 (可能是文件，也可能是下一级文件夹)
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getParentId, folderId)
                .eq(FileInfo::getUserId, BaseContext.getCurrentId()); // 安全检查：只删除自己的文件
        List<FileInfo> children = this.list(queryWrapper);

        // 2. 遍历这群子节点
        for (FileInfo child : children) {
            if (child.getIsDirectory() == 1) {
                // 如果发现子节点还是个文件夹，不要慌，自己调用自己（递归）！
                deleteFolderRecursively(child.getId());
            } else {
                // 如果发现是个普通文件，直接干掉
                this.removeById(child.getId());
            }
        }

        // 3. 当上面的 for 循环结束时，说明当前文件夹里面的东西已经全被清空了
        // 最后，安心地挥刀自宫，把当前这个空文件夹删掉
        this.removeById(folderId);
    }
    @Override
    @CacheEvict(value = "fileListCache", allEntries = true)
    public FileInfo createDirectory(String folderName, Long parentId) {
        // 1. 检查同级目录下，是否已经有同名的文件或文件夹了
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getParentId, parentId)
                .eq(FileInfo::getFileName, folderName);

        if (this.count(queryWrapper) > 0) {
            // 抛出我们之前自定义的业务异常！
            throw new FileExistException("当前目录下已存在同名文件夹，请换个名字！");
        }

        // 2. 构造文件夹实体
        FileInfo directory = new FileInfo();
        directory.setFileName(folderName);
        directory.setIsDirectory(1); // 1 代表这是一个文件夹
        directory.setParentId(parentId);
        directory.setUserId(BaseContext.getCurrentId());
        // 文件夹不需要存物理路径和MD5，给默认值即可
        directory.setFilePath("");
        directory.setFileMd5("");
        directory.setFileSize(0L);

        // 3. 保存到数据库
        this.save(directory);
        return directory;
    }

    @Override
    @CacheEvict(value = "fileListCache", allEntries = true)
    public FileInfo renameFile(Long id, String newName) {
        // 1. 查询原文件信息
        FileInfo fileInfo = this.getById(id);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在");
        }
        if (!fileInfo.getUserId().equals(BaseContext.getCurrentId())) {
            throw new RuntimeException("非法操作：你没有权限修改此文件！");
        }
        // 2. 检查同级目录下是否已经存在同名文件
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getParentId, fileInfo.getParentId())
                .eq(FileInfo::getFileName, newName)
                // 排除掉自己（以防用户点重命名但又没改名字直接提交）
                .ne(FileInfo::getId, id);

        if (this.count(queryWrapper) > 0) {
            throw new FileExistException("当前目录下已存在同名文件或文件夹！");
        }

        // 3. 更新名字并保存
        fileInfo.setFileName(newName);
        this.updateById(fileInfo);

        return fileInfo;
    }

    @Override
    @CacheEvict(value = "fileListCache", allEntries = true)
    public FileInfo moveFile(Long id, Long targetParentId) {
        // 1. 查询原文件信息
        FileInfo fileInfo = this.getById(id);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在");
        }
        if (!fileInfo.getUserId().equals(BaseContext.getCurrentId())) {
            throw new RuntimeException("非法操作：你没有权限修改此文件！");
        }
        // 2. 严谨判断：不能移动到自己原来的目录
        if (fileInfo.getParentId().equals(targetParentId)) {
            throw new RuntimeException("文件已经在该目录下了");
        }
        // 3. 防止循环引用：不能将文件夹移动到自身内部或其子孙节点中
        if (fileInfo.getIsDirectory() == 1 && isDescendantOf(targetParentId, fileInfo.getId())) {
            throw new RuntimeException("不能将文件夹移动到自身内部！");
        }

        // 4. 检查目标目录下是否已经存在同名文件
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getParentId, targetParentId)
                .eq(FileInfo::getFileName, fileInfo.getFileName());

        if (this.count(queryWrapper) > 0) {
            throw new FileExistException("目标目录下已存在同名文件，无法移动！");
        }

        // 5. 更新父目录ID并保存
        fileInfo.setParentId(targetParentId);
        this.updateById(fileInfo);

        return fileInfo;
    }

    /**
     * 检查 targetId 是否是 ancestorId 的子孙节点（递归向上查找）
     * 用于防止移动文件夹时产生循环引用
     */
    private boolean isDescendantOf(Long targetId, Long ancestorId) {
        if (targetId == null || targetId == 0) {
            return false; // 到达根目录，说明不是子孙节点
        }
        if (targetId.equals(ancestorId)) {
            return true; // 找到了，说明 targetId 在 ancestorId 的子树中
        }
        FileInfo parent = this.getById(targetId);
        if (parent == null) {
            return false;
        }
        return isDescendantOf(parent.getParentId(), ancestorId);
    }
    @Override
    public String generateShareLink(Long id, Integer expireHours) {
        // 1. 查询文件信息
        FileInfo fileInfo = this.getById(id);

        // 2. 校验文件状态
        if (fileInfo == null || fileInfo.getIsDeleted() == 1) {
            throw new RuntimeException("文件不存在或已被删除！");
        }
        // 3. 🔒 安全检查：验证文件所有权
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null || !fileInfo.getUserId().equals(currentUserId)) {
            throw new RuntimeException("非法操作：你没有权限分享此文件！");
        }
        if (fileInfo.getIsDirectory() == 1) {
            // 企业级处理：文件夹的下载一般要走 ZIP 压缩流，这里暂时拦截
            throw new RuntimeException("暂不支持直接分享文件夹！");
        }

        // 4. 调用工具类生成签名 URL
        try {
            return minioUtil.getPresignedObjectUrl(fileInfo.getFilePath(), expireHours);
        } catch (Exception e) {
            log.error("生成分享链接失败", e);
            throw new RuntimeException("生成分享链接失败：内部服务错误");
        }
    }


}