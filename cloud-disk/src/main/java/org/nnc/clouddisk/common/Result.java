package org.nnc.clouddisk.common;

import lombok.Data;

/**
 * 全局统一 API 响应体
 * @param <T> 具体的数据类型
 */
@Data
public class Result<T> {
    
    private Integer code; // 状态码：200表示成功，500表示失败
    private String msg;   // 提示信息
    private T data;       // 实际返回的数据

    // 默认构造函数
    public Result() {}

    // 全参构造函数
    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 成功时的快捷返回方法
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    /**
     * 失败时的快捷返回方法
     */
    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }
}