package com.openagentflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenAgentFlow 自定义配置对象。
 *
 * <p>这里集中承载 Milvus、安全开关和 RAG 默认参数，避免业务代码散落读取配置键。</p>
 */
@ConfigurationProperties(prefix = "openagentflow")
public class OpenAgentFlowProperties {

    /** Milvus 向量数据库连接配置。 */
    private Milvus milvus = new Milvus();

    /** 安全认证配置。 */
    private Security security = new Security();

    /** RAG 检索默认配置。 */
    private Rag rag = new Rag();

    public Milvus getMilvus() {
        return milvus;
    }

    public void setMilvus(Milvus milvus) {
        this.milvus = milvus;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Rag getRag() {
        return rag;
    }

    public void setRag(Rag rag) {
        this.rag = rag;
    }

    /**
     * Milvus 连接配置。
     */
    public static class Milvus {

        /** 是否启用 Milvus 客户端，开发环境未启动 Milvus 时可关闭以便后端降级启动。 */
        private Boolean enabled = true;

        /** Milvus 主机地址。 */
        private String host = "localhost";

        /** Milvus 服务端口。 */
        private Integer port = 19530;

        /** Milvus database 名称。 */
        private String databaseName = "default";

        /** Milvus 用户名，可为空。 */
        private String username;

        /** Milvus 密码，可为空。 */
        private String password;

        /** 知识库默认 Collection 名称。 */
        private String defaultKnowledgeCollection = "oaf_knowledge_chunks";

        /** Agent 记忆默认 Collection 名称。 */
        private String defaultMemoryCollection = "oaf_agent_memory";

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getDatabaseName() {
            return databaseName;
        }

        public void setDatabaseName(String databaseName) {
            this.databaseName = databaseName;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDefaultKnowledgeCollection() {
            return defaultKnowledgeCollection;
        }

        public void setDefaultKnowledgeCollection(String defaultKnowledgeCollection) {
            this.defaultKnowledgeCollection = defaultKnowledgeCollection;
        }

        public String getDefaultMemoryCollection() {
            return defaultMemoryCollection;
        }

        public void setDefaultMemoryCollection(String defaultMemoryCollection) {
            this.defaultMemoryCollection = defaultMemoryCollection;
        }
    }

    /**
     * 安全认证配置。
     */
    public static class Security {

        /** 是否启用认证鉴权。 */
        private Boolean authEnabled = false;

        /** JWT 签名密钥。 */
        private String jwtSecret = "openagentflow-local-dev-secret-change-me";

        /** JWT 有效分钟数。 */
        private Long jwtExpireMinutes = 1440L;

        /** CORS 允许来源，多个域名用英文逗号分隔。 */
        private String allowedOrigins = "http://localhost:5173,http://127.0.0.1:5173";

        public Boolean getAuthEnabled() {
            return authEnabled;
        }

        public void setAuthEnabled(Boolean authEnabled) {
            this.authEnabled = authEnabled;
        }

        public String getJwtSecret() {
            return jwtSecret;
        }

        public void setJwtSecret(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        public Long getJwtExpireMinutes() {
            return jwtExpireMinutes;
        }

        public void setJwtExpireMinutes(Long jwtExpireMinutes) {
            this.jwtExpireMinutes = jwtExpireMinutes;
        }

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    /**
     * RAG 检索默认配置。
     */
    public static class Rag {

        /** 默认召回 TopK 数量。 */
        private Integer defaultTopK = 5;

        /** 默认相似度阈值。 */
        private Double defaultScoreThreshold = 0.65;

        public Integer getDefaultTopK() {
            return defaultTopK;
        }

        public void setDefaultTopK(Integer defaultTopK) {
            this.defaultTopK = defaultTopK;
        }

        public Double getDefaultScoreThreshold() {
            return defaultScoreThreshold;
        }

        public void setDefaultScoreThreshold(Double defaultScoreThreshold) {
            this.defaultScoreThreshold = defaultScoreThreshold;
        }
    }
}
