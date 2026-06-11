package org.nnc.clouddisk.utils;

import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * MD5 工具类
 * 用于计算文件或字符串的 MD5 指纹，实现网盘的“秒传”核心逻辑
 */
public class Md5Util {

    /**
     * 计算前端上传的文件 (MultipartFile) 的 MD5
     * * @param file Spring MVC 接收到的文件对象
     * @return 32位小写的 MD5 字符串
     */
    public static String getMd5(MultipartFile file) throws IOException {
        // 利用 try-with-resources 自动关闭流
        try (InputStream inputStream = file.getInputStream()) {
            return DigestUtils.md5DigestAsHex(inputStream);
        }
    }

    /**
     * 计算普通 InputStream 的 MD5
     * * @param inputStream 输入流
     * @return 32位小写的 MD5 字符串
     */
    public static String getMd5(InputStream inputStream) throws IOException {
        return DigestUtils.md5DigestAsHex(inputStream);
    }

    /**
     * 计算普通字符串的 MD5
     * （附赠功能：以后如果你要做用户登录，可以用来对密码进行 MD5 加密存入数据库）
     * * @param text 普通字符串
     * @return 32位小写的 MD5 字符串
     */
    public static String getMd5(String text) {
        return DigestUtils.md5DigestAsHex(text.getBytes(StandardCharsets.UTF_8));
    }
}