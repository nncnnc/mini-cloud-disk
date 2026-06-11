package org.nnc.clouddisk.common.exception;

/**
 * 文件已存在异常
 * @Author nnc
 * @Version 1.0
 */
public class FileExistException extends RuntimeException {

    /**
     * 无参构造，使用默认报错信息
     */
    public FileExistException() {
        super("文件或文件夹已存在，请重命名后重试！");
    }

    /**
     * 带错误信息的构造方法，方便我们在抛出异常时自定义提示语
     * 例如：throw new FileExistException("该目录下已存在同名图片");
     *
     * @param message 自定义错误信息
     */
    public FileExistException(String message) {
        super(message);
    }
}