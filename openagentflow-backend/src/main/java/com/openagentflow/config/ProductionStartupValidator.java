package com.openagentflow.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * 生产环境启动前置校验。
 * <p>用于阻止默认密钥、默认密码等开发配置进入生产运行。</p>
 */
@Component
public class ProductionStartupValidator implements ApplicationRunner {

    /** 本地开发默认 JWT 密钥。 */
    private static final String DEFAULT_DEV_JWT_SECRET = "openagentflow-local-dev-secret-change-me";

    /** Compose 示例默认 JWT 密钥。 */
    private static final String DEFAULT_COMPOSE_JWT_SECRET = "please-change-this-secret-before-production";

    /** MinIO 示例默认密钥。 */
    private static final String DEFAULT_MINIO_SECRET = "minioadmin";

    /** Spring 环境对象。 */
    private final Environment environment;

    /** OpenAgentFlow 自定义配置。 */
    private final OpenAgentFlowProperties properties;

    public ProductionStartupValidator(Environment environment, OpenAgentFlowProperties properties) {
        this.environment = environment;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProductionProfile()) {
            return;
        }
        validateJwtSecret();
        validateCorsOrigins();
        validateMysqlPassword();
        validateKafka();
        validateObjectStorage();
        validateSecretEncryption();
    }

    /**
     * 判断当前是否生产 Profile。
     *
     * @return 是否生产环境
     */
    private boolean isProductionProfile() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
    }

    /**
     * 校验 JWT 密钥强度。
     */
    private void validateJwtSecret() {
        String secret = properties.getSecurity().getJwtSecret();
        if (!StringUtils.hasText(secret)
                || DEFAULT_DEV_JWT_SECRET.equals(secret)
                || DEFAULT_COMPOSE_JWT_SECRET.equals(secret)
                || secret.length() < 32) {
            throw new IllegalStateException("生产环境必须配置长度不少于 32 位的 OAF_JWT_SECRET，且不能使用示例默认值");
        }
    }

    /**
     * 校验 CORS 来源。
     */
    private void validateCorsOrigins() {
        String origins = properties.getSecurity().getAllowedOrigins();
        if (!StringUtils.hasText(origins) || origins.contains("*") || origins.contains("localhost") || origins.contains("127.0.0.1")) {
            throw new IllegalStateException("生产环境必须通过 OAF_CORS_ALLOWED_ORIGINS 配置正式域名，不能使用 localhost 或通配来源");
        }
    }

    /**
     * 校验 MySQL 密码。
     */
    private void validateMysqlPassword() {
        String password = environment.getProperty("spring.datasource.password");
        if (!StringUtils.hasText(password) || "123456".equals(password) || "password".equalsIgnoreCase(password)) {
            throw new IllegalStateException("生产环境必须配置强 MySQL 密码，不能使用 123456 或 password");
        }
    }

    /**
     * 校验生产环境 Kafka 分布式任务配置。
     */
    private void validateKafka() {
        String bootstrapServers = environment.getProperty("spring.kafka.bootstrap-servers");
        if (!Boolean.TRUE.equals(properties.getAsyncTask().getEnabled())) {
            throw new IllegalStateException("生产环境必须启用 OAF_KAFKA_TASK_ENABLED，保证耗时任务分布式执行");
        }
        if (!StringUtils.hasText(bootstrapServers)
                || bootstrapServers.contains("localhost")
                || bootstrapServers.contains("127.0.0.1")) {
            throw new IllegalStateException("生产环境必须通过 OAF_KAFKA_BOOTSTRAP_SERVERS 配置可用的 Kafka 集群地址");
        }
        if (properties.getAsyncTask().getReplicationFactor() < 3
                || properties.getAsyncTask().getMinInSyncReplicas() < 2) {
            throw new IllegalStateException("生产 Kafka Topic 必须配置副本数至少3、最小同步副本数至少2");
        }
        String securityProtocol = environment.getProperty("spring.kafka.properties.security.protocol", "");
        if (!"SASL_SSL".equalsIgnoreCase(securityProtocol) && !"SSL".equalsIgnoreCase(securityProtocol)) {
            throw new IllegalStateException("生产 Kafka 必须通过 OAF_KAFKA_SECURITY_PROTOCOL 启用 SASL_SSL 或 SSL");
        }
    }

    /**
     * 校验生产环境共享对象存储配置，避免不同 Worker 无法读取上传文档。
     */
    private void validateObjectStorage() {
        OpenAgentFlowProperties.ObjectStorage storage = properties.getObjectStorage();
        if (!Boolean.TRUE.equals(storage.getEnabled())) {
            throw new IllegalStateException("生产环境必须启用 OAF_OBJECT_STORAGE_ENABLED，文档任务需要共享对象存储");
        }
        if (!StringUtils.hasText(storage.getEndpoint())
                || !StringUtils.hasText(storage.getPublicEndpoint())
                || storage.getPublicEndpoint().contains("localhost")
                || storage.getPublicEndpoint().contains("127.0.0.1")
                || !StringUtils.hasText(storage.getBucket())
                || !StringUtils.hasText(storage.getAccessKey())
                || !StringUtils.hasText(storage.getSecretKey())
                || DEFAULT_MINIO_SECRET.equals(storage.getSecretKey())) {
            throw new IllegalStateException("生产环境必须配置正式的 MinIO/S3 内部地址、公网地址、存储桶和非默认访问密钥");
        }
    }

    /** 校验API Key等敏感配置的主加密密钥。 */
    private void validateSecretEncryption() {
        String key = environment.getProperty("openagentflow.security.secret-encryption-key");
        if (!StringUtils.hasText(key) || key.length() < 32 || key.startsWith("CHANGE_ME")) {
            throw new IllegalStateException("生产环境必须配置长度不少于32位的OAF_SECRET_ENCRYPTION_KEY");
        }
    }
}
