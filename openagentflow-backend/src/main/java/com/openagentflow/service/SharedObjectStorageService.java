package com.openagentflow.service;

import com.openagentflow.config.OpenAgentFlowProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 分布式任务共享对象存储服务。
 */
@Service
public class SharedObjectStorageService {

    /** 平台对象存储配置。 */
    private final OpenAgentFlowProperties.ObjectStorage properties;

    /** MinIO 客户端。 */
    private final MinioClient minioClient;

    public SharedObjectStorageService(OpenAgentFlowProperties openAgentFlowProperties) {
        this.properties = openAgentFlowProperties.getObjectStorage();
        this.minioClient = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    /**
     * 保存知识库原始文档。
     *
     * @param objectKey 对象键
     * @param bytes 文件内容
     * @param contentType 文件类型
     * @return 实际存储桶名称，关闭 MinIO 时返回 local
     */
    public String put(String objectKey, byte[] bytes, String contentType) {
        try {
            if (!Boolean.TRUE.equals(properties.getEnabled())) {
                Path target = Path.of("data", objectKey);
                Files.createDirectories(target.getParent());
                Files.write(target, bytes);
                return "local";
            }
            ensureBucket();
            try (InputStream input = new ByteArrayInputStream(bytes)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(objectKey)
                        .stream(input, bytes.length, -1)
                        .contentType(StringUtils.hasText(contentType) ? contentType : "application/octet-stream")
                        .build());
            }
            return properties.getBucket();
        } catch (Exception exception) {
            throw new IllegalStateException("共享对象存储写入失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 读取任务原始文件。
     *
     * @param bucket 存储桶名称
     * @param objectKey 对象键或旧版本地路径
     * @return 文件字节
     */
    public byte[] get(String bucket, String objectKey) {
        try {
            if (!StringUtils.hasText(bucket) || "local".equalsIgnoreCase(bucket)) {
                Path path = Path.of(objectKey);
                if (!Files.exists(path)) {
                    path = Path.of("data", objectKey);
                }
                return Files.readAllBytes(path);
            }
            try (InputStream input = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build())) {
                return input.readAllBytes();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("共享对象存储读取失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 确保 OpenAgentFlow 对象桶存在。
     */
    private synchronized void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(properties.getBucket())
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(properties.getBucket())
                    .build());
        }
    }
}
