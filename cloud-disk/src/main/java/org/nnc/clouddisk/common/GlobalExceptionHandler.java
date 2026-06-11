package org.nnc.clouddisk.common;

import lombok.extern.slf4j.Slf4j;
import org.nnc.clouddisk.common.exception.FileExistException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1. 业务逻辑异常拦截 - 文件夹同名冲突
     * 返回 400 表示客户端请求参数有问题
     */
    @ExceptionHandler(FileExistException.class)
    public Result<?> handleFileExistException(FileExistException e) {
        log.warn("文件名冲突: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /**
     * 2. 业务逻辑异常拦截
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        // 如果消息包含 Token/登录 关键字, 返回 401
        String msg = e.getMessage();
        if (msg != null && (msg.contains("Token") || msg.contains("登录") || msg.contains("权限"))) {
            log.warn("认证/授权失败: {}", msg);
            return Result.error(401, msg);
        }
        log.warn("业务警告: {}", e.getMessage());
        return Result.error(500, e.getMessage());
    }

    /**
     * 3. 专门捕获文件大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxSizeException(MaxUploadSizeExceededException e) {
        log.error("文件上传失败, 超过了配置的最大限制!", e);
        return Result.error(413, "文件太大了! 不能超过系统配置的最大限制.");
    }

    /**
     * 4. 兜底异常拦截 (空指针NullPointerException、数据库异常等)
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统发生未知异常:", e);
        return Result.error(500, "服务器开小差了, 请稍后再试!");
    }
}
