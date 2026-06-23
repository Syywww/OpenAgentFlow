package com.openagentflow.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Milvus 客户端配置。
 *
 * <p>当前只负责建立基础连接，Collection 创建、索引构建和向量同步由后续业务服务承接。</p>
 */
@Configuration
public class MilvusClientConfig {

    /**
     * 创建 Milvus Java SDK 客户端。
     *
     * @param properties OpenAgentFlow 自定义配置
     * @return Milvus 服务客户端
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "openagentflow.milvus", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MilvusServiceClient milvusServiceClient(OpenAgentFlowProperties properties) {
        OpenAgentFlowProperties.Milvus milvus = properties.getMilvus();

        // 使用配置文件中的本地 Milvus 地址，默认 localhost:19530。
        ConnectParam.Builder builder = ConnectParam.newBuilder()
                .withHost(milvus.getHost())
                .withPort(milvus.getPort());

        // 如果后续开启 Milvus 鉴权，这里会自动带上用户名和密码。
        if (StringUtils.hasText(milvus.getUsername()) && StringUtils.hasText(milvus.getPassword())) {
            builder.withAuthorization(milvus.getUsername(), milvus.getPassword());
        }

        return new MilvusServiceClient(builder.build());
    }
}
