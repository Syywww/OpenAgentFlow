package com.openagentflow.service;

import com.openagentflow.config.OpenAgentFlowProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Kafka 异步任务 Topic 路由器。
 */
@Service("asyncTaskTopicRouter")
public class AsyncTaskTopicRouter {

    /** Worker 分类。 */
    public static final List<String> CATEGORIES = List.of("document", "evaluation", "integration", "maintenance");

    /** 异步任务配置。 */
    private final OpenAgentFlowProperties.AsyncTask properties;

    public AsyncTaskTopicRouter(OpenAgentFlowProperties openAgentFlowProperties) {
        this.properties = openAgentFlowProperties.getAsyncTask();
    }

    /**
     * 根据任务类型返回主 Topic。
     */
    public String primaryTopic(String taskType) {
        return categoryTopic(properties.getTopic(), categoryOf(taskType));
    }

    /**
     * 根据任务类型和重试次数返回重试 Topic。
     */
    public String retryTopic(String taskType, int attempt) {
        String base = attempt <= 1 ? properties.getRetryTopic5s() : properties.getRetryTopic30s();
        return categoryTopic(base, categoryOf(taskType));
    }

    /**
     * 返回当前 Worker 角色应订阅的 Topic。
     * <p>all 角色额外订阅旧版通用 Topic，保证升级期间已有消息可以继续消费。</p>
     */
    public String[] consumerTopics() {
        String role = normalizeRole(properties.getWorkerRole());
        Set<String> topics = new LinkedHashSet<>();
        List<String> categories = "all".equals(role) ? CATEGORIES : List.of(role);
        for (String category : categories) {
            topics.add(categoryTopic(properties.getTopic(), category));
            topics.add(categoryTopic(properties.getRetryTopic5s(), category));
            topics.add(categoryTopic(properties.getRetryTopic30s(), category));
        }
        if ("all".equals(role)) {
            topics.add(properties.getTopic());
            topics.add(properties.getRetryTopic5s());
            topics.add(properties.getRetryTopic30s());
        }
        return topics.toArray(String[]::new);
    }

    /**
     * 返回需要自动创建的全部 Topic。
     */
    public List<TopicDefinition> topicDefinitions() {
        List<TopicDefinition> topics = new ArrayList<>();
        for (String category : CATEGORIES) {
            topics.add(new TopicDefinition(categoryTopic(properties.getTopic(), category), "604800000"));
            topics.add(new TopicDefinition(categoryTopic(properties.getRetryTopic5s(), category), "604800000"));
            topics.add(new TopicDefinition(categoryTopic(properties.getRetryTopic30s(), category), "604800000"));
        }
        // 保留旧 Topic 便于滚动升级，所有新任务已经路由到分类 Topic。
        topics.add(new TopicDefinition(properties.getTopic(), "604800000"));
        topics.add(new TopicDefinition(properties.getRetryTopic5s(), "604800000"));
        topics.add(new TopicDefinition(properties.getRetryTopic30s(), "604800000"));
        topics.add(new TopicDefinition(properties.getDeadLetterTopic(), "2592000000"));
        return topics;
    }

    /**
     * 返回任务所属 Worker 分类。
     */
    public String categoryOf(String taskType) {
        if ((taskType != null && taskType.startsWith("DOCUMENT_"))
                || "KNOWLEDGE_VECTOR_REBUILD".equals(taskType)
                || "KNOWLEDGE_INDEX_BUILD".equals(taskType)) {
            return "document";
        }
        if ("EVALUATION_RUN".equals(taskType)) {
            return "evaluation";
        }
        if ("MCP_DISCOVERY".equals(taskType) || "TEMPLATE_INSTALL".equals(taskType) || "TEMPLATE_UPGRADE".equals(taskType)) {
            return "integration";
        }
        return "maintenance";
    }

    private String categoryTopic(String base, String category) {
        return base + "." + category;
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "all";
        }
        String normalized = role.trim().toLowerCase();
        return CATEGORIES.contains(normalized) ? normalized : "all";
    }

    /**
     * Topic 名称及保留时间定义。
     */
    public record TopicDefinition(String name, String retentionMs) {
    }
}
