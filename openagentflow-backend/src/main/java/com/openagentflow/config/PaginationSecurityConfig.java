package com.openagentflow.config;

import com.openagentflow.service.SignedCursorCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 深分页签名安全配置。 */
@Configuration
public class PaginationSecurityConfig {

    /** 创建 HMAC 签名游标编解码器，生产环境应通过环境变量覆盖密钥。 */
    @Bean
    public SignedCursorCodec signedCursorCodec(
            @Value("${openagentflow.pagination.cursor-secret:openagentflow-local-cursor-secret-change-me}") String secret) {
        return new SignedCursorCodec(secret);
    }
}
