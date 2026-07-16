package com.openagentflow.service;

import com.openagentflow.config.OpenAgentFlowProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import io.minio.RemoveObjectArgs;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/**
 * 分布式任务共享对象存储服务。
 */
@Service
public class SharedObjectStorageService {

    /** 平台对象存储配置。 */
    private final OpenAgentFlowProperties.ObjectStorage properties;

    /** MinIO 客户端。 */
    private final MinioClient minioClient;

    /** 公网预签名客户端，不负责后端对象读写。 */
    private final MinioClient presignClient;

    public SharedObjectStorageService(OpenAgentFlowProperties openAgentFlowProperties) {
        this.properties = openAgentFlowProperties.getObjectStorage();
        this.minioClient = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
        this.presignClient = MinioClient.builder()
                .endpoint(properties.getPublicEndpoint())
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
        return putStream(objectKey, new ByteArrayInputStream(bytes), bytes.length, contentType).bucket();
    }

    /**
     * 以流式方式保存知识库原始文档，并在同一数据流上计算 MD5。
     *
     * @param objectKey 对象键
     * @param input 文件输入流
     * @param size 文件大小
     * @param contentType 文件类型
     * @return 存储结果
     */
    public StoredObject putStream(String objectKey, InputStream input, long size, String contentType) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            DigestInputStream digestInput = new DigestInputStream(input, digest);
            if (!Boolean.TRUE.equals(properties.getEnabled())) {
                Path target = Path.of("data", objectKey);
                Files.createDirectories(target.getParent());
                Files.copy(digestInput, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return new StoredObject("local", objectKey, HexFormat.of().formatHex(digest.digest()), Files.size(target));
            }
            ensureBucket();
            try (InputStream source = digestInput) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(objectKey)
                        .stream(source, size, 10 * 1024 * 1024)
                        .contentType(StringUtils.hasText(contentType) ? contentType : "application/octet-stream")
                        .build());
            }
            return new StoredObject(properties.getBucket(), objectKey, HexFormat.of().formatHex(digest.digest()), size);
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
        try (InputStream input = open(bucket, objectKey)) {
                return input.readAllBytes();
        } catch (Exception exception) {
            throw new IllegalStateException("共享对象存储读取失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 打开对象输入流，调用方负责关闭。
     */
    public InputStream open(String bucket, String objectKey) {
        try {
            if (!StringUtils.hasText(bucket) || "local".equalsIgnoreCase(bucket)) {
                Path path = Path.of(objectKey);
                if (!Files.exists(path)) {
                    path = Path.of("data", objectKey);
                }
                return Files.newInputStream(path);
            }
            return minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception exception) {
            throw new IllegalStateException("共享对象存储流读取失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 把对象流式写入 Worker 临时文件，避免整份文件进入堆内存。
     */
    public Path materializeTempFile(String bucket, String objectKey, String fileExt) {
        try {
            Path temp = Files.createTempFile("oaf-document-", StringUtils.hasText(fileExt) ? "." + fileExt : ".tmp");
            try (InputStream input = open(bucket, objectKey)) {
                Files.copy(input, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return temp;
        } catch (Exception exception) {
            throw new IllegalStateException("共享对象存储临时文件生成失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 创建浏览器直传 MinIO 的预签名 PUT URL。
     */
    public String presignedPutUrl(String objectKey, int expiryMinutes) {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            throw new IllegalStateException("本地文件模式不支持预签名直传");
        }
        try {
            ensureBucket();
            return presignClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .expiry(Math.max(1, Math.min(expiryMinutes, 120)), TimeUnit.MINUTES)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("生成 MinIO 预签名地址失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 查询已直传对象信息。
     */
    public StoredObject stat(String objectKey) {
        try {
            StatObjectResponse response = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .build());
            return new StoredObject(properties.getBucket(), objectKey, normalizeEtag(response.etag()), response.size());
        } catch (Exception exception) {
            throw new IllegalStateException("查询 MinIO 对象失败：" + exception.getMessage(), exception);
        }
    }

    /** 检查指定对象是否真实存在。 */
    public boolean exists(String bucket, String objectKey) {
        try {
            if (!StringUtils.hasText(bucket) || "local".equalsIgnoreCase(bucket)) {
                Path direct = Path.of(objectKey);
                return Files.exists(direct) || Files.exists(Path.of("data", objectKey));
            }
            minioClient.statObject(StatObjectArgs.builder().bucket(bucket).object(objectKey).build());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 删除不再需要的对象，例如重复上传或未完成任务清理。
     */
    public void delete(String bucket, String objectKey) {
        try {
            if (!StringUtils.hasText(bucket) || "local".equalsIgnoreCase(bucket)) {
                Path path = Path.of("data", objectKey);
                Files.deleteIfExists(path);
                return;
            }
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception exception) {
            throw new IllegalStateException("删除对象存储文件失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 将已有对象流式复制到新的工作空间对象键，并重新计算内容哈希。
     *
     * @param sourceBucket 来源存储桶
     * @param sourceKey 来源对象键
     * @param targetKey 目标对象键
     * @param contentType 对象内容类型
     * @param size 已知对象大小
     * @return 目标对象信息
     */
    public StoredObject copy(String sourceBucket, String sourceKey, String targetKey, String contentType, long size) {
        try (InputStream input = open(sourceBucket, sourceKey)) {
            return putStream(targetKey, input, Math.max(0L, size), contentType);
        } catch (Exception exception) {
            throw new IllegalStateException("共享对象存储复制失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 返回平台文档存储桶名称。
     */
    public String bucketName() {
        return Boolean.TRUE.equals(properties.getEnabled()) ? properties.getBucket() : "local";
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

    private String normalizeEtag(String etag) {
        return etag == null ? "" : etag.replace("\"", "");
    }

    /**
     * 对象存储写入结果。
     *
     * @param bucket 存储桶
     * @param objectKey 对象键
     * @param contentHash 内容MD5
     * @param size 对象大小
     */
    public record StoredObject(String bucket, String objectKey, String contentHash, long size) {
    }
}
