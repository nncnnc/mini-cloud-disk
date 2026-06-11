package org.nnc.clouddisk.controller;

import org.nnc.clouddisk.common.Result;
import org.nnc.clouddisk.entity.FileInfo;
import org.nnc.clouddisk.service.IFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 文件夹控制器
 * 专门处理目录相关的操作，如新建文件夹
 */
@RestController
@RequestMapping("/api/directory")
public class DirectoryController {

    @Autowired
    private IFileService fileService;

    /**
     * 新建文件夹
     * POST 请求：http://localhost:8080/api/directory/create?folderName=新建文件夹&parentId=0
     */
    @PostMapping("/create")
    public Result<FileInfo> createDirectory(
            @RequestParam("folderName") String folderName,
            @RequestParam(defaultValue = "0") Long parentId) {
        // 参数校验
        if (folderName == null || folderName.trim().isEmpty()) {
            return Result.error("文件夹名称不能为空");
        }
        folderName = folderName.trim();
        if (folderName.length() > 100) {
            return Result.error("文件夹名称过长（最多100个字符）");
        }
        if (folderName.contains("/") || folderName.contains("\\")) {
            return Result.error("文件夹名称不能包含路径分隔符");
        }
        FileInfo directory = fileService.createDirectory(folderName, parentId);
        return Result.success(directory);
    }
}