package org.nnc.clouddisk.controller;

import org.nnc.clouddisk.common.Result;
import org.nnc.clouddisk.entity.SysUser;
import org.nnc.clouddisk.service.ISysUserService;
import org.nnc.clouddisk.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private ISysUserService userService;

    /**
     * 注册接口
     * POST 请求：http://localhost:8080/api/user/register?username=admin&password=123456
     */
    @PostMapping("/register")
    public Result<String> register(@RequestParam("username") String username, 
                                   @RequestParam("password") String password) {
        try {
            userService.register(username, password);
            return Result.success("注册成功！去登录吧");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 登录接口
     * POST 请求：http://localhost:8080/api/user/login?username=admin&password=123456
     */
    @PostMapping("/login")
    // 🚀 这里一定要改成 Result<Map<String, Object>>
    public Result<Map<String, Object>> login(@RequestParam("username") String username,
                                             @RequestParam("password") String password) {
        try {
            // 1. 调用 Service 校验账号密码
            SysUser user = userService.login(username, password);

            // 2. 账号密码正确，颁发 JWT 令牌
            String token = JwtUtil.generateToken(user);

            // 3. 将用户信息和 Token 一起打包返回给前端
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("token", token);
            resultData.put("userInfo", user);

            return Result.success(resultData);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
