package com.openagentflow.mcp;

import java.util.List;
import java.util.StringJoiner;

/** MCP SSE 事件解析器。 */
public final class McpSseEventParser {

    private McpSseEventParser() {
    }

    /** 将一个 SSE 事件的文本行解析为结构化事件。 */
    public static Event parse(List<String> lines) {
        String id = "";
        String type = "message";
        StringJoiner data = new StringJoiner("\n");
        if (lines != null) {
            for (String line : lines) {
                if (line == null || line.startsWith(":")) {
                    continue;
                }
                int separator = line.indexOf(':');
                String field = separator < 0 ? line : line.substring(0, separator);
                String value = separator < 0 ? "" : line.substring(separator + 1);
                if (value.startsWith(" ")) {
                    value = value.substring(1);
                }
                switch (field) {
                    case "id" -> id = value;
                    case "event" -> type = value;
                    case "data" -> data.add(value);
                    default -> { }
                }
            }
        }
        return new Event(id, type, data.toString());
    }

    /** SSE事件。 */
    public record Event(String id, String type, String data) {
    }
}

