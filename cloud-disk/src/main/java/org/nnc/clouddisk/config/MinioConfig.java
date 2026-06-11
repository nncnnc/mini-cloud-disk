package org.nnc.clouddisk.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 配置类
 * 负责读取 yml 中的配置，并初始化 MinioClient 交给 Spring 管理
 */
@Data // Lombok 注解，自动生成 Getter/Setter (读取 yml 属性必须有 Setter)
@Configuration
@ConfigurationProperties(prefix = "minio") // 自动把 application.yml 中前缀为 minio 的属性注入进下面对应的字段中
public class MinioConfig {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName; // 这个虽然在这里没直接用来构建 Client，但后续业务类可以注入这个配置类来获取桶名

    /**
     * 实例化 MinioClient 并注入到 Spring 容器中
     */
    @Bean
    public MinioClient minioClient() {
        // 使用建造者模式 (Builder) 创建 Minio 客户端对象
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}