package org.nnc.clouddisk.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.nnc.clouddisk.common.Result;
import org.nnc.clouddisk.entity.FileInfo;
import org.nnc.clouddisk.service.IFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件前端控制器
 * 负责接收用户的 HTTP 请求，进行简单的参数校验后，将工作委派给 Service 层
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private IFileService fileService;

    /**
     * 单文件上传接口（自动包含秒传逻辑）
     * @param file     前端通过表单(form-data)传过来的文件，参数名必须叫 "file"
     * @param parentId 目标文件夹 ID，0 表示根目录
     */
    @PostMapping("/upload")
    public Result<FileInfo> upload(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "parentId", required = false, defaultValue = "0") Long parentId) {
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空！");
        }
        try {
            FileInfo savedFile = fileService.uploadFile(file, parentId);
            return Result.success(savedFile);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 文件夹上传接口 — 支持批量上传文件夹内的所有文件
     * 前端通过 FormData 以 "files" 为参数名传多个文件
     * @param files    文件夹内的所有文件列表
     * @param parentId 目标文件夹 ID
     */
    @PostMapping("/upload-folder")
    public Result<List<FileInfo>> uploadFolder(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "parentId", required = false, defaultValue = "0") Long parentId) {
        if (files == null || files.isEmpty()) {
            return Result.error("上传文件列表不能为空！");
        }
        // 检查是否有为空的文件
        List<MultipartFile> validFiles = files.stream()
                .filter(f -> f != null && !f.isEmpty())
                .toList();
        if (validFiles.isEmpty()) {
            return Result.error("上传文件列表不能为空！");
        }
        try {
            List<FileInfo> results = validFiles.stream()
                    .map(file -> {
                        try {
                            return fileService.uploadFile(file, parentId);
                        } catch (Exception e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                    })
                    .toList();
            return Result.success(results);
        } catch (RuntimeException e) {
            throw e; // 交给 GlobalExceptionHandler
        }
    }

    /**
     * 文件下载接口
     * 路径变量形式，例如：http://localhost:8080/api/file/download/1
     */
    @GetMapping("/download/{id}")
    public void download(@PathVariable("id") Long id, HttpServletResponse response) {
        try {
            fileService.downloadFile(id, response);
        } catch (Exception e) {
            log.error("文件下载失败，fileId={}", id, e);
            // 由于是二进制流，重置 response 并写回 JSON 错误信息
            try {
                response.reset();
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(500);
                response.getWriter().write("{\"code\":500,\"msg\":\"文件下载失败\"}");
            } catch (Exception ignored) {
                response.setStatus(500);
            }
        }
    }

    /**
     * 文件预览接口 — 返回文件流供浏览器预览（不做下载提示）
     * GET 请求示例：http://localhost:8080/api/file/preview/1
     */
    @GetMapping("/preview/{id}")
    public void preview(@PathVariable("id") Long id, HttpServletResponse response) {
        try {
            fileService.previewFile(id, response);
        } catch (Exception e) {
            log.error("文件预览失败，fileId={}", id, e);
            try {
                response.reset();
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(500);
                response.getWriter().write("{\"code\":500,\"msg\":\"文件预览失败\"}");
            } catch (Exception ignored) {
                response.setStatus(500);
            }
        }
    }

    /**
     * 分页查询文件列表
     * GET 请求示例：http://localhost:8080/api/file/list?pageNo=1&pageSize=10&parentId=0
     */
    @GetMapping("/list")
    public Result<Page<FileInfo>> getList(
            @RequestParam(defaultValue = "1") Long pageNo,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(defaultValue = "0") Long parentId) {
        Page<FileInfo> pageResult = fileService.getFileList(pageNo, pageSize, parentId);
        return Result.success(pageResult);
    }

    /**
     * 删除文件接口
     * DELETE 请求示例：http://localhost:8080/api/file/delete/1
     */
    @DeleteMapping("/delete/{id}")
    public Result<String> deleteFile(@PathVariable("id") Long id) {
        fileService.deleteFile(id);
        return Result.success("删除成功 (已移入回收站)");
    }

    /**
     * 文件重命名接口
     * PUT 请求示例：http://localhost:8080/api/file/rename?id=1&newName=我的新简历.pdf
     */
    @PutMapping("/rename")
    public Result<FileInfo> rename(
            @RequestParam("id") Long id,
            @RequestParam("newName") String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            return Result.error("新名称不能为空");
        }
        FileInfo updatedFile = fileService.renameFile(id, newName);
        return Result.success(updatedFile);
    }

    /**
     * 移动文件接口
     * PUT 请求示例：http://localhost:8080/api/file/move?id=1&targetParentId=5
     */
    @PutMapping("/move")
    public Result<FileInfo> move(
            @RequestParam("id") Long id,
            @RequestParam("targetParentId") Long targetParentId) {
        FileInfo movedFile = fileService.moveFile(id, targetParentId);
        return Result.success(movedFile);
    }

    /**
     * 生成文件分享链接
     * GET 请求示例：http://localhost:8080/api/file/share?id=1&expireHours=24
     */
    @GetMapping("/share")
    public Result<String> shareFile(
            @RequestParam("id") Long id,
            @RequestParam(value = "expireHours", defaultValue = "24") Integer expireHours) {
        String shareUrl = fileService.generateShareLink(id, expireHours);
        return Result.success(shareUrl);
    }
}