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
}

