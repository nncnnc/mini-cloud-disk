package org.nnc.clouddisk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.nnc.clouddisk.entity.SysUser;

public interface ISysUserService extends IService<SysUser> {
    // 注册逻辑
    void register(String username, String password);
    // 登录逻辑
    SysUser login(String username, String password);

    void rePassword(String password,String newPassword);
}