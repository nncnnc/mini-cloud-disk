package org.nnc.clouddisk.utils;

import io.minio.http.Method;
import org.nnc.clouddisk.config.MinioConfig;
import io.minio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 工具类
 * 封装繁琐的 MinIO API，提供极简的上传、下载、删除方法
 */
@Component
public class MinioUtil {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

    /**
     * 上传文件
     *
     * @param file       Spring MVC 接收到的文件对象
     * @param objectName 存入 MinIO 的目标路径和文件名 (例如: "2023/10/01/uuid.jpg")
     */
    public void uploadFile(MultipartFile file, String objectName) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            // 构造上传参数
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    // 参数：流，文件大小，分片大小（-1代表使用默认分片大小）
                    .stream(inputStream, file.getSize(), -1)
                    // 设置文件类型，这样浏览器下载/预览时能正确识别
                    .contentType(file.getContentType())
                    .build();

            // 执行上传
            minioClient.putObject(putObjectArgs);
        }
    }

    /**
     * 获取文件下载流
     *
     * @param objectName MinIO 中的目标路径和文件名
     * @return 文件的输入流
     */
    public InputStream downloadFile(String objectName) throws Exception {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(objectName)
                .build();
                
        return minioClient.getObject(getObjectArgs);
    }

    /**
     * 删除文件
     *
     * @param objectName MinIO 中的目标路径和文件名
     */
    public void deleteFile(String objectName) throws Exception {
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(objectName)
                .build();
                
        minioClient.removeObject(removeObjectArgs);
    }

    /**
     * 生成带有过期时间的防盗链分享链接
     *
     * @param objectName  MinIO 中的目标文件名 (UUID)
     * @param expireHours 过期时间（小时）
     * @return 临时的下载/预览 URL
     */
    public String getPresignedObjectUrl(String objectName, int expireHours) throws Exception {
        GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                .method(Method.GET) // 指定这是一个 GET 请求链接
                .bucket(minioConfig.getBucketName())
                .object(objectName)
                .expiry(expireHours, TimeUnit.HOURS) // 核心：设置过期时间！
                .build();

        return minioClient.getPresignedObjectUrl(args);
    }
}
