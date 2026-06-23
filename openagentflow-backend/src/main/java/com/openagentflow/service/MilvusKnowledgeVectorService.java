package com.openagentflow.service;

import com.openagentflow.config.OpenAgentFlowProperties;
import com.openagentflow.entity.KnowledgeChunkEntity;
import com.openagentflow.entity.KnowledgeEmbeddingEntity;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.FlushParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.index.CreateIndexParam;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Milvus 知识向量写入服务。
 */
@Service
public class MilvusKnowledgeVectorService {

    /** Milvus 客户端。 */
    private final ObjectProvider<MilvusServiceClient> milvusServiceClientProvider;

    /** OpenAgentFlow 配置。 */
    private final OpenAgentFlowProperties properties;

    public MilvusKnowledgeVectorService(ObjectProvider<MilvusServiceClient> milvusServiceClientProvider,
                                        OpenAgentFlowProperties properties) {
        this.milvusServiceClientProvider = milvusServiceClientProvider;
        this.properties = properties;
    }

    /**
     * 写入单条知识分片向量，兼容旧调用方。
     *
     * @param collectionName 集合基础名称
     * @param embedding 向量记录
     * @param chunk 分片记录
     * @param vector 向量值
     */
    public void upsertKnowledgeChunk(String collectionName,
                                     KnowledgeEmbeddingEntity embedding,
                                     KnowledgeChunkEntity chunk,
                                     List<Double> vector) {
        upsertKnowledgeChunks(collectionName, List.of(embedding), List.of(chunk), List.of(vector));
    }

    /**
     * 批量写入知识分片向量。
     *
     * @param collectionName 集合基础名称
     * @param embeddings 向量记录列表
     * @param chunks 分片记录列表
     * @param vectors 向量值列表
     */
    public void upsertKnowledgeChunks(String collectionName,
                                      List<KnowledgeEmbeddingEntity> embeddings,
                                      List<KnowledgeChunkEntity> chunks,
                                      List<List<Double>> vectors) {
        if (embeddings == null || embeddings.isEmpty()) {
            return;
        }
        if (chunks == null || vectors == null || embeddings.size() != chunks.size() || embeddings.size() != vectors.size()) {
            throw new IllegalArgumentException("Milvus 批量写入数据数量不一致");
        }
        MilvusServiceClient milvusServiceClient = requireMilvusClient();

        int dimension = vectors.get(0).size();
        String baseCollection = StringUtils.hasText(collectionName)
                ? collectionName
                : properties.getMilvus().getDefaultKnowledgeCollection();
        String targetCollection = dimensionCollectionName(baseCollection, dimension);
        ensureCollection(targetCollection, dimension);

        List<String> primaryKeys = new ArrayList<>(embeddings.size());
        List<String> chunkIds = new ArrayList<>(embeddings.size());
        List<String> documentIds = new ArrayList<>(embeddings.size());
        List<String> kbIds = new ArrayList<>(embeddings.size());
        List<String> contents = new ArrayList<>(embeddings.size());
        List<List<Float>> floatVectors = new ArrayList<>(embeddings.size());

        for (int index = 0; index < embeddings.size(); index++) {
            KnowledgeEmbeddingEntity embedding = embeddings.get(index);
            KnowledgeChunkEntity chunk = chunks.get(index);
            List<Double> vector = vectors.get(index);
            if (vector.size() != dimension) {
                throw new IllegalArgumentException("同一批 Milvus 向量维度必须一致");
            }

            // Milvus Java SDK 要求按字段聚合列数据，这里把行数据转换成列数据。
            primaryKeys.add(embedding.getVectorPrimaryKey());
            chunkIds.add(chunk.getId());
            documentIds.add(chunk.getDocumentId());
            kbIds.add(chunk.getKbId());
            contents.add(truncate(chunk.getContent(), 4096));
            floatVectors.add(vector.stream().map(Double::floatValue).toList());
        }

        InsertParam insertParam = InsertParam.newBuilder()
                .withDatabaseName(properties.getMilvus().getDatabaseName())
                .withCollectionName(targetCollection)
                .withFields(List.of(
                        new InsertParam.Field("vector_primary_key", primaryKeys),
                        new InsertParam.Field("chunk_id", chunkIds),
                        new InsertParam.Field("document_id", documentIds),
                        new InsertParam.Field("kb_id", kbIds),
                        new InsertParam.Field("content", contents),
                        new InsertParam.Field("embedding", floatVectors)
                ))
                .build();
        R<?> result = milvusServiceClient.insert(insertParam);
        assertMilvusSuccess(result, "Milvus 向量批量写入失败");

        // 批量写入后只 flush 一次，避免大文档分片时把网络和磁盘开销放大数百倍。
        milvusServiceClient.flush(FlushParam.newBuilder()
                .withDatabaseName(properties.getMilvus().getDatabaseName())
                .addCollectionName(targetCollection)
                .withSyncFlush(false)
                .build());
    }

    /**
     * 按维度生成实际集合名，避免旧 256 维兜底向量和真实 2048 维模型向量冲突。
     *
     * @param baseCollection 集合基础名称
     * @param dimension 向量维度
     * @return 实际集合名称
     */
    private String dimensionCollectionName(String baseCollection, int dimension) {
        String suffix = "_d" + dimension;
        return baseCollection.endsWith(suffix) ? baseCollection : baseCollection + suffix;
    }

    /**
     * 确保 Milvus 集合存在并已加载。
     *
     * @param collectionName 集合名称
     * @param dimension 向量维度
     */
    private void ensureCollection(String collectionName, int dimension) {
        MilvusServiceClient milvusServiceClient = requireMilvusClient();
        R<Boolean> hasCollection = milvusServiceClient.hasCollection(HasCollectionParam.newBuilder()
                .withDatabaseName(properties.getMilvus().getDatabaseName())
                .withCollectionName(collectionName)
                .build());
        assertMilvusSuccess(hasCollection, "Milvus 集合检查失败");
        if (Boolean.TRUE.equals(hasCollection.getData())) {
            loadCollection(collectionName);
            return;
        }

        List<FieldType> fields = new ArrayList<>();
        fields.add(FieldType.newBuilder()
                .withName("vector_primary_key")
                .withDataType(DataType.VarChar)
                .withPrimaryKey(true)
                .withAutoID(false)
                .withMaxLength(160)
                .build());
        fields.add(varCharField("chunk_id", 80));
        fields.add(varCharField("document_id", 80));
        fields.add(varCharField("kb_id", 80));
        fields.add(varCharField("content", 4096));
        fields.add(FieldType.newBuilder()
                .withName("embedding")
                .withDataType(DataType.FloatVector)
                .withDimension(dimension)
                .build());

        R<?> createResult = milvusServiceClient.createCollection(CreateCollectionParam.newBuilder()
                .withDatabaseName(properties.getMilvus().getDatabaseName())
                .withCollectionName(collectionName)
                .withDescription("OpenAgentFlow 知识库分片向量")
                .withShardsNum(2)
                .withFieldTypes(fields)
                .build());
        assertMilvusSuccess(createResult, "Milvus 集合创建失败");

        R<?> indexResult = milvusServiceClient.createIndex(CreateIndexParam.newBuilder()
                .withDatabaseName(properties.getMilvus().getDatabaseName())
                .withCollectionName(collectionName)
                .withFieldName("embedding")
                .withIndexType(IndexType.HNSW)
                .withMetricType(MetricType.COSINE)
                .withExtraParam("{\"M\":16,\"efConstruction\":128}")
                .withSyncMode(Boolean.FALSE)
                .build());
        assertMilvusSuccess(indexResult, "Milvus 索引创建失败");
        loadCollection(collectionName);
    }

    /**
     * 加载集合到 Milvus 查询节点。
     *
     * @param collectionName 集合名称
     */
    private void loadCollection(String collectionName) {
        MilvusServiceClient milvusServiceClient = requireMilvusClient();
        R<?> loadResult = milvusServiceClient.loadCollection(LoadCollectionParam.newBuilder()
                .withDatabaseName(properties.getMilvus().getDatabaseName())
                .withCollectionName(collectionName)
                .withSyncLoad(false)
                .build());
        assertMilvusSuccess(loadResult, "Milvus 集合加载失败");
    }

    /**
     * 创建 VarChar 字段定义。
     *
     * @param name 字段名称
     * @param maxLength 最大长度
     * @return 字段定义
     */
    private FieldType varCharField(String name, int maxLength) {
        return FieldType.newBuilder()
                .withName(name)
                .withDataType(DataType.VarChar)
                .withMaxLength(maxLength)
                .build();
    }

    /**
     * 校验 Milvus 调用结果。
     *
     * @param result Milvus SDK 返回对象
     * @param message 业务错误消息
     */
    private void assertMilvusSuccess(R<?> result, String message) {
        if (result == null || result.getStatus() == null || result.getStatus() != 0) {
            String detail = result == null ? "无返回" : result.getMessage();
            throw new IllegalStateException(message + "：" + detail);
        }
    }

    /**
     * 获取 Milvus 客户端；开发环境未启动 Milvus 时抛出业务可捕获异常，由上层保留 MySQL 向量兜底。
     *
     * @return Milvus 客户端
     */
    private MilvusServiceClient requireMilvusClient() {
        MilvusServiceClient client = milvusServiceClientProvider.getIfAvailable();
        if (client == null) {
            throw new IllegalStateException("Milvus 未启用或未连接，已使用 MySQL 向量兜底");
        }
        return client;
    }

    /**
     * 截断写入 Milvus 的分片预览文本。
     *
     * @param text 原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}
