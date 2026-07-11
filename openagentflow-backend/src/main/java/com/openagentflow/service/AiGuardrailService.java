package com.openagentflow.service;

import com.openagentflow.domain.chat.ChatCompletionRequest;
import com.openagentflow.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.text.Normalizer;
import java.net.InetAddress;
import java.net.URI;

/** AI输入、输出和工具参数安全护栏服务。 */
@Service
public class AiGuardrailService {

    /** 常见提示词注入表达式。 */
    private static final Pattern PROMPT_INJECTION = Pattern.compile(
            "(?i)(忽略.{0,12}(之前|以上).{0,12}(指令|规则)|泄露.{0,12}(系统提示词|system prompt)|ignore.{0,20}previous.{0,20}instructions|reveal.{0,20}system.{0,20}prompt)");

    /** API Key和Bearer令牌表达式。 */
    private static final Pattern SECRET = Pattern.compile("(?i)(sk-[a-z0-9_-]{12,}|ark-[a-z0-9_-]{12,}|bearer\\s+[a-z0-9._-]{16,})");

    /** 身份证和手机号表达式。 */
    private static final Pattern PII = Pattern.compile("(?<!\\d)(1[3-9]\\d{9}|[1-9]\\d{5}(?:19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx])(?!\\d)");

    /** JDBC工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** JSON工具。 */
    private final ObjectMapper objectMapper;

    /** 允许HTTP工具访问的私网主机白名单。 */
    private final List<String> privateNetworkAllowlist;

    public AiGuardrailService(JdbcTemplate jdbcTemplate,
                              ObjectMapper objectMapper,
                              @Value("${openagentflow.security.tool-private-network-allowlist:}") String privateNetworkAllowlist) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.privateNetworkAllowlist = List.of(privateNetworkAllowlist.split(",")).stream()
                .map(String::trim).filter(StringUtils::hasText).map(value -> value.toLowerCase(Locale.ROOT)).toList();
    }

    /** 检查用户输入，高置信Prompt注入直接阻断。 */
    public void inspectInput(ChatCompletionRequest request) {
        String input = request == null ? "" : request.getInput();
        String normalized = normalize(input);
        if (StringUtils.hasText(input) && (PROMPT_INJECTION.matcher(normalized).find() || matchesDynamicRule("input", normalized))) {
            saveEvent(null, request.getAgentId(), "input", "prompt_injection", "block", 0.95D, input);
            throw new BusinessException("AI_GUARDRAIL_BLOCKED", "输入疑似包含Prompt注入指令，已被安全护栏阻断");
        }
    }

    /** 检查RAG召回内容，阻断知识库投毒形成的间接提示词注入。 */
    public void inspectRetrievedContent(String workspaceId, String runId, String agentId, List<com.openagentflow.domain.knowledge.KnowledgeSource> sources) {
        if (sources == null) return;
        for (com.openagentflow.domain.knowledge.KnowledgeSource source : sources) {
            String text = normalize(source.getQuoteText());
            if (PROMPT_INJECTION.matcher(text).find() || matchesDynamicRule("retrieval", text)) {
                saveEvent(runId, agentId, "retrieval", "indirect_prompt_injection", "block", 0.95D, text, workspaceId);
                throw new BusinessException("RAG_CONTENT_GUARDRAIL_BLOCKED", "召回内容疑似包含间接提示词注入，已停止本次回答");
            }
        }
    }

    /** 校验HTTP工具目标，阻止访问回环、链路本地和私网地址。 */
    public void assertSafeHttpTarget(String url) {
        try {
            URI uri = URI.create(url);
            if (!List.of("http", "https").contains(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
                throw new BusinessException("TOOL_URL_FORBIDDEN", "工具URL只允许HTTP或HTTPS地址");
            }
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            if (privateNetworkAllowlist.stream().anyMatch(allowed -> host.equals(allowed) || host.endsWith("." + allowed))) {
                return;
            }
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                    throw new BusinessException("TOOL_SSRF_BLOCKED", "工具URL解析到受保护的内网地址");
                }
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("TOOL_URL_INVALID", "工具URL解析失败：" + exception.getMessage());
        }
    }

    /** 从数据库动态加载启用规则并匹配正则或关键词。 */
    private boolean matchesDynamicRule(String stage, String content) {
        List<Map<String, Object>> rules = jdbcTemplate.queryForList("""
                SELECT r.rule_expr,r.keywords,p.apply_scope FROM guardrail_rule r
                JOIN guardrail_policy p ON p.id=r.policy_id
                WHERE r.enabled=1 AND p.enabled=1 AND (p.apply_scope IS NULL OR p.apply_scope IN ('all',?))
                """, stage);
        for (Map<String, Object> rule : rules) {
            String expression = String.valueOf(rule.getOrDefault("rule_expr", ""));
            String keywords = String.valueOf(rule.getOrDefault("keywords", ""));
            try {
                if (StringUtils.hasText(expression) && Pattern.compile(expression, Pattern.CASE_INSENSITIVE).matcher(content).find()) return true;
            } catch (Exception ignored) {
                // 非法正则不影响主链路，治理页面可根据策略命中率发现并修正配置。
            }
            if (StringUtils.hasText(keywords) && List.of(keywords.split("[,，|]")).stream()
                    .map(String::trim).filter(StringUtils::hasText).anyMatch(content::contains)) return true;
        }
        return false;
    }

    /** Unicode标准化并移除常见混淆分隔符。 */
    private String normalize(String content) {
        if (content == null) return "";
        return Normalizer.normalize(content, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Cf}\\p{Cc}]", "").replaceAll("[\\s_\\-]{2,}", " ");
    }

    /** 对模型输出进行密钥和个人敏感信息脱敏。 */
    public String sanitizeOutput(String workspaceId, String runId, String agentId, String content) {
        if (!StringUtils.hasText(content)) return content;
        String sanitized = SECRET.matcher(content).replaceAll("[敏感密钥已隐藏]");
        sanitized = PII.matcher(sanitized).replaceAll("[个人信息已隐藏]");
        if (!sanitized.equals(content)) {
            saveEvent(runId, agentId, "output", "sensitive_data", "redact", 0.9D, content, workspaceId);
        }
        return sanitized;
    }

    /** 判断工具动作是否要求二次确认。 */
    public boolean requiresToolConfirmation(String toolCode, Map<String, Object> arguments) {
        String text = (toolCode + " " + String.valueOf(arguments)).toLowerCase(Locale.ROOT);
        boolean dangerous = List.of("delete", "drop", "truncate", "exec", "shutdown", "删除", "执行命令").stream().anyMatch(text::contains);
        if (dangerous) saveEvent(null, null, "tool", "dangerous_action", "confirm", 0.9D, text);
        return dangerous;
    }

    private void saveEvent(String runId, String agentId, String stage, String risk, String action, double score, String content) {
        saveEvent(runId, agentId, stage, risk, action, score, content, null);
    }

    private void saveEvent(String runId, String agentId, String stage, String risk, String action, double score, String content, String workspaceId) {
        String hash = DigestUtils.md5DigestAsHex((content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
        jdbcTemplate.update("""
                INSERT INTO ai_guardrail_event
                  (id,workspace_id,run_id,agent_id,guard_stage,risk_type,action_type,risk_score,content_hash,detail_json,created_at)
                VALUES (?,?,?,?,?,?,?,?,?,JSON_OBJECT('contentLength',?),NOW(3))
                """, UUID.randomUUID().toString(), workspaceId, runId, agentId, stage, risk, action, score, hash,
                content == null ? 0 : content.length());
    }
}
