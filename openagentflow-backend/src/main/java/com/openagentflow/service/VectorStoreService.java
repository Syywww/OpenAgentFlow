package com.openagentflow.service;

import com.openagentflow.config.OpenAgentFlowProperties;
import com.openagentflow.domain.vector.VectorStoreStatus;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.GetVersionResponse;
import io.milvus.param.R;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 向量存储服务。
 */
@Service
public class VectorStoreService {

    /** OpenAgentFlow 自定义配置。 */
    private final OpenAgentFlowProperties properties;

    /** Milvus 服务客户端。 */
    private final ObjectProvider<MilvusServiceClient> milvusServiceClientProvider;

    public VectorStoreService(OpenAgentFlowProperties properties, ObjectProvider<MilvusServiceClient> milvusServiceClientProvider) {
        this.properties = properties;
        this.milvusServiceClientProvider = milvusServiceClientProvider;
    }

    /**
     * 查询 Milvus 连接状态。
     *
     * @return 向量存储状态
     */
    public VectorStoreStatus getStatus() {
        OpenAgentFlowProperties.Milvus milvus = properties.getMilvus();
        VectorStoreStatus status = new VectorStoreStatus();
        status.setHost(milvus.getHost());
        status.setPort(milvus.getPort());
        status.setDatabaseName(milvus.getDatabaseName());
        status.setKnowledgeCollection(milvus.getDefaultKnowledgeCollection());
        status.setMemoryCollection(milvus.getDefaultMemoryCollection());

        MilvusServiceClient milvusServiceClient = milvusServiceClientProvider.getIfAvailable();
        if (milvusClientDisabled(milvus, milvusServiceClient)) {
            // Milvus 关闭时后端仍可启动，知识库处理链路会继续保留 MySQL 向量兜底。
            status.setConnected(false);
            status.setMessage("Milvus 未启用，当前使用 MySQL 向量兜底");
            return status;
        }

        try {
            // 通过轻量级版本接口验证 Milvus 是否可访问。
            R<GetVersionResponse> response = milvusServiceClient.getVersion();
            status.setConnected(response.getStatus() == R.Status.Success.getCode());
            status.setMessage(status.getConnected() ? "Milvus 连接正常" : response.getMessage());
        } catch (Exception exception) {
            // Milvus 不可用时只返回状态，不阻断后端主进程启动。
            status.setConnected(false);
            status.setMessage("Milvus 连接失败：" + exception.getMessage());
        }
        return status;
    }

    /**
     * 判断 Milvus 客户端是否处于关闭或不可用状态。
     *
     * @param milvus Milvus 配置
     * @param milvusServiceClient Milvus 客户端
     * @return 是否不可用
     */
    private boolean milvusClientDisabled(OpenAgentFlowProperties.Milvus milvus, MilvusServiceClient milvusServiceClient) {
        return !Boolean.TRUE.equals(milvus.getEnabled()) || milvusServiceClient == null;
    }
}
