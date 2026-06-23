package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.config.OpenAgentFlowProperties;
import com.openagentflow.domain.knowledge.AgentKnowledgeBindingRequest;
import com.openagentflow.domain.knowledge.AgentKnowledgeBindingSummary;
import com.openagentflow.domain.knowledge.KnowledgeBaseDetail;
import com.openagentflow.domain.knowledge.KnowledgeBaseRequest;
import com.openagentflow.domain.knowledge.KnowledgeBaseSummary;
import com.openagentflow.domain.knowledge.KnowledgeChunkSummary;
import com.openagentflow.domain.knowledge.KnowledgeDocumentSummary;
import com.openagentflow.domain.knowledge.KnowledgeRetrievalRequest;
import com.openagentflow.domain.knowledge.KnowledgeRetrievalResult;
import com.openagentflow.domain.knowledge.KnowledgeSource;
import com.openagentflow.domain.knowledge.KnowledgeUploadResult;
import com.openagentflow.entity.AgentEntity;
import com.openagentflow.entity.AgentKnowledgeBindingEntity;
import com.openagentflow.entity.KnowledgeBaseEntity;
import com.openagentflow.entity.KnowledgeChunkEntity;
import com.openagentflow.entity.KnowledgeDocumentEntity;
import com.openagentflow.entity.KnowledgeEmbeddingEntity;
import com.openagentflow.entity.KnowledgeRetrievalLogEntity;
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.AgentKnowledgeBindingMapper;
import com.openagentflow.mapper.AgentMapper;
import com.openagentflow.mapper.KnowledgeBaseMapper;
import com.openagentflow.mapper.KnowledgeChunkMapper;
import com.openagentflow.mapper.KnowledgeDocumentMapper;
import com.openagentflow.mapper.KnowledgeEmbeddingMapper;
import com.openagentflow.mapper.KnowledgeRetrievalLogMapper;
import com.openagentflow.mapper.ModelConfigMapper;
import com.openagentflow.security.AuthUserDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 知识库应用服务。
 */
@Service
public class KnowledgeBaseService {

    /** 日志对象，用于输出文档处理进度和模型调用结果。 */
    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    /** 默认向量连接 ID。 */
    private static final String DEFAULT_VECTOR_CONNECTION_ID = "70000000-0000-0000-0000-000000000001";

    /** 默认知识库向量集合 ID。 */
    private static final String DEFAULT_VECTOR_COLLECTION_ID = "70000000-0000-0000-0000-000000000101";

    /** 知识库 Mapper。 */
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    /** 知识文档 Mapper。 */
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    /** 知识分片 Mapper。 */
    private final KnowledgeChunkMapper knowledgeChunkMapper;

    /** 知识向量 Mapper。 */
    private final KnowledgeEmbeddingMapper knowledgeEmbeddingMapper;

    /** 检索日志 Mapper。 */
    private final KnowledgeRetrievalLogMapper knowledgeRetrievalLogMapper;

    /** Agent 知识库绑定 Mapper。 */
    private final AgentKnowledgeBindingMapper agentKnowledgeBindingMapper;

    /** Agent Mapper。 */
    private final AgentMapper agentMapper;

    /** Agent 资源级权限服务。 */
    private final AgentAccessService agentAccessService;

    /** 工作空间治理服务。 */
    private final WorkspaceGovernanceService workspaceGovernanceService;

    /** 模型配置 Mapper。 */
    private final ModelConfigMapper modelConfigMapper;

    /** 文档解析服务。 */
    private final DocumentParseService documentParseService;

    /** 切片服务。 */
    private final KnowledgeChunkingService chunkingService;

    /** Embedding 服务。 */
    private final EmbeddingService embeddingService;

    /** Milvus 写入服务。 */
    private final MilvusKnowledgeVectorService milvusKnowledgeVectorService;

    /** JDBC 工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** JSON 工具。 */
    private final ObjectMapper objectMapper;

    /** OpenAgentFlow 配置。 */
    private final OpenAgentFlowProperties properties;

    public KnowledgeBaseService(KnowledgeBaseMapper knowledgeBaseMapper,
                                KnowledgeDocumentMapper knowledgeDocumentMapper,
                                KnowledgeChunkMapper knowledgeChunkMapper,
                                KnowledgeEmbeddingMapper knowledgeEmbeddingMapper,
                                KnowledgeRetrievalLogMapper knowledgeRetrievalLogMapper,
                                AgentKnowledgeBindingMapper agentKnowledgeBindingMapper,
                                AgentMapper agentMapper,
                                AgentAccessService agentAccessService,
                                WorkspaceGovernanceService workspaceGovernanceService,
                                ModelConfigMapper modelConfigMapper,
                                DocumentParseService documentParseService,
                                KnowledgeChunkingService chunkingService,
                                EmbeddingService embeddingService,
                                MilvusKnowledgeVectorService milvusKnowledgeVectorService,
                                JdbcTemplate jdbcTemplate,
                                ObjectMapper objectMapper,
                                OpenAgentFlowProperties properties) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.knowledgeEmbeddingMapper = knowledgeEmbeddingMapper;
        this.knowledgeRetrievalLogMapper = knowledgeRetrievalLogMapper;
        this.agentKnowledgeBindingMapper = agentKnowledgeBindingMapper;
        this.agentMapper = agentMapper;
        this.agentAccessService = agentAccessService;
        this.workspaceGovernanceService = workspaceGovernanceService;
        this.modelConfigMapper = modelConfigMapper;
        this.documentParseService = documentParseService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.milvusKnowledgeVectorService = milvusKnowledgeVectorService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * 查询知识库摘要列表。
     *
     * @return 知识库摘要列表
     */
    public List<KnowledgeBaseSummary> listKnowledgeBases() {
        return knowledgeBaseMapper.selectList(new LambdaQueryWrapper<KnowledgeBaseEntity>()
                        .isNull(KnowledgeBaseEntity::getDeletedAt)
                        .orderByDesc(KnowledgeBaseEntity::getUpdatedAt)
                        .last("limit 100"))
                .stream()
                .filter(this::canView)
                .map(this::toSummary)
                .toList();
    }

    /**
     * 查询知识库详情。
     *
     * @param id 知识库 ID
     * @return 知识库详情
     */
    public KnowledgeBaseDetail getKnowledgeBase(String id) {
        KnowledgeBaseEntity entity = requireKnowledgeBase(id);
        assertCanView(entity);
        KnowledgeBaseDetail detail = new KnowledgeBaseDetail();
        KnowledgeBaseSummary summary = toSummary(entity);
        copySummary(summary, detail);
        detail.setDocuments(listDocuments(id));
        detail.setChunks(listChunks(id, 50));
        return detail;
    }

    /**
     * 创建知识库。
     *
     * @param request 保存请求
     * @return 知识库详情
     */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseDetail createKnowledgeBase(KnowledgeBaseRequest request) {
        String userId = currentUserIdOrThrow();
        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setId(newId());
        fillKnowledgeBase(entity, request, true);
        entity.setOwnerUserId(userId);
        entity.setCreatedBy(userId);
        entity.setWorkspaceId(workspaceGovernanceService.attachResource(request.getWorkspaceId(), "knowledge_base", entity.getId(), userId));
        entity.setVersion(0L);
        knowledgeBaseMapper.insert(entity);
        return getKnowledgeBase(entity.getId());
    }

    /**
     * 更新知识库。
     *
     * @param id 知识库 ID
     * @param request 保存请求
     * @return 知识库详情
     */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseDetail updateKnowledgeBase(String id, KnowledgeBaseRequest request) {
        KnowledgeBaseEntity entity = requireKnowledgeBase(id);
        assertCanManage(entity);
        fillKnowledgeBase(entity, request, false);
        knowledgeBaseMapper.updateById(entity);
        return getKnowledgeBase(id);
    }

    /**
     * 软删除知识库。
     *
     * @param id 知识库 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(String id) {
        KnowledgeBaseEntity entity = requireKnowledgeBase(id);
        assertCanManage(entity);
        entity.setStatus("deleted");
        entity.setDeletedAt(LocalDateTime.now());
        knowledgeBaseMapper.updateById(entity);
    }

    /**
     * 上传、解析、切片并向量化文档。
     *
     * @param kbId 知识库 ID
     * @param file 上传文件
     * @return 上传处理结果
     */
    public KnowledgeUploadResult uploadDocument(String kbId, MultipartFile file) {
        KnowledgeBaseEntity kb = requireKnowledgeBase(kbId);
        assertCanManage(kb);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("DOCUMENT_EMPTY", "上传文件不能为空");
        }
        try {
            byte[] bytes = file.getBytes();
            String fileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "document.txt";
            String fileExt = fileExt(fileName);
            String fileHash = DigestUtils.md5DigestAsHex(bytes);
            String documentId = newId();
            String storageKey = saveUploadFile(kbId, documentId, fileName, bytes);

            KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
            document.setId(documentId);
            document.setKbId(kbId);
            document.setDocName(fileName);
            document.setDocType(fileExt);
            document.setFileExt(fileExt);
            document.setFileSize(file.getSize());
            document.setFileHash(fileHash);
            document.setStorageBucket("local");
            document.setStorageKey(storageKey);
            document.setSourceType("upload");
            document.setParseStatus("processing");
            document.setMetadata("{}");
            document.setUploadedBy(currentUserId());
            knowledgeDocumentMapper.insert(document);

            String text = documentParseService.parse(bytes, fileExt);
            List<String> chunks = chunkingService.split(text, kb.getChunkSize(), kb.getChunkOverlap());
            if (chunks.isEmpty()) {
                throw new BusinessException("DOCUMENT_CHUNK_EMPTY", "文档没有生成有效分片");
            }

            ModelConfigEntity embeddingModel = embeddingService.resolveEmbeddingModel(kb.getEmbeddingModelId());
            if (!StringUtils.hasText(kb.getEmbeddingModelId())) {
                kb.setEmbeddingModelId(embeddingModel.getId());
                knowledgeBaseMapper.updateById(kb);
            }
            List<List<Double>> vectors = embeddingService.embed(embeddingModel, chunks);
            boolean allMilvusSynced = true;
            String milvusMessage = "";
            for (int index = 0; index < chunks.size(); index++) {
                KnowledgeChunkEntity chunk = saveChunk(kb, document, chunks.get(index), index + 1);
                KnowledgeEmbeddingEntity embedding = saveEmbedding(kb, chunk, embeddingModel, vectors.get(index));
                try {
                    milvusKnowledgeVectorService.upsertKnowledgeChunk(kb.getMilvusCollectionName(), embedding, chunk, vectors.get(index));
                    embedding.setSyncStatus("synced");
                    embedding.setLastSyncedAt(LocalDateTime.now());
                } catch (Exception exception) {
                    allMilvusSynced = false;
                    milvusMessage = exception.getMessage();
                    embedding.setSyncStatus("mysql_fallback");
                }
                knowledgeEmbeddingMapper.updateById(embedding);
            }
            document.setParseStatus("parsed");
            knowledgeDocumentMapper.updateById(document);

            KnowledgeUploadResult result = new KnowledgeUploadResult();
            result.setDocument(toDocumentSummary(document));
            result.setChunkCount(chunks.size());
            result.setEmbeddingCount(vectors.size());
            result.setMilvusSynced(allMilvusSynced);
            result.setMessage(allMilvusSynced ? "文档已解析、切片、向量化并写入 Milvus" : "Milvus 写入失败，已保留 MySQL 向量兜底：" + milvusMessage);
            return result;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("DOCUMENT_UPLOAD_FAILED", exception.getMessage());
        }
    }

    /**
     * 查询知识库文档列表。
     *
     * @param kbId 知识库 ID
     * @return 文档摘要列表
     */
    public List<KnowledgeDocumentSummary> listDocuments(String kbId) {
        KnowledgeBaseEntity kb = requireKnowledgeBase(kbId);
        assertCanView(kb);
        return knowledgeDocumentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                        .eq(KnowledgeDocumentEntity::getKbId, kbId)
                        .orderByDesc(KnowledgeDocumentEntity::getUploadedAt))
                .stream()
                .map(this::toDocumentSummary)
                .toList();
    }

    /**
     * 查询知识库分片列表。
     *
     * @param kbId 知识库 ID
     * @param limit 返回上限
     * @return 分片摘要列表
     */
    public List<KnowledgeChunkSummary> listChunks(String kbId, int limit) {
        KnowledgeBaseEntity kb = requireKnowledgeBase(kbId);
        assertCanView(kb);
        return knowledgeChunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunkEntity>()
                        .eq(KnowledgeChunkEntity::getKbId, kbId)
                        .orderByDesc(KnowledgeChunkEntity::getCreatedAt)
                        .last("limit " + Math.max(1, Math.min(limit, 200))))
                .stream()
                .map(this::toChunkSummary)
                .toList();
    }

    /**
     * 执行单个知识库检索测试。
     *
     * @param kbId 知识库 ID
     * @param request 检索请求
     * @return 检索结果
     */
    public KnowledgeRetrievalResult retrievalTest(String kbId, KnowledgeRetrievalRequest request) {
        KnowledgeBaseEntity kb = requireKnowledgeBase(kbId);
        assertCanView(kb);
        int topK = request.getTopK() == null ? properties.getRag().getDefaultTopK() : request.getTopK();
        double threshold = request.getScoreThreshold() == null ? properties.getRag().getDefaultScoreThreshold() : request.getScoreThreshold();
        return retrieveFromKnowledgeBase(kb, null, null, request.getQuery(), topK, threshold);
    }

    /**
     * 查询 Agent 已绑定知识库。
     *
     * @param agentId Agent ID
     * @return 绑定摘要列表
     */
    public List<AgentKnowledgeBindingSummary> listAgentBindings(String agentId) {
        AgentEntity agent = requireAgent(agentId);
        agentAccessService.assertCanView(agent);
        return agentKnowledgeBindingMapper.selectList(new LambdaQueryWrapper<AgentKnowledgeBindingEntity>()
                        .eq(AgentKnowledgeBindingEntity::getAgentId, agentId)
                        .eq(AgentKnowledgeBindingEntity::getEnabled, true))
                .stream()
                .map(this::toBindingSummary)
                .toList();
    }

    /**
     * 保存 Agent 知识库绑定。
     *
     * @param agentId Agent ID
     * @param request 绑定请求
     * @return 保存后的绑定列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<AgentKnowledgeBindingSummary> saveAgentBindings(String agentId, AgentKnowledgeBindingRequest request) {
        AgentEntity agent = requireAgent(agentId);
        agentAccessService.assertCanManage(agent);
        agentKnowledgeBindingMapper.delete(new LambdaQueryWrapper<AgentKnowledgeBindingEntity>()
                .eq(AgentKnowledgeBindingEntity::getAgentId, agentId));
        List<String> ids = request.getKnowledgeBaseIds() == null ? List.of() : request.getKnowledgeBaseIds();
        Set<String> uniqueIds = new LinkedHashSet<>(ids);
        String config = toJson(Map.of(
                "topK", request.getTopK() == null ? properties.getRag().getDefaultTopK() : request.getTopK(),
                "scoreThreshold", request.getScoreThreshold() == null ? properties.getRag().getDefaultScoreThreshold() : request.getScoreThreshold()
        ));
        for (String kbId : uniqueIds) {
            KnowledgeBaseEntity kb = requireKnowledgeBase(kbId);
            assertCanView(kb);
            AgentKnowledgeBindingEntity binding = new AgentKnowledgeBindingEntity();
            binding.setAgentId(agentId);
            binding.setKnowledgeBaseId(kbId);
            binding.setRetrievalConfig(config);
            binding.setEnabled(true);
            agentKnowledgeBindingMapper.insert(binding);
        }
        return listAgentBindings(agentId);
    }

    /**
     * 根据 Agent 绑定的知识库执行 RAG 检索。
     *
     * @param agent Agent 实体
     * @param query 用户问题
     * @param runId 运行 ID
     * @return 引用来源列表
     */
    public List<KnowledgeSource> retrieveForAgent(AgentEntity agent, String query, String runId) {
        if (agent == null || !StringUtils.hasText(query)) {
            return List.of();
        }
        List<AgentKnowledgeBindingEntity> bindings = agentKnowledgeBindingMapper.selectList(new LambdaQueryWrapper<AgentKnowledgeBindingEntity>()
                .eq(AgentKnowledgeBindingEntity::getAgentId, agent.getId())
                .eq(AgentKnowledgeBindingEntity::getEnabled, true));
        List<KnowledgeSource> allSources = new ArrayList<>();
        for (AgentKnowledgeBindingEntity binding : bindings) {
            KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(binding.getKnowledgeBaseId());
            if (kb == null || kb.getDeletedAt() != null || !"active".equalsIgnoreCase(kb.getStatus())) {
                continue;
            }
            Map<String, Object> config = parseMap(binding.getRetrievalConfig());
            int topK = intValue(config.get("topK"), properties.getRag().getDefaultTopK());
            double threshold = doubleValue(config.get("scoreThreshold"), properties.getRag().getDefaultScoreThreshold());
            KnowledgeRetrievalResult result = retrieveFromKnowledgeBase(kb, agent.getId(), runId, query, topK, threshold);
            allSources.addAll(result.getSources());
        }
        return allSources.stream()
                .sorted(Comparator.comparing(KnowledgeSource::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(properties.getRag().getDefaultTopK())
                .toList();
    }

    /**
     * 执行单知识库 MySQL 向量兜底检索。
     *
     * @param kb 知识库实体
     * @param agentId Agent ID
     * @param runId 运行 ID
     * @param query 查询文本
     * @param topK 返回条数
     * @param threshold 相似度阈值
     * @return 检索结果
     */
    private KnowledgeRetrievalResult retrieveFromKnowledgeBase(KnowledgeBaseEntity kb,
                                                              String agentId,
                                                              String runId,
                                                              String query,
                                                              int topK,
                                                              double threshold) {
        Instant startedAt = Instant.now();
        ModelConfigEntity embeddingModel = embeddingService.resolveEmbeddingModel(kb.getEmbeddingModelId());
        List<Double> queryVector = embeddingService.embed(embeddingModel, List.of(query)).getFirst();
        List<KnowledgeSource> sources = new ArrayList<>();
        List<KnowledgeEmbeddingEntity> embeddings = knowledgeEmbeddingMapper.selectList(new LambdaQueryWrapper<KnowledgeEmbeddingEntity>()
                .eq(KnowledgeEmbeddingEntity::getKbId, kb.getId())
                .isNotNull(KnowledgeEmbeddingEntity::getEmbeddingJson)
                .last("limit 2000"));
        for (KnowledgeEmbeddingEntity embedding : embeddings) {
            KnowledgeChunkEntity chunk = knowledgeChunkMapper.selectById(embedding.getChunkId());
            if (chunk == null || !"active".equalsIgnoreCase(chunk.getStatus())) {
                continue;
            }
            List<Double> vector = parseVector(embedding.getEmbeddingJson());
            double score = cosine(queryVector, vector);
            if (score < threshold) {
                continue;
            }
            sources.add(toSource(kb, chunk, score));
        }
        sources = sources.stream()
                .sorted(Comparator.comparing(KnowledgeSource::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(Math.max(1, topK))
                .toList();
        String logId = saveRetrievalLog(kb, agentId, runId, query, queryVector, topK, threshold, sources, startedAt);
        sources.forEach(source -> source.setRetrievalLogId(logId));

        KnowledgeRetrievalResult result = new KnowledgeRetrievalResult();
        result.setRetrievalLogId(logId);
        result.setSources(sources);
        result.setLatencyMs((int) Duration.between(startedAt, Instant.now()).toMillis());
        return result;
    }

    /**
     * 填充知识库实体。
     *
     * @param entity 知识库实体
     * @param request 保存请求
     * @param create 是否创建场景
     */
    private void fillKnowledgeBase(KnowledgeBaseEntity entity, KnowledgeBaseRequest request, boolean create) {
        String code = StringUtils.hasText(request.getKbCode()) ? request.getKbCode().trim() : slugify(request.getKbName());
        entity.setKbCode(create ? uniqueKbCode(code) : code);
        entity.setKbName(request.getKbName().trim());
        entity.setDescription(request.getDescription());
        if (!create && StringUtils.hasText(request.getWorkspaceId())) {
            entity.setWorkspaceId(workspaceGovernanceService.attachResource(request.getWorkspaceId(), "knowledge_base", entity.getId(), entity.getOwnerUserId()));
        }
        ModelConfigEntity embeddingModel = embeddingService.resolveEmbeddingModel(request.getEmbeddingModelId());
        entity.setEmbeddingModelId(embeddingModel.getId());
        entity.setVectorConnectionId(DEFAULT_VECTOR_CONNECTION_ID);
        entity.setVectorCollectionId(DEFAULT_VECTOR_COLLECTION_ID);
        entity.setMilvusCollectionName(properties.getMilvus().getDefaultKnowledgeCollection());
        entity.setChunkStrategy(StringUtils.hasText(request.getChunkStrategy()) ? request.getChunkStrategy() : "fixed_size");
        entity.setChunkSize(request.getChunkSize() == null ? 512 : request.getChunkSize());
        entity.setChunkOverlap(request.getChunkOverlap() == null ? 64 : request.getChunkOverlap());
        entity.setVisibility(StringUtils.hasText(request.getVisibility()) ? request.getVisibility() : "private");
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "active");
    }

    /**
     * 保存切片记录。
     *
     * @param kb 知识库
     * @param document 文档
     * @param content 切片内容
     * @param chunkNo 切片序号
     * @return 切片实体
     */
    private KnowledgeChunkEntity saveChunk(KnowledgeBaseEntity kb, KnowledgeDocumentEntity document, String content, int chunkNo) {
        KnowledgeChunkEntity chunk = new KnowledgeChunkEntity();
        chunk.setId(newId());
        chunk.setKbId(kb.getId());
        chunk.setDocumentId(document.getId());
        chunk.setChunkNo(chunkNo);
        chunk.setTitle(document.getDocName() + " #" + chunkNo);
        chunk.setContent(content);
        chunk.setTokenCount(chunkingService.estimateTokens(content));
        chunk.setStartOffset(0);
        chunk.setEndOffset(content.length());
        chunk.setMetadata("{}");
        chunk.setStatus("active");
        knowledgeChunkMapper.insert(chunk);
        return chunk;
    }

    /**
     * 保存向量记录。
     *
     * @param kb 知识库
     * @param chunk 分片
     * @param model Embedding 模型
     * @param vector 向量
     * @return 向量实体
     */
    private KnowledgeEmbeddingEntity saveEmbedding(KnowledgeBaseEntity kb,
                                                   KnowledgeChunkEntity chunk,
                                                   ModelConfigEntity model,
                                                   List<Double> vector) {
        KnowledgeEmbeddingEntity embedding = new KnowledgeEmbeddingEntity();
        embedding.setId(newId());
        embedding.setChunkId(chunk.getId());
        embedding.setKbId(kb.getId());
        embedding.setModelId(model.getId());
        embedding.setVectorCollectionId(kb.getVectorCollectionId());
        embedding.setMilvusCollectionName(kb.getMilvusCollectionName());
        embedding.setVectorPrimaryKey("chunk_" + chunk.getId().replace("-", ""));
        embedding.setSyncStatus("pending");
        embedding.setEmbeddingJson(toJson(vector));
        embedding.setEmbeddingDim(vector.size());
        embedding.setContentHash(DigestUtils.md5DigestAsHex(chunk.getContent().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        knowledgeEmbeddingMapper.insert(embedding);
        return embedding;
    }

    /**
     * 保存检索日志。
     *
     * @param kb 知识库
     * @param agentId Agent ID
     * @param runId 运行 ID
     * @param query 查询文本
     * @param queryVector 查询向量
     * @param topK 返回条数
     * @param threshold 阈值
     * @param sources 引用来源
     * @param startedAt 开始时间
     * @return 检索日志 ID
     */
    private String saveRetrievalLog(KnowledgeBaseEntity kb,
                                    String agentId,
                                    String runId,
                                    String query,
                                    List<Double> queryVector,
                                    int topK,
                                    double threshold,
                                    List<KnowledgeSource> sources,
                                    Instant startedAt) {
        KnowledgeRetrievalLogEntity log = new KnowledgeRetrievalLogEntity();
        log.setId(newId());
        log.setKbId(kb.getId());
        log.setAgentId(agentId);
        log.setRunId(runId);
        log.setVectorCollectionId(kb.getVectorCollectionId());
        log.setMilvusCollectionName(kb.getMilvusCollectionName());
        log.setQueryText(query);
        log.setQueryEmbeddingJson(toJson(queryVector));
        log.setMilvusSearchParams(toJson(Map.of("mode", "mysql_cosine_fallback")));
        log.setTopK(topK);
        log.setScoreThreshold(BigDecimal.valueOf(threshold));
        log.setRerankEnabled(false);
        log.setResultCount(sources.size());
        log.setLatencyMs((int) Duration.between(startedAt, Instant.now()).toMillis());
        log.setResults(toJson(sources));
        log.setMilvusResultIds(toJson(sources.stream().map(KnowledgeSource::getChunkId).toList()));
        knowledgeRetrievalLogMapper.insert(log);
        return log.getId();
    }

    /**
     * 转换知识库摘要。
     *
     * @param entity 知识库实体
     * @return 摘要对象
     */
    private KnowledgeBaseSummary toSummary(KnowledgeBaseEntity entity) {
        KnowledgeBaseSummary item = new KnowledgeBaseSummary();
        item.setId(entity.getId());
        item.setKbCode(entity.getKbCode());
        item.setKbName(entity.getKbName());
        item.setDescription(entity.getDescription());
        item.setWorkspaceId(entity.getWorkspaceId());
        item.setWorkspaceName(findWorkspaceName(entity.getWorkspaceId()));
        item.setEmbeddingModelId(entity.getEmbeddingModelId());
        item.setEmbeddingModelName(findModelName(entity.getEmbeddingModelId()));
        item.setChunkStrategy(entity.getChunkStrategy());
        item.setChunkSize(entity.getChunkSize());
        item.setChunkOverlap(entity.getChunkOverlap());
        item.setMilvusCollectionName(entity.getMilvusCollectionName());
        item.setStatus(entity.getStatus());
        item.setDocumentCount(count("knowledge_document", "kb_id", entity.getId()));
        item.setChunkCount(count("knowledge_chunk", "kb_id", entity.getId()));
        item.setEmbeddingCount(count("knowledge_embedding", "kb_id", entity.getId()));
        item.setCreatedAt(entity.getCreatedAt());
        item.setUpdatedAt(entity.getUpdatedAt());
        return item;
    }

    /**
     * 拷贝摘要字段到详情对象。
     *
     * @param source 摘要
     * @param target 详情
     */
    private void copySummary(KnowledgeBaseSummary source, KnowledgeBaseDetail target) {
        target.setId(source.getId());
        target.setKbCode(source.getKbCode());
        target.setKbName(source.getKbName());
        target.setDescription(source.getDescription());
        target.setWorkspaceId(source.getWorkspaceId());
        target.setWorkspaceName(source.getWorkspaceName());
        target.setEmbeddingModelId(source.getEmbeddingModelId());
        target.setEmbeddingModelName(source.getEmbeddingModelName());
        target.setChunkStrategy(source.getChunkStrategy());
        target.setChunkSize(source.getChunkSize());
        target.setChunkOverlap(source.getChunkOverlap());
        target.setMilvusCollectionName(source.getMilvusCollectionName());
        target.setStatus(source.getStatus());
        target.setDocumentCount(source.getDocumentCount());
        target.setChunkCount(source.getChunkCount());
        target.setEmbeddingCount(source.getEmbeddingCount());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
    }

    /**
     * 转换文档摘要。
     *
     * @param entity 文档实体
     * @return 文档摘要
     */
    private KnowledgeDocumentSummary toDocumentSummary(KnowledgeDocumentEntity entity) {
        Map<String, Object> metadata = parseMap(entity.getMetadata());
        KnowledgeDocumentSummary item = new KnowledgeDocumentSummary();
        item.setId(entity.getId());
        item.setKbId(entity.getKbId());
        item.setDocName(entity.getDocName());
        item.setDocType(entity.getDocType());
        item.setFileExt(entity.getFileExt());
        item.setFileSize(entity.getFileSize());
        item.setFileHash(entity.getFileHash());
        item.setParseStatus(entity.getParseStatus());
        item.setParseError(entity.getParseError());
        item.setAsyncTaskId(asString(metadata.get("asyncTaskId")));
        item.setProcessStage(asString(metadata.get("processStage")));
        item.setProcessStageLabel(asString(metadata.get("processStageLabel")));
        item.setProgressPercent(intValue(metadata.get("progressPercent"), "parsed".equals(entity.getParseStatus()) ? 100 : 0));
        item.setLastMessage(asString(metadata.get("lastMessage")));
        item.setChunkCount(count("knowledge_chunk", "document_id", entity.getId()));
        item.setEmbeddingCount(countByJoin(entity.getId()));
        item.setUploadedAt(entity.getUploadedAt());
        return item;
    }

    /**
     * 转换分片摘要。
     *
     * @param entity 分片实体
     * @return 分片摘要
     */
    private KnowledgeChunkSummary toChunkSummary(KnowledgeChunkEntity entity) {
        KnowledgeChunkSummary item = new KnowledgeChunkSummary();
        item.setId(entity.getId());
        item.setDocumentId(entity.getDocumentId());
        item.setChunkNo(entity.getChunkNo());
        item.setTitle(entity.getTitle());
        item.setContent(entity.getContent());
        item.setTokenCount(entity.getTokenCount());
        item.setStatus(entity.getStatus());
        item.setSyncStatus(findEmbeddingSyncStatus(entity.getId()));
        item.setCreatedAt(entity.getCreatedAt());
        return item;
    }

    /**
     * 转换 Agent 知识库绑定摘要。
     *
     * @param entity 绑定实体
     * @return 绑定摘要
     */
    private AgentKnowledgeBindingSummary toBindingSummary(AgentKnowledgeBindingEntity entity) {
        AgentKnowledgeBindingSummary item = new AgentKnowledgeBindingSummary();
        item.setAgentId(entity.getAgentId());
        item.setKnowledgeBaseId(entity.getKnowledgeBaseId());
        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(entity.getKnowledgeBaseId());
        item.setKbName(kb == null ? "" : kb.getKbName());
        item.setRetrievalConfig(entity.getRetrievalConfig());
        item.setEnabled(entity.getEnabled());
        item.setCreatedAt(entity.getCreatedAt());
        return item;
    }

    /**
     * 转换检索引用来源。
     *
     * @param kb 知识库
     * @param chunk 分片
     * @param score 得分
     * @return 引用来源
     */
    private KnowledgeSource toSource(KnowledgeBaseEntity kb, KnowledgeChunkEntity chunk, double score) {
        KnowledgeDocumentEntity document = knowledgeDocumentMapper.selectById(chunk.getDocumentId());
        KnowledgeSource source = new KnowledgeSource();
        source.setKbId(kb.getId());
        source.setKbName(kb.getKbName());
        source.setDocumentId(chunk.getDocumentId());
        source.setDocumentName(document == null ? "" : document.getDocName());
        source.setChunkId(chunk.getId());
        source.setChunkNo(chunk.getChunkNo());
        source.setQuoteText(chunk.getContent());
        source.setScore(score);
        source.setPageNo(chunk.getPageNo());
        return source;
    }

    /**
     * 查询知识库实体。
     *
     * @param id 知识库 ID
     * @return 知识库实体
     */
    private KnowledgeBaseEntity requireKnowledgeBase(String id) {
        KnowledgeBaseEntity entity = knowledgeBaseMapper.selectById(id);
        if (entity == null || entity.getDeletedAt() != null) {
            throw new BusinessException("KNOWLEDGE_BASE_NOT_FOUND", "知识库不存在");
        }
        return entity;
    }

    /**
     * 查询 Agent 实体。
     *
     * @param id Agent ID
     * @return Agent 实体
     */
    private AgentEntity requireAgent(String id) {
        AgentEntity entity = agentMapper.selectById(id);
        if (entity == null || entity.getDeletedAt() != null) {
            throw new BusinessException("AGENT_NOT_FOUND", "Agent 不存在");
        }
        return entity;
    }

    /**
     * 判断当前用户是否可查看知识库。
     *
     * @param entity 知识库实体
     * @return 是否可查看
     */
    private boolean canView(KnowledgeBaseEntity entity) {
        if (entity == null || entity.getDeletedAt() != null) {
            return false;
        }
        return workspaceGovernanceService.canViewResource(
                "knowledge_base",
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getOwnerUserId(),
                entity.getCreatedBy(),
                entity.getVisibility());
    }

    /**
     * 校验知识库查看权限。
     *
     * @param entity 知识库实体
     */
    private void assertCanView(KnowledgeBaseEntity entity) {
        if (!canView(entity)) {
            throw new BusinessException("KNOWLEDGE_FORBIDDEN", "没有访问该知识库的权限");
        }
    }

    /**
     * 校验知识库管理权限。
     *
     * @param entity 知识库实体
     */
    private void assertCanManage(KnowledgeBaseEntity entity) {
        if (!workspaceGovernanceService.canManageResource(entity.getWorkspaceId(), entity.getOwnerUserId(), entity.getCreatedBy())) {
            throw new BusinessException("KNOWLEDGE_FORBIDDEN", "没有管理该知识库的权限");
        }
    }

    /**
     * 计算余弦相似度。
     *
     * @param left 左向量
     * @param right 右向量
     * @return 相似度得分
     */
    private double cosine(List<Double> left, List<Double> right) {
        int size = Math.min(left.size(), right.size());
        double dot = 0D;
        double leftNorm = 0D;
        double rightNorm = 0D;
        for (int index = 0; index < size; index++) {
            double l = left.get(index);
            double r = right.get(index);
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }
        if (leftNorm == 0D || rightNorm == 0D) {
            return 0D;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    /**
     * 解析向量 JSON。
     *
     * @param json 向量 JSON
     * @return 向量值
     */
    private List<Double> parseVector(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return List.of();
        }
    }

    /**
     * 解析 JSON 对象。
     *
     * @param json JSON 字符串
     * @return Map 对象
     */
    private Map<String, Object> parseMap(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return Map.of();
            }
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return Map.of();
        }
    }

    /**
     * 保存上传文件到本地 data 目录。
     *
     * @param kbId 知识库 ID
     * @param documentId 文档 ID
     * @param fileName 文件名
     * @param bytes 文件字节
     * @return 存储相对路径
     */
    private String saveUploadFile(String kbId, String documentId, String fileName, byte[] bytes) throws Exception {
        Path folder = Path.of("data", "uploads", "knowledge", kbId);
        Files.createDirectories(folder);
        String safeName = fileName.replaceAll("[\\\\/:*?\"<>|]+", "_");
        Path target = folder.resolve(documentId + "_" + safeName);
        Files.write(target, bytes);
        return target.toString().replace('\\', '/');
    }

    /**
     * 提取文件扩展名。
     *
     * @param fileName 文件名
     * @return 扩展名
     */
    private String fileExt(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 ? "txt" : fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 查询字段计数。
     *
     * @param table 表名
     * @param column 字段名
     * @param value 字段值
     * @return 数量
     */
    private Integer count(String table, String column, String value) {
        Number count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM " + table + " WHERE " + column + " = ?", Number.class, value);
        return count == null ? 0 : count.intValue();
    }

    /**
     * 查询文档对应向量数量。
     *
     * @param documentId 文档 ID
     * @return 向量数量
     */
    private Integer countByJoin(String documentId) {
        Number count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM knowledge_embedding e
                JOIN knowledge_chunk c ON c.id = e.chunk_id
                WHERE c.document_id = ?
                """, Number.class, documentId);
        return count == null ? 0 : count.intValue();
    }

    /**
     * 查询分片向量同步状态。
     *
     * @param chunkId 分片 ID
     * @return 同步状态
     */
    private String findEmbeddingSyncStatus(String chunkId) {
        KnowledgeEmbeddingEntity embedding = knowledgeEmbeddingMapper.selectOne(new LambdaQueryWrapper<KnowledgeEmbeddingEntity>()
                .eq(KnowledgeEmbeddingEntity::getChunkId, chunkId)
                .last("limit 1"));
        return embedding == null ? "pending" : embedding.getSyncStatus();
    }

    /**
     * 查询模型展示名称。
     *
     * @param modelId 模型 ID
     * @return 模型名称
     */
    private String findModelName(String modelId) {
        if (!StringUtils.hasText(modelId)) {
            return "";
        }
        ModelConfigEntity model = modelConfigMapper.selectById(modelId);
        return model == null ? "" : model.getModelName();
    }

    /**
     * 查询工作空间展示名称。
     *
     * @param workspaceId 工作空间 ID
     * @return 工作空间名称
     */
    private String findWorkspaceName(String workspaceId) {
        if (!StringUtils.hasText(workspaceId)) {
            return "";
        }
        List<String> names = jdbcTemplate.queryForList(
                "SELECT workspace_name FROM oaf_workspace WHERE id = ? LIMIT 1",
                String.class,
                workspaceId);
        return names.isEmpty() ? "" : names.get(0);
    }

    /**
     * 生成唯一知识库编码。
     *
     * @param baseCode 基础编码
     * @return 唯一编码
     */
    private String uniqueKbCode(String baseCode) {
        String normalized = StringUtils.hasText(baseCode) ? baseCode : "kb";
        String candidate = normalized;
        int suffix = 1;
        while (knowledgeBaseMapper.selectCount(new LambdaQueryWrapper<KnowledgeBaseEntity>()
                .eq(KnowledgeBaseEntity::getKbCode, candidate)) > 0) {
            candidate = normalized + "-" + suffix++;
        }
        return candidate;
    }

    /**
     * 将名称转换为保守编码。
     *
     * @param text 名称文本
     * @return 编码文本
     */
    private String slugify(String text) {
        String cleaned = text == null ? "kb" : text.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("^-|-$", "");
        return StringUtils.hasText(cleaned) ? cleaned : "kb";
    }

    /**
     * 判断当前用户是否系统管理员。
     *
     * @return 是否系统管理员
     */
    private boolean isSystemManager() {
        return hasAuthority("ROLE_super_admin") || hasAuthority("ROLE_admin") || hasAuthority("knowledge:manage");
    }

    /**
     * 判断当前用户是否拥有指定权限。
     *
     * @param authority 权限标识
     * @return 是否拥有
     */
    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    /**
     * 获取当前用户 ID。
     *
     * @return 当前用户 ID
     */
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails userDetails) {
            return userDetails.getUser().getId();
        }
        return null;
    }

    /**
     * 获取当前用户 ID，未登录时抛出异常。
     *
     * @return 当前用户 ID
     */
    private String currentUserIdOrThrow() {
        String userId = currentUserId();
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException("UNAUTHORIZED", "请先登录");
        }
        return userId;
    }

    /**
     * 读取整数配置值。
     *
     * @param value 配置值
     * @param fallback 默认值
     * @return 整数值
     */
    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    /**
     * 读取小数配置值。
     *
     * @param value 配置值
     * @param fallback 默认值
     * @return 小数值
     */
    private double doubleValue(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return fallback;
    }

    /**
     * 安全读取字符串值。
     *
     * @param value 原始值
     * @return 字符串值
     */
    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 生成 UUID 主键。
     *
     * @return UUID 字符串
     */
    private String newId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 转换 JSON 字符串。
     *
     * @param value 任意对象
     * @return JSON 字符串
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }
}
