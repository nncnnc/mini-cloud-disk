package org.nnc.clouddisk.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * 全局接口日志切面（隐形摄像头）
 */
@Aspect      // 告诉 Spring 这是一个切面类
@Component   // 交给 Spring 容器管理
@Slf4j       // Lombok 提供的日志注解，自动生成 log 对象
public class LogAspect {

    // 注入 Jackson 的 JSON 处理工具，用于把对象转成好看的 JSON 字符串打印
//    private final ObjectMapper objectMapper = new ObjectMapper();
    // 换成这行：让 Spring 自动注入配置好的 ObjectMapper
    @Autowired
    private ObjectMapper objectMapper;
    /**
     * 定义切入点 (Pointcut)
     * 这里的意思是：拦截 com.yourname.disk.controller 包下所有的类的所有方法！
     */
    @Pointcut("execution(* org.nnc.clouddisk.controller..*.*(..))")
    public void webLog() {
    }

    /**
     * 环绕通知 (Around Advice)
     * 顾名思义，它能把 Controller 的方法“包围”起来，在方法执行前和执行后都能做事情
     */
    @Around("webLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取当前 HTTP 请求的上下文
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        // 记录开始时间
        long startTime = System.currentTimeMillis();

        // 2. 打印请求参数的“上半场”日志
        log.info("========================================== Start ==========================================");
        if (request != null) {
            // 打印请求的 URL 和 HTTP 动词 (GET/POST)
            log.info("URL            : {} {}", request.getMethod(), request.getRequestURL().toString());
            // 打印调用者的 IP 地址
            log.info("IP             : {}", request.getRemoteAddr());
        }
        // 打印具体调用的是哪个类的哪个方法
        log.info("Class Method   : {}.{}", joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
        // 打印前端传过来的参数
        log.info("Request Args   : {}", Arrays.toString(joinPoint.getArgs()));

        Object result = null;
        try {
            // 3. 放行！这句话极其重要！它代表让真正的 Controller 方法去执行业务逻辑
            result = joinPoint.proceed();
        } finally {
            // 4. 打印返回结果的“下半场”日志 (使用 finally 保证即使接口报错了，也能打印出耗时)
            long timeCost = System.currentTimeMillis() - startTime;
            
            // 尝试将返回结果转成 JSON 打印，如果是下载文件的二进制流就不强转了
            try {
                String responseStr = objectMapper.writeValueAsString(result);
                // 如果返回体太长，截断一下防止刷屏
                if (responseStr.length() > 500) {
                    responseStr = responseStr.substring(0, 500) + "...(太长已截断)";
                }
                log.info("Response       : {}", responseStr);
            } catch (Exception e) {
                log.info("Response       : [非 JSON 格式数据或序列化失败]");
            }
            
            log.info("Time Cost      : {} ms", timeCost);
            log.info("=========================================== End ===========================================");
            log.info(""); // 留个空行，让各个请求的日志区分更明显
        }

        // 5. 必须把 Controller 的执行结果原封不动地返回给框架，否则前端收不到数据！
        return result;
    }
}