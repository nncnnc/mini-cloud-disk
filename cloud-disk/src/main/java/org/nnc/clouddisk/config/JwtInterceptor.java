package org.nnc.clouddisk.config;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.micrometer.common.lang.Nullable;
import org.nnc.clouddisk.utils.BaseContext;
import org.nnc.clouddisk.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 全局登录拦截器 (保安)
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行跨域预检请求 (OPTIONS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 1. 从 HTTP 请求头 (Header) 中获取前端传来的 Token
        // 行业规范：通常 Token 放在名为 "token" 或 "Authorization" 的请求头里
        String token = request.getHeader("token");

        // 2. 没带手环？直接拦截！
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("访问受限：请先登录！");
        }

        try {
            DecodedJWT jwt = JwtUtil.verifyToken(token);
            Long currentUserId = jwt.getClaim("userId").asLong();

            // 🚀 核心改动：把解析出来的 userId 塞进 ThreadLocal 的隐形口袋里！
            BaseContext.setCurrentId(currentUserId);

            return true;
        } catch (Exception e) {
            throw new RuntimeException("Token 无效或已过期，请重新登录！");
        }

    }
    /**
     * 这个方法会在 Controller 里的方法执行完毕，准备把数据返回给前端之前被调用
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        // 🚀 核心改动：必须清空 ThreadLocal！
        // 因为 Tomcat 底层用的是线程池，线程是用完回收再给别人用的。如果不清空，下一个没登录的人拿到这个线程，就会直接盗用上一个人的身份！
        BaseContext.remove();
    }

}