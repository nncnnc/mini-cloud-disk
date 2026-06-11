package org.nnc.clouddisk.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.nnc.clouddisk.entity.SysUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 工具类
 * 已与 Spring Boot 环境完美解耦配置
 */
@Component // 第一步：必须打上 Component 交给 Spring 容器管理，否则无法读取配置
public class JwtUtil {

    // 第二步：去掉原本的 final 修饰符和写死的值
    private static String SECRET;
    
    private static long EXPIRE_TIME;

    // 第三步：这是核心神操作！
    // 既然 @Value 不能直接作用于 static 变量，那我们就写一个非静态的 Setter 方法，
    // 让 Spring 启动时调用这个 Setter 方法，在方法内部把读到的值赋给静态变量！
    @Value("${jwt.secret}")
    public void setSecret(String secret) {
        JwtUtil.SECRET = secret;
    }

    @Value("${jwt.expire-time}")
    public void setExpireTime(long expireTime) {
        JwtUtil.EXPIRE_TIME = expireTime;
    }

    /**
     * 根据用户信息生成 Token
     */
    public static String generateToken(SysUser user) {
        Date expireDate = new Date(System.currentTimeMillis() + EXPIRE_TIME);

        return JWT.create()
                .withClaim("userId", user.getId())
                .withClaim("username", user.getUsername())
                .withClaim("role", user.getRole())
                .withExpiresAt(expireDate)
                .sign(Algorithm.HMAC256(SECRET)); // 现在这里用的是 yml 里读出来的动态密钥了！
    }

    /**
     * 校验并解析 Token
     */
    public static DecodedJWT verifyToken(String token) {
        return JWT.require(Algorithm.HMAC256(SECRET)) // 同上，动态密钥
                .build()
                .verify(token);
    }
}