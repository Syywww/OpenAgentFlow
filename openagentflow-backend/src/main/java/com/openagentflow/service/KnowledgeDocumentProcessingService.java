package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.knowledge.KnowledgeDocumentSummary;
import com.openagentflow.domain.knowledge.KnowledgeUploadResult;
import com.openagentflow.entity.AsyncTaskEntity;
import com.openagentflow.entity.KnowledgeBaseEntity;
import com.openagentflow.entity.KnowledgeChunkEntity;
import com.openagentflow.entity.KnowledgeDocumentEntity;
import com.openagentflow.entity.KnowledgeEmbeddingEntity;
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.KnowledgeBaseMapper;
import com.openagentflow.mapper.KnowledgeChunkMapper;
import com.openagentflow.mapper.KnowledgeDocumentMapper;
import com.openagentflow.mapper.KnowledgeEmbeddingMapper;
import com.openagentflow.security.AuthUserDetails;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * 知识库文档上传与后台处理服务。
 */
@Service
public class KnowledgeDocumentProcessingService {

    /** 日志对象，用于输出处理进度和模型调用结果。 */
    private static final Logger log = LoggerFactory.getLogger(KnowledgeDocumentProcessingService.class);

    /** 默认向量集合 ID。 */
    private static final String DEFAULT_VECTOR_COLLECTION_ID = "70000000-0000-0000-0000-000000000101";

    /** 知识库 Mapper。 */
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    /** 文档 Mapper。 */
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    /** 分片 Mapper。 */
    private final KnowledgeChunkMapper knowledgeChunkMapper;

    /** 向量 Mapper。 */
    private final KnowledgeEmbeddingMapper knowledgeEmbeddingMapper;

    /** 文档解析服务。 */
    private final DocumentParseService documentParseService;

    /** 文档切片服务。 */
    private final KnowledgeChunkingService chunkingService;

    /** Embedding 服务。 */
    private final EmbeddingService embeddingService;

    /** Milvus 写入服务。 */
    private final MilvusKnowledgeVectorService milvusKnowledgeVectorService;

    /** JDBC 工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** JSON 工具。 */
    private final ObjectMapper objectMapper;

    /** 异步任务中心服务。 */
    private final AsyncTaskService asyncTaskService;

    /** 平台异步任务线程池。 */
    private final Executor asyncTaskExecutor;

    public KnowledgeDocumentProcessingService(KnowledgeBaseMapper knowledgeBaseMapper,
                                              KnowledgeDocumentMapper knowledgeDocumentMapper,
                                              KnowledgeChunkMapper knowledgeChunkMapper,
                                              KnowledgeEmbeddingMapper knowledgeEmbeddingMapper,
                                              DocumentParseService documentParseService,
                                              KnowledgeChunkingService chunkingService,
                                              EmbeddingService embeddingService,
                                              MilvusKnowledgeVectorService milvusKnowledgeVectorService,
                                              JdbcTemplate jdbcTemplate,
                                              ObjectMapper objectMapper,
                                              AsyncTaskService asyncTaskService,
                                              @Qualifier("oafAsyncTaskExecutor") Executor asyncTaskExecutor) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.knowledgeEmbeddingMapper = knowledgeEmbeddingMapper;
        this.documentParseService = documentParseService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.milvusKnowledgeVectorService = milvusKnowledgeVectorService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.asyncTaskService = asyncTaskService;
        this.asyncTaskExecutor = asyncTaskExecutor;
    }

    /**
     * 接收上传文件并启动后台处理。
     *
     * @param kbId 知识库 ID
     * @param file 上传文件
     * @return 上传受理结果
     */
    public KnowledgeUploadResult acceptUpload(String kbId, MultipartFile file) {
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
            KnowledgeDocumentEntity duplicate = findParsedDuplicateDocument(kbId, fileHash);
            if (duplicate != null) {
                KnowledgeUploadResult result = new KnowledgeUploadResult();
                result.setDocument(toSummary(duplicate));
                result.setChunkCount(count("knowledge_chunk", "document_id", duplicate.getId()));
                result.setEmbeddingCount(countByDocument(duplicate.getId()));
                result.setMilvusSynced(true);
                result.setAsyncAccepted(false);
                result.setMessage("检测到相同文件已解析，已复用已有分片和向量结果");
                return result;
            }
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
            document.setMetadata(toJson(progressMetadata("accepted", "已接收文件", 5, "文件已上传，等待后台解析")));
            document.setUploadedBy(currentUserId());
            knowledgeDocumentMapper.insert(document);

            AsyncTaskEntity asyncTask = asyncTaskService.createTask(
                    "处理知识文档：" + fileName,
                    "DOCUMENT_PROCESS",
                    "knowledge_document",
                    documentId,
                    "knowledge_document",
                    documentId,
                    kb.getWorkspaceId(),
                    Map.of("kbId", kbId, "documentId", documentId, "fileName", fileName, "fileSize", file.getSize(), "fileExt", fileExt)
            );
            Map<String, Object> metadata = parseMetadata(document.getMetadata());
            metadata.put("asyncTaskId", asyncTask.getId());
            document.setMetadata(toJson(metadata));
            knowledgeDocumentMapper.updateById(document);

            // 后台处理不阻塞上传请求，前端通过任务中心或文档状态接口轮询进度。
            asyncTaskExecutor.execute(() -> processDocument(asyncTask.getId(), kbId, documentId, bytes, fileExt));
            log.info("知识库文档已接收，进入异步任务中心：kbId={}, documentId={}, taskId={}, fileName={}", kbId, documentId, asyncTask.getId(), fileName);

            KnowledgeUploadResult result = new KnowledgeUploadResult();
            result.setDocument(toSummary(document));
            result.setChunkCount(0);
            result.setEmbeddingCount(0);
            result.setMilvusSynced(false);
            result.setAsyncAccepted(true);
            result.setAsyncTaskId(asyncTask.getId());
            result.setMessage("文件已上传，后台正在解析、切片、调用 Embedding 模型并写入 Milvus");
            return result;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("DOCUMENT_UPLOAD_FAILED", exception.getMessage());
        }
    }

    /**
     * 查询单个文档处理状态。
     *
     * @param kbId 知识库 ID
     * @param documentId 文档 ID
     * @return 文档摘要和处理日志
     */
    public KnowledgeDocumentSummary getDocumentStatus(String kbId, String documentId) {
        KnowledgeBaseEntity kb = requireKnowledgeBase(kbId);
        assertCanView(kb);
        KnowledgeDocumentEntity document = knowledgeDocumentMapper.selectById(documentId);
        if (document == null || !kbId.equals(document.getKbId())) {
            throw new BusinessException("DOCUMENT_NOT_FOUND", "文档不存在");
        }
        return toSummary(document);
    }

    /**
     * 后台处理文档。
     *
     * @param kbId 知识库 ID
     * @param documentId 文档 ID
     * @param bytes 文件字节
     * @param fileExt 文件扩展名
     */
    private void processDocument(String taskId, String kbId, String documentId, byte[] bytes, String fileExt) {
        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(kbId);
        KnowledgeDocumentEntity document = knowledgeDocumentMapper.selectById(documentId);
        if (kb == null || document == null) {
            log.warn("知识库文档后台处理跳过，记录不存在：kbId={}, documentId={}", kbId, documentId);
            return;
        }
        try {
            asyncTaskService.markRunning(taskId);
            checkCanceled(taskId);
            updateProgress(taskId, documentId, "parsing", "解析文档", 15, "开始解析文档内容", null);
            String text = documentParseService.parse(bytes, fileExt);
            checkCanceled(taskId);
            updateProgress(taskId, documentId, "chunking", "文档切片", 30, "文档解析完成，开始切片", Map.of("textLength", text.length()));

            List<KnowledgeChunkingService.ChunkSegment> segments = chunkingService.splitSegments(text, kb.getChunkStrategy(), kb.getChunkSize(), kb.getChunkOverlap());
            List<KnowledgeChunkingService.ChunkSegment> embeddingSegments = segments.stream()
                    .filter(KnowledgeChunkingService.ChunkSegment::embeddingEnabled)
                    .toList();
            if (embeddingSegments.isEmpty()) {
                throw new BusinessException("DOCUMENT_CHUNK_EMPTY", "文档没有生成有效分片");
            }
            checkCanceled(taskId);
            updateProgress(taskId, documentId, "embedding", "调用向量模型", 45, "已生成 " + segments.size() + " 个分片，其中 " + embeddingSegments.size() + " 个子分片需要向量化", Map.of("chunkCount", segments.size(), "embeddingChunkCount", embeddingSegments.size()));

            ModelConfigEntity embeddingModel = embeddingService.resolveEmbeddingModel(kb.getEmbeddingModelId());
            if (!StringUtils.hasText(kb.getEmbeddingModelId())) {
                kb.setEmbeddingModelId(embeddingModel.getId());
                knowledgeBaseMapper.updateById(kb);
            }

            Instant embeddingStartedAt = Instant.now();
            EmbeddingBatchResult embeddingResult = embeddingService.embedWithTrace(embeddingModel, embeddingSegments.stream().map(KnowledgeChunkingService.ChunkSegment::content).toList());
            List<List<Double>> vectors = embeddingResult.getVectors();
            String embeddingMessage = Boolean.TRUE.equals(embeddingResult.getFallbackUsed())
                    ? "真实 Embedding 调用失败，已使用本地兜底向量：" + embeddingResult.getErrorMessage()
                    : "真实 Embedding 调用成功，接口 " + embeddingResult.getEmbeddingApi() + "，向量维度 " + embeddingResult.getDimension();
            checkCanceled(taskId);
            updateProgress(taskId, documentId, "embedding_done", "向量生成完成", 65, embeddingMessage, Map.of(
                    "embeddingApi", safeValue(embeddingResult.getEmbeddingApi()),
                    "embeddingModelCode", safeValue(embeddingResult.getModelCode()),
                    "embeddingModelName", safeValue(embeddingResult.getModelName()),
                    "embeddingDimension", embeddingResult.getDimension(),
                    "embeddingFallbackUsed", Boolean.TRUE.equals(embeddingResult.getFallbackUsed()),
                    "embeddingLatencyMs", Duration.between(embeddingStartedAt, Instant.now()).toMillis()
            ));
            log.info("知识库文档 Embedding 完成：documentId={}, modelCode={}, api={}, dimension={}, fallback={}",
                    documentId, embeddingResult.getModelCode(), embeddingResult.getEmbeddingApi(), embeddingResult.getDimension(), embeddingResult.getFallbackUsed());

            checkCanceled(taskId);
            updateProgress(taskId, documentId, "saving", "保存分片", 70,
                    "开始保存 " + segments.size() + " 个分片和 " + embeddingSegments.size() + " 条向量记录到 MySQL", Map.of("milvusSynced", false));
            List<KnowledgeChunkEntity> savedChunks = new ArrayList<>(embeddingSegments.size());
            List<KnowledgeEmbeddingEntity> savedEmbeddings = new ArrayList<>(embeddingSegments.size());
            Map<Integer, String> parentChunkIds = new LinkedHashMap<>();
            int vectorIndex = 0;
            int chunkNo = 1;
            for (KnowledgeChunkingService.ChunkSegment segment : segments) {
                // 先把分片与向量落入 MySQL，Milvus 异常时仍可通过 MySQL 向量兜底检索。
                String parentChunkId = segment.parentOrdinal() == null ? null : parentChunkIds.get(segment.parentOrdinal());
                KnowledgeChunkEntity chunk = saveChunk(kb, document, segment, chunkNo++, parentChunkId);
                if ("parent".equalsIgnoreCase(segment.level())) {
                    parentChunkIds.put(segment.ordinal(), chunk.getId());
                    continue;
                }
                KnowledgeEmbeddingEntity embedding = saveEmbedding(kb, chunk, embeddingModel, vectors.get(vectorIndex++));
                savedChunks.add(chunk);
                savedEmbeddings.add(embedding);
            }

            boolean allMilvusSynced = true;
            String milvusMessage = "";
            try {
                checkCanceled(taskId);
                updateProgress(taskId, documentId, "milvus", "批量写入 Milvus", 82,
                        "开始批量写入 " + savedChunks.size() + " 个分片向量到 Milvus", Map.of("milvusSynced", false));
                milvusKnowledgeVectorService.upsertKnowledgeChunks(kb.getMilvusCollectionName(), savedEmbeddings, savedChunks, vectors);
                LocalDateTime syncedAt = LocalDateTime.now();
                for (KnowledgeEmbeddingEntity embedding : savedEmbeddings) {
                    // 批量写入成功后统一标记同步状态，避免每个分片都触发一次 Milvus flush。
                    embedding.setSyncStatus("synced");
                    embedding.setLastSyncedAt(syncedAt);
                    knowledgeEmbeddingMapper.updateById(embedding);
                }
                updateProgress(taskId, documentId, "milvus", "批量写入 Milvus", 95,
                        "已批量写入 " + savedChunks.size() + " 个分片向量到 Milvus", Map.of("milvusSynced", true));
            } catch (Exception exception) {
                allMilvusSynced = false;
                milvusMessage = exception.getMessage();
                for (KnowledgeEmbeddingEntity embedding : savedEmbeddings) {
                    // Milvus 不可用时保留 MySQL 中的 embedding_json，后续检索仍可降级使用。
                    embedding.setSyncStatus("mysql_fallback");
                    knowledgeEmbeddingMapper.updateById(embedding);
                }
                log.warn("Milvus 批量写入失败，保留 MySQL 向量兜底：documentId={}, chunkCount={}, error={}",
                        documentId, savedChunks.size(), exception.getMessage());
                updateProgress(taskId, documentId, "milvus_fallback", "Milvus 兜底", 95,
                        "Milvus 批量写入失败，已保留 MySQL 向量兜底：" + milvusMessage, Map.of("milvusSynced", false));
            }

            KnowledgeDocumentEntity latest = knowledgeDocumentMapper.selectById(documentId);
            latest.setParseStatus("parsed");
            latest.setParseError(null);
            knowledgeDocumentMapper.updateById(latest);
            createKnowledgeBaseVersionSnapshot(kb, "upload-" + System.currentTimeMillis());
            Map<String, Object> result = Map.of("milvusSynced", allMilvusSynced, "chunkCount", segments.size(), "embeddingCount", vectors.size());
            String doneMessage = allMilvusSynced ? "文档已解析、切片、真实向量化并写入 Milvus" : "文档已处理完成，Milvus 写入存在兜底：" + milvusMessage;
            updateProgress(taskId, documentId, "done", "处理完成", 100, doneMessage, result);
            asyncTaskService.markSuccess(taskId, doneMessage, result);
        } catch (Exception exception) {
            boolean canceled = "TASK_CANCELED".equals(exception.getMessage());
            KnowledgeDocumentEntity failed = knowledgeDocumentMapper.selectById(documentId);
            if (failed != null) {
                failed.setParseStatus(canceled ? "canceled" : "failed");
                failed.setParseError(canceled ? "用户已取消任务" : exception.getMessage());
                knowledgeDocumentMapper.updateById(failed);
            }
            if (canceled) {
                updateProgress(taskId, documentId, "canceled", "任务已取消", 100, "用户已取消任务，文档处理已停止", null);
                asyncTaskService.markCanceled(taskId, "任务已取消，文档处理已停止");
            } else {
                updateProgress(taskId, documentId, "failed", "处理失败", 100, "处理失败：" + exception.getMessage(), null);
                asyncTaskService.markFailed(taskId, "DOCUMENT_PROCESS_FAILED", exception.getMessage());
            }
            log.error("知识库文档处理失败：kbId={}, documentId={}", kbId, documentId, exception);
        }
    }

    /**
     * 重试文档处理任务。
     *
     * @param taskId 异步任务ID
     */
    public void retryTask(String taskId) {
        AsyncTaskEntity task = asyncTaskService.prepareRetry(taskId);
        KnowledgeDocumentEntity document = knowledgeDocumentMapper.selectById(task.getSourceId());
        if (document == null) {
            asyncTaskService.markFailed(taskId, "DOCUMENT_NOT_FOUND", "文档不存在，无法重试");
            throw new BusinessException("DOCUMENT_NOT_FOUND", "文档不存在，无法重试");
        }
        try {
            Path path = Path.of(document.getStorageKey());
            byte[] bytes = Files.readAllBytes(path);
            document.setParseStatus("processing");
            document.setParseError(null);
            knowledgeDocumentMapper.updateById(document);
            asyncTaskExecutor.execute(() -> processDocument(taskId, document.getKbId(), document.getId(), bytes, document.getFileExt()));
        } catch (Exception exception) {
            asyncTaskService.markFailed(taskId, "DOCUMENT_RETRY_FAILED", exception.getMessage());
            throw new BusinessException("DOCUMENT_RETRY_FAILED", exception.getMessage());
        }
    }

    /**
     * 更新文档处理进度。
     *
     * @param documentId 文档 ID
     * @param stage 阶段编码
     * @param stageLabel 阶段名称
     * @param progress 进度百分比
     * @param message 日志消息
     * @param extra 额外元数据
     */
    private void updateProgress(String taskId, String documentId, String stage, String stageLabel, int progress, String message, Map<String, Object> extra) {
        KnowledgeDocumentEntity document = knowledgeDocumentMapper.selectById(documentId);
        if (document == null) {
            return;
        }
        Map<String, Object> metadata = parseMetadata(document.getMetadata());
        metadata.put("processStage", stage);
        metadata.put("processStageLabel", stageLabel);
        metadata.put("progressPercent", progress);
        metadata.put("lastMessage", message);
        if (extra != null) {
            metadata.putAll(extra);
        }
        List<String> logs = readLogs(metadata);
        logs.add(DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now()) + " " + message);
        metadata.put("logs", logs);
        document.setMetadata(toJson(metadata));
        knowledgeDocumentMapper.updateById(document);
        asyncTaskService.updateProgress(taskId, stage, message, progress, extra);
        log.info("知识库文档处理进度：documentId={}, stage={}, progress={}%, message={}", documentId, stage, progress, message);
    }

    /**
     * 检查任务是否已被用户取消。
     *
     * @param taskId 异步任务ID
     */
    private void checkCanceled(String taskId) {
        if (asyncTaskService.isCancelRequested(taskId)) {
            throw new BusinessException("TASK_CANCELED", "TASK_CANCELED");
        }
    }

    /**
     * 构建初始处理进度元数据。
     *
     * @param stage 阶段编码
     * @param stageLabel 阶段名称
     * @param progress 进度百分比
     * @param message 日志消息
     * @return 元数据
     */
    private Map<String, Object> progressMetadata(String stage, String stageLabel, int progress, String message) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("processStage", stage);
        metadata.put("processStageLabel", stageLabel);
        metadata.put("progressPercent", progress);
        metadata.put("lastMessage", message);
        metadata.put("logs", new ArrayList<>(List.of(DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now()) + " " + message)));
        return metadata;
    }

    /**
     * 转换为文档摘要。
     *
     * @param entity 文档实体
     * @return 文档摘要
     */
    private KnowledgeDocumentSummary toSummary(KnowledgeDocumentEntity entity) {
        Map<String, Object> metadata = parseMetadata(entity.getMetadata());
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
        item.setProcessStage(asString(metadata.get("processStage")));
        item.setProcessStageLabel(asString(metadata.get("processStageLabel")));
        item.setProgressPercent(asInt(metadata.get("progressPercent"), "parsed".equals(entity.getParseStatus()) ? 100 : 0));
        item.setLastMessage(asString(metadata.get("lastMessage")));
        item.setAsyncTaskId(asString(metadata.get("asyncTaskId")));
        item.setEmbeddingFallbackUsed(asBoolean(metadata.get("embeddingFallbackUsed")));
        item.setEmbeddingApi(asString(metadata.get("embeddingApi")));
        item.setEmbeddingModelCode(asString(metadata.get("embeddingModelCode")));
        item.setEmbeddingModelName(asString(metadata.get("embeddingModelName")));
        item.setEmbeddingDimension(asInt(metadata.get("embeddingDimension"), null));
        item.setMilvusSynced(asBoolean(metadata.get("milvusSynced")));
        item.setProcessLogs(readLogs(metadata));
        item.setChunkCount(count("knowledge_chunk", "document_id", entity.getId()));
        item.setEmbeddingCount(countByDocument(entity.getId()));
        item.setUploadedAt(entity.getUploadedAt());
        return item;
    }

    /**
     * 保存文档分片。
     *
     * @param kb 知识库
     * @param document 文档
     * @param content 分片内容
     * @param chunkNo 分片序号
     * @return 分片实体
     */
    private KnowledgeChunkEntity saveChunk(KnowledgeBaseEntity kb, KnowledgeDocumentEntity document, String content, int chunkNo) {
        KnowledgeChunkingService.ChunkSegment segment = new KnowledgeChunkingService.ChunkSegment(
                content, "child", null, chunkNo, "", "", chunkNo, 0, content == null ? 0 : content.length(), true);
        return saveChunk(kb, document, segment, chunkNo, null);
    }

    /**
     * 保存带结构元数据的文档分片。
     *
     * @param kb 知识库
     * @param document 文档
     * @param segment 切片段落
     * @param chunkNo 全局切片序号
     * @param parentChunkId 父分片ID
     * @return 分片实体
     */
    private KnowledgeChunkEntity saveChunk(KnowledgeBaseEntity kb,
                                           KnowledgeDocumentEntity document,
                                           KnowledgeChunkingService.ChunkSegment segment,
                                           int chunkNo,
                                           String parentChunkId) {
        String content = segment.content();
        String contentHash = DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8));
        KnowledgeChunkEntity chunk = new KnowledgeChunkEntity();
        chunk.setId(newId());
        chunk.setKbId(kb.getId());
        chunk.setDocumentId(document.getId());
        chunk.setChunkNo(chunkNo);
        chunk.setParentChunkId(parentChunkId);
        chunk.setChunkLevel(segment.level());
        chunk.setTitle(StringUtils.hasText(segment.sectionTitle()) ? segment.sectionTitle() : document.getDocName() + " #" + chunkNo);
        chunk.setSectionTitle(segment.sectionTitle());
        chunk.setSectionPath(segment.sectionPath());
        chunk.setParagraphNo(segment.paragraphNo());
        chunk.setContent(content);
        chunk.setTokenCount(chunkingService.estimateTokens(content));
        chunk.setStartOffset(segment.startOffset() == null ? 0 : segment.startOffset());
        chunk.setEndOffset(segment.endOffset() == null ? content.length() : segment.endOffset());
        chunk.setStrategyVersion("rag-chunk-v2");
        chunk.setContentHash(contentHash);
        chunk.setSourceHash(document.getFileHash());
        chunk.setMetadata(toJson(Map.of(
                "chunkStrategy", safeValue(kb.getChunkStrategy()),
                "chunkLevel", safeValue(segment.level()),
                "parentChunkId", safeValue(parentChunkId),
                "sectionTitle", safeValue(segment.sectionTitle()),
                "sectionPath", safeValue(segment.sectionPath()),
                "contentHash", contentHash,
                "sourceHash", safeValue(document.getFileHash())
        )));
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
        embedding.setVectorCollectionId(StringUtils.hasText(kb.getVectorCollectionId()) ? kb.getVectorCollectionId() : DEFAULT_VECTOR_COLLECTION_ID);
        embedding.setMilvusCollectionName(kb.getMilvusCollectionName());
        embedding.setVectorPrimaryKey("chunk_" + chunk.getId().replace("-", ""));
        embedding.setSyncStatus("pending");
        embedding.setEmbeddingJson(toJson(vector));
        embedding.setEmbeddingDim(vector.size());
        embedding.setContentHash(DigestUtils.md5DigestAsHex(chunk.getContent().getBytes(StandardCharsets.UTF_8)));
        knowledgeEmbeddingMapper.insert(embedding);
        return embedding;
    }

    /**
     * 查找同一知识库下已解析的相同文件，避免重复进入后台切片和向量化。
     *
     * @param kbId 知识库 ID
     * @param fileHash 文件哈希
     * @return 已解析文档，找不到返回 null
     */
    private KnowledgeDocumentEntity findParsedDuplicateDocument(String kbId, String fileHash) {
        if (!StringUtils.hasText(fileHash)) {
            return null;
        }
        return knowledgeDocumentMapper.selectOne(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getKbId, kbId)
                .eq(KnowledgeDocumentEntity::getFileHash, fileHash)
                .eq(KnowledgeDocumentEntity::getParseStatus, "parsed")
                .orderByDesc(KnowledgeDocumentEntity::getUploadedAt)
                .last("limit 1"));
    }

    /**
     * 创建知识库版本快照，用于大量文档场景下的版本追踪和后续回滚。
     *
     * @param kb 知识库
     * @param versionNo 版本号
     */
    private void createKnowledgeBaseVersionSnapshot(KnowledgeBaseEntity kb, String versionNo) {
        try {
            int documentCount = count("knowledge_document", "kb_id", kb.getId());
            int chunkCount = count("knowledge_chunk", "kb_id", kb.getId());
            int embeddingCount = count("knowledge_embedding", "kb_id", kb.getId());
            jdbcTemplate.update("""
                    INSERT INTO knowledge_base_version
                      (id, kb_id, version_no, version_name, document_count, chunk_count, embedding_count,
                       chunk_strategy, chunk_size, chunk_overlap, snapshot_json, status, created_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), 'active', ?)
                    ON DUPLICATE KEY UPDATE
                      document_count = VALUES(document_count),
                      chunk_count = VALUES(chunk_count),
                      embedding_count = VALUES(embedding_count),
                      snapshot_json = VALUES(snapshot_json),
                      status = 'active'
                    """,
                    newId(),
                    kb.getId(),
                    versionNo,
                    "文档处理快照",
                    documentCount,
                    chunkCount,
                    embeddingCount,
                    kb.getChunkStrategy(),
                    kb.getChunkSize(),
                    kb.getChunkOverlap(),
                    toJson(Map.of("kbId", kb.getId(), "documentCount", documentCount, "chunkCount", chunkCount, "embeddingCount", embeddingCount)),
                    currentUserId());
        } catch (Exception ignored) {
            // 快照失败不能影响文档处理任务。
        }
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
        if (isSystemManager()) {
            return;
        }
        String userId = currentUserId();
        if (!StringUtils.hasText(userId) || (!userId.equals(entity.getOwnerUserId()) && !userId.equals(entity.getCreatedBy()))) {
            throw new BusinessException("KNOWLEDGE_FORBIDDEN", "没有管理该知识库的权限");
        }
    }

    /**
     * 判断当前用户是否可以查看知识库。
     *
     * @param entity 知识库实体
     * @return 是否可查看
     */
    private boolean canView(KnowledgeBaseEntity entity) {
        if (entity == null || entity.getDeletedAt() != null) {
            return false;
        }
        if ("public".equalsIgnoreCase(entity.getVisibility()) || isSystemManager()) {
            return true;
        }
        String userId = currentUserId();
        return StringUtils.hasText(userId) && (userId.equals(entity.getOwnerUserId()) || userId.equals(entity.getCreatedBy()));
    }

    /**
     * 判断当前用户是否是系统管理员。
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
    private Integer countByDocument(String documentId) {
        Number count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM knowledge_embedding e
                JOIN knowledge_chunk c ON c.id = e.chunk_id
                WHERE c.document_id = ?
                """, Number.class, documentId);
        return count == null ? 0 : count.intValue();
    }

    /**
     * 解析元数据。
     *
     * @param json JSON 字符串
     * @return 元数据 Map
     */
    private Map<String, Object> parseMetadata(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return new LinkedHashMap<>();
            }
            return new LinkedHashMap<>(objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            }));
        } catch (Exception exception) {
            return new LinkedHashMap<>();
        }
    }

    /**
     * 读取处理日志。
     *
     * @param metadata 元数据
     * @return 日志列表
     */
    private List<String> readLogs(Map<String, Object> metadata) {
        Object logs = metadata.get("logs");
        if (logs instanceof List<?> list) {
            return new ArrayList<>(list.stream().map(String::valueOf).toList());
        }
        return new ArrayList<>();
    }

    /**
     * 读取字符串值。
     *
     * @param value 原始值
     * @return 字符串
     */
    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 读取整数值。
     *
     * @param value 原始值
     * @param fallback 默认值
     * @return 整数值
     */
    private Integer asInt(Object value, Integer fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (Exception ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    /**
     * 读取布尔值。
     *
     * @param value 原始值
     * @return 布尔值
     */
    private Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * 安全字符串值，避免 Map.of 遇到 null。
     *
     * @param value 原始值
     * @return 字符串
     */
    private String safeValue(String value) {
        return value == null ? "" : value;
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
