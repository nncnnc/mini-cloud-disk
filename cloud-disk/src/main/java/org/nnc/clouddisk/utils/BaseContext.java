package org.nnc.clouddisk.utils;

/**
 * 基于 ThreadLocal 封装的全局工具类
 * 作用：在同一次 HTTP 请求（同一个线程）内，随时随地获取当前登录用户的 ID
 */
public class BaseContext {

    // 这是一个泛型为 Long 的“隐形口袋”，每个线程都有自己独立的一个，互不干扰
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /**
     * 把用户 ID 放进口袋
     */
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    /**
     * 从口袋里拿出用户 ID
     */
    public static Long getCurrentId() {
        return threadLocal.get();
    }

    /**
     * 清空口袋（极其重要：防止内存泄漏和线程池数据串位！）
     */
    public static void remove() {
        threadLocal.remove();
    }
}