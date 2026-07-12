package com.openagentflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.chat.ChatMessage;
import com.openagentflow.domain.chat.ChatRunContext;
import com.openagentflow.domain.chat.LlmCallResult;
import com.openagentflow.domain.model.ModelRouteDecision;
import com.openagentflow.entity.AgentEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Memory结构化事实提取服务。
 *
 * <p>只从用户明确陈述中抽取可复用事实，不把助手生成内容当成事实来源。</p>
 */
@Service
public class MemoryExtractionService {

    /** 手机号和身份证等个人敏感信息。 */
    private static final Pattern PII = Pattern.compile("(?<!\\d)(1[3-9]\\d{9}|[1-9]\\d{5}(?:19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx])(?!\\d)");

    /** 单条结构化记忆候选。 */
    public record Candidate(String category, String factKey, String text,
                            double importance, double confidence, double sourceReliability) { }

    private final ModelGatewayService modelGatewayService;
    private final OpenAiCompatibleClient client;
    private final ObjectMapper objectMapper;
    private final AiGuardrailService guardrailService;

    public MemoryExtractionService(ModelGatewayService modelGatewayService,
                                   OpenAiCompatibleClient client,
                                   ObjectMapper objectMapper,
                                   AiGuardrailService guardrailService) {
        this.modelGatewayService = modelGatewayService;
        this.client = client;
        this.objectMapper = objectMapper;
        this.guardrailService = guardrailService;
    }

    /**
     * 使用LLM提取用户事实，失败时执行保守规则降级。
     */
    public List<Candidate> extract(AgentEntity agent, String workspaceId, String runId, String userInput, String piiMode) {
        if (!valuableInput(userInput)) return List.of();
        try {
            ModelRouteDecision route = modelGatewayService.resolveAgentChatRoute(null, agent);
            ChatRunContext context = new ChatRunContext();
            context.setRunId(runId);
            context.setAgent(agent);
            context.setModel(route.getModel());
            context.setProvider(route.getProvider());
            context.setApiKey(route.getApiKey());
            context.setMessages(List.of(
                    new ChatMessage("system", extractionPrompt()),
                    new ChatMessage("user", userInput)));
            LlmCallResult result = client.complete(context, 0D, 600);
            return parse(workspaceId, runId, agent.getId(), result.getContent(), piiMode);
        } catch (Exception ignored) {
            // 模型不可用时只接受带有明确自述信号的内容，避免把普通问句误写成长期事实。
            return conservativeFallback(workspaceId, runId, agent.getId(), userInput, piiMode);
        }
    }

    private String extractionPrompt() {
        return """
                你是企业级Memory事实提取器。只能提取用户在本轮明确陈述、未来对话仍有价值的事实、偏好、身份、约束或长期任务。
                禁止根据助手回答推断事实，禁止保存寒暄、问题本身、临时请求、密码、令牌、身份证或手机号。
                只返回JSON数组，不要Markdown。每项格式：
                {"category":"preference|profile|constraint|task|business_fact","factKey":"稳定英文键","text":"简洁中文事实","importance":0.0,"confidence":0.0,"sourceReliability":0.0}
                没有值得保存的内容时返回[]。同一事实只输出一次。
                """;
    }

    private List<Candidate> parse(String workspaceId, String runId, String agentId, String content, String piiMode) throws Exception {
        String json = content == null ? "[]" : content.replace("```json", "").replace("```", "").trim();
        JsonNode root = objectMapper.readTree(json);
        if (!root.isArray()) return List.of();
        List<Candidate> result = new ArrayList<>();
        for (JsonNode node : root) {
            String text = applyPiiPolicy(workspaceId, runId, agentId, node.path("text").asText(""), piiMode);
            String factKey = normalizeKey(node.path("factKey").asText("general_fact"));
            if (!StringUtils.hasText(text) || text.length() > 1000) continue;
            result.add(new Candidate(node.path("category").asText("business_fact"), factKey, text,
                    clamp(node.path("importance").asDouble(0.6)), clamp(node.path("confidence").asDouble(0.7)),
                    clamp(node.path("sourceReliability").asDouble(0.8))));
        }
        return result.stream().limit(8).toList();
    }

    private List<Candidate> conservativeFallback(String workspaceId, String runId, String agentId, String input, String piiMode) {
        String normalized = input.trim();
        List<String> signals = List.of("我喜欢", "我不喜欢", "我偏好", "请记住", "以后请", "我的习惯", "我的职位", "我负责");
        if (signals.stream().noneMatch(normalized::contains)) return List.of();
        String text = applyPiiPolicy(workspaceId, runId, agentId, normalized, piiMode);
        if (!StringUtils.hasText(text)) return List.of();
        return List.of(new Candidate("preference", "user_preference", text, 0.65D, 0.65D, 0.75D));
    }

    private boolean valuableInput(String input) {
        if (!StringUtils.hasText(input) || input.trim().length() < 6 || input.trim().endsWith("?") || input.trim().endsWith("？")) return false;
        String value = input.trim().toLowerCase(Locale.ROOT);
        return !List.of("你好", "您好", "谢谢", "再见", "在吗", "hello", "hi").contains(value);
    }

    private String sanitize(String workspaceId, String runId, String agentId, String value) {
        return guardrailService.sanitizeOutput(workspaceId, runId, agentId, value).trim();
    }

    private String applyPiiPolicy(String workspaceId, String runId, String agentId, String value, String piiMode) {
        if ("reject".equalsIgnoreCase(piiMode) && PII.matcher(value).find()) return "";
        if ("allow".equalsIgnoreCase(piiMode)) return value.trim();
        return sanitize(workspaceId, runId, agentId, value);
    }

    private String normalizeKey(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        return normalized.length() > 200 ? normalized.substring(0, 200) : normalized;
    }

    private double clamp(double value) { return Math.max(0D, Math.min(1D, value)); }
}
