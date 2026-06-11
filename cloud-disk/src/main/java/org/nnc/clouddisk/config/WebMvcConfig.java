package org.nnc.clouddisk.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 全局配置类
 * 目前主要用于配置跨域请求 (CORS)
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 允许跨域访问的路径：这里放行所有 API 接口
        registry.addMapping("/**")
                // 允许跨域访问的源：Spring Boot 3.x 推荐使用 allowedOriginPatterns 代替 allowedOrigins
                .allowedOriginPatterns("*")
                // 允许的请求方式：GET, POST, PUT, DELETE 等
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许携带的请求头
                .allowedHeaders("*")
                // 是否允许携带 Cookie 和身份凭证 (前端传 token 需要开启)
                .allowCredentials(true)
                // 跨域预检请求的有效期 (秒)，在这个时间内不需要再发送 OPTIONS 预检请求
                .maxAge(3600);
    }

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册保安
        registry.addInterceptor(jwtInterceptor)
                // 拦截所有 /api/** 下的接口
                .addPathPatterns("/api/**")
                // 告诉保安，这几个地方不要拦（登录、注册、免密分享下载、MinIO内部下载）
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register"
                );
    }
}