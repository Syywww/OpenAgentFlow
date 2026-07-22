package com.openagentflow.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 测试专用的最小 stdio MCP JSON-RPC 服务端。 */
public final class McpStdioEchoServer {

    /** JSON-RPC ID 提取表达式。 */
    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private McpStdioEchoServer() {
    }

    /** 持续读取单行请求并回显固定 result。 */
    public static void main(String[] args) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = ID_PATTERN.matcher(line);
                if (matcher.find()) {
                    System.out.println("{\"jsonrpc\":\"2.0\",\"id\":\"" + matcher.group(1)
                            + "\",\"result\":{\"transport\":\"stdio\"}}");
                    System.out.flush();
                }
            }
        }
    }
}
