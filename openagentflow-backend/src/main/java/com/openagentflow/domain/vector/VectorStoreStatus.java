package com.openagentflow.domain.vector;

/**
 * 向量存储状态对象。
 */
public class VectorStoreStatus {

    /** Milvus 主机地址。 */
    private String host;

    /** Milvus 服务端口。 */
    private Integer port;

    /** Milvus database 名称。 */
    private String databaseName;

    /** 知识库默认 Collection 名称。 */
    private String knowledgeCollection;

    /** Agent 记忆默认 Collection 名称。 */
    private String memoryCollection;

    /** 连接是否成功。 */
    private Boolean connected;

    /** 状态说明。 */
    private String message;

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

    public String getKnowledgeCollection() {
        return knowledgeCollection;
    }

    public void setKnowledgeCollection(String knowledgeCollection) {
        this.knowledgeCollection = knowledgeCollection;
    }

    public String getMemoryCollection() {
        return memoryCollection;
    }

    public void setMemoryCollection(String memoryCollection) {
        this.memoryCollection = memoryCollection;
    }

    public Boolean getConnected() {
        return connected;
    }

    public void setConnected(Boolean connected) {
        this.connected = connected;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
