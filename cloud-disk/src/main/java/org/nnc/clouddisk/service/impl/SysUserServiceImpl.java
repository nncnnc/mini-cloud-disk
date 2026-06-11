package org.nnc.clouddisk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.nnc.clouddisk.entity.SysUser;
import org.nnc.clouddisk.mapper.SysUserMapper;
import org.nnc.clouddisk.service.ISysUserService;
import org.nnc.clouddisk.utils.BaseContext;
import org.nnc.clouddisk.utils.Md5Util;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    @Override
    public void register(String username, String password) {
        // 1. 检查账号是否已被注册
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, username);
        if (this.count(queryWrapper) > 0) {
            throw new RuntimeException("该用户名已被注册，请换一个！");
        }

        // 2. 密码加密！(调用我们之前写的 Md5Util 处理普通字符串)
        String encryptedPassword = Md5Util.getMd5(password);

        // 3. 封装对象存入数据库
        SysUser newUser = new SysUser();
        newUser.setUsername(username);
        newUser.setPassword(encryptedPassword);
        newUser.setRole("USER"); // 默认注册的都是普通用户
        
        this.save(newUser);
    }

    @Override
    public SysUser login(String username, String password) {
        // 1. 根据用户名去数据库捞人
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, username);
        SysUser user = this.getOne(queryWrapper);

        // 2. 校验账号存不存在
        if (user == null) {
            throw new RuntimeException("账号不存在！");
        }

        // 3. 校验密码对不对 (把用户输入的明文再次MD5，跟数据库里的密文比对)
        String inputPasswordMd5 = Md5Util.getMd5(password);
        if (!user.getPassword().equals(inputPasswordMd5)) {
            throw new RuntimeException("密码错误！");
        }

        // 4. 登录成功，把密码清空再返回给前端（保护隐私）
        user.setPassword(null);
        return user;
    }

    @Override
    public void rePassword(String password, String newPassword) {
        Long currentUserId = BaseContext.getCurrentId();
        SysUser user = this.getById(currentUserId);
        if (user == null) {
            throw new RuntimeException("用户不存在！");
        }

        String inputOldPasswordMd5 = Md5Util.getMd5(password);
        if (!user.getPassword().equals(inputOldPasswordMd5)) {
            throw new RuntimeException("原密码错误，修改失败！");
        }

        String inputNewPasswordMd5 = Md5Util.getMd5(newPassword);
        if (user.getPassword().equals(inputNewPasswordMd5)) {
            throw new RuntimeException("新密码不能与原密码相同！");
        }

        // 强烈建议：新建一个 User 对象，只装载要修改的字段 (id 和 password)
        // 这样底层的 SQL 只会执行 UPDATE sys_user SET password = ? WHERE id = ?
        // 如果直接 this.updateById(user)，会把所有字段重新写一遍，效率低且容易引发并发问题
        SysUser updateUser = new SysUser();
        updateUser.setId(currentUserId);
        updateUser.setPassword(inputNewPasswordMd5);

        this.updateById(updateUser);
    }
}