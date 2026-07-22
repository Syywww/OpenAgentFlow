package com.openagentflow.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openagentflow.config.OpenAgentFlowProperties;
import com.openagentflow.entity.McpServerEntity;
import com.openagentflow.exception.BusinessException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** MCP stdio 原生传输实现。 */
@Component
public class McpStdioTransport implements McpTransport {

    /** MCP 子进程诊断日志。 */
    private static final Logger log = LoggerFactory.getLogger(McpStdioTransport.class);

    /** JSON 工具。 */
    private final ObjectMapper objectMapper;

    /** stdio 可执行命令安全策略。 */
    private final McpCommandPolicy commandPolicy;

    /** 每个 MCP Server 对应一个长期子进程。 */
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public McpStdioTransport(ObjectMapper objectMapper, OpenAgentFlowProperties properties) {
        this.objectMapper = objectMapper;
        String configured = properties.getMcp().getStdioAllowedCommands();
        List<String> allowed = configured == null ? List.of() : java.util.Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        this.commandPolicy = new McpCommandPolicy(allowed);
    }

    @Override
    public boolean supports(String transportType) {
        return "stdio".equals(transportType == null ? "" : transportType.trim().toLowerCase(Locale.ROOT));
    }

    @Override
    public JsonNode request(McpServerEntity server, ObjectNode payload, Duration timeout) {
        Session session = requireSession(server);
        String requestId = payload.path("id").asText();
        CompletableFuture<JsonNode> pending = new CompletableFuture<>();
        session.pending().put(requestId, pending);
        try {
            write(session, payload);
            return pending.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception exception) {
            session.pending().remove(requestId);
            throw wrap("MCP stdio 请求失败", exception);
        }
    }

    @Override
    public void notify(McpServerEntity server, ObjectNode payload, Duration timeout) {
        try {
            write(requireSession(server), payload);
        } catch (Exception exception) {
            throw wrap("MCP stdio 通知失败", exception);
        }
    }

    /** 获取仍在运行的会话，失效时按当前配置重建。 */
    private Session requireSession(McpServerEntity server) {
        if (server == null || !StringUtils.hasText(server.getCommand())) {
            throw new BusinessException("MCP_STDIO_COMMAND_EMPTY", "stdio MCP Server 需要配置启动命令");
        }
        return sessions.compute(server.getId(), (serverId, current) -> {
            if (current != null && current.process().isAlive()) {
                return current;
            }
            if (current != null) {
                current.close();
            }
            return openSession(server);
        });
    }

    /** 使用 ProcessBuilder 直接启动白名单命令，不经过 shell。 */
    private Session openSession(McpServerEntity server) {
        try {
            List<String> command = new ArrayList<>();
            command.add(commandPolicy.requireAllowed(server.getCommand().trim()));
            command.addAll(parseArguments(server.getArgs()));
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.environment().putAll(parseEnvironment(server.getEnvVars()));
            Process process = builder.start();
            Session session = new Session(process,
                    new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)),
                    new ConcurrentHashMap<>());
            startStdoutReader(server, session);
            startStderrReader(server, session);
            process.onExit().thenRun(() -> failPending(server.getId(), session,
                    new IllegalStateException("MCP stdio 子进程已退出，退出码：" + process.exitValue())));
            return session;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("MCP_STDIO_START_FAILED", "MCP stdio 子进程启动失败：" + exception.getMessage());
        }
    }

    /** 持续读取 stdout 中逐行输出的 JSON-RPC 消息。 */
    private void startStdoutReader(McpServerEntity server, Session session) {
        Thread.ofVirtual().name("mcp-stdio-out-" + server.getId()).start(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    session.process().getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    JsonNode message = objectMapper.readTree(line);
                    CompletableFuture<JsonNode> future = session.pending().remove(message.path("id").asText());
                    if (future != null) {
                        future.complete(message);
                    }
                }
            } catch (Exception exception) {
                failPending(server.getId(), session, exception);
            }
        });
    }

    /** 单独消费 stderr，避免子进程因缓冲区写满而阻塞。 */
    private void startStderrReader(McpServerEntity server, Session session) {
        Thread.ofVirtual().name("mcp-stdio-err-" + server.getId()).start(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    session.process().getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.warn("MCP stdio serverId={} stderr={}", server.getId(), limit(line, 1000));
                }
            } catch (Exception exception) {
                log.debug("MCP stdio stderr 流已关闭，serverId={}", server.getId());
            }
        });
    }

    /** 串行写入单行 JSON，防止并发请求内容交叉。 */
    private void write(Session session, ObjectNode payload) throws Exception {
        synchronized (session.writer()) {
            session.writer().write(objectMapper.writeValueAsString(payload));
            session.writer().newLine();
            session.writer().flush();
        }
    }

    /** 参数优先按 JSON 数组解析，兼容带引号的普通命令行文本。 */
    private List<String> parseArguments(String raw) {
        try {
            if (!StringUtils.hasText(raw)) {
                return List.of();
            }
            String text = raw.trim();
            if (text.startsWith("[")) {
                return objectMapper.readValue(text, new TypeReference<List<String>>() {
                });
            }
            return tokenize(text);
        } catch (Exception exception) {
            throw new BusinessException("MCP_STDIO_ARGS_INVALID", "MCP stdio 参数格式错误：" + exception.getMessage());
        }
    }

    /** 解析简单命令行参数中的单双引号，不执行变量和命令替换。 */
    private List<String> tokenize(String text) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if ((character == '\'' || character == '"')) {
                if (quote == 0) {
                    quote = character;
                } else if (quote == character) {
                    quote = 0;
                } else {
                    current.append(character);
                }
            } else if (Character.isWhitespace(character) && quote == 0) {
                if (!current.isEmpty()) {
                    values.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(character);
            }
        }
        if (quote != 0) {
            throw new BusinessException("MCP_STDIO_ARGS_INVALID", "MCP stdio 参数包含未闭合引号");
        }
        if (!current.isEmpty()) {
            values.add(current.toString());
        }
        return values;
    }

    /** 解析子进程环境变量配置。 */
    private Map<String, String> parseEnvironment(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return Map.of();
            }
            Map<String, Object> source = objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
            Map<String, String> result = new LinkedHashMap<>();
            source.forEach((key, value) -> result.put(key, value == null ? "" : String.valueOf(value)));
            return result;
        } catch (Exception exception) {
            throw new BusinessException("MCP_STDIO_ENV_INVALID", "MCP stdio 环境变量必须是 JSON 对象");
        }
    }

    /** 让当前会话的全部等待请求快速失败。 */
    private void failPending(String serverId, Session session, Throwable error) {
        sessions.remove(serverId, session);
        session.pending().values().forEach(future -> future.completeExceptionally(error));
        session.pending().clear();
    }

    private BusinessException wrap(String prefix, Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        return new BusinessException("MCP_STDIO_FAILED", prefix + "：" + cause.getMessage());
    }

    private String limit(String text, int maxLength) {
        return text == null || text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    @Override
    public void close(String serverId) {
        Session session = sessions.remove(serverId);
        if (session != null) {
            session.close();
        }
    }

    /** 应用关闭时终止全部托管子进程。 */
    @PreDestroy
    public void shutdown() {
        List.copyOf(sessions.keySet()).forEach(this::close);
    }

    /** stdio 子进程会话。 */
    private record Session(Process process, BufferedWriter writer,
                           Map<String, CompletableFuture<JsonNode>> pending) {

        /** 关闭输入输出流并优雅终止子进程。 */
        private void close() {
            try {
                writer.close();
            } catch (Exception ignored) {
                // 关闭阶段不覆盖原业务异常。
            }
            process.destroy();
            try {
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
    }
}
