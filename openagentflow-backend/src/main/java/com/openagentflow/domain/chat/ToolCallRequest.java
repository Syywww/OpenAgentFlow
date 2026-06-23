package com.openagentflow.domain.chat;

/**
 * 模型请求调用的工具信息。
 */
public class ToolCallRequest {

    /** 模型生成的工具调用 ID。 */
    private String id;

    /** 工具函数名称。 */
    private String name;

    /** 工具参数 JSON 字符串。 */
    private String argumentsJson;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArgumentsJson() {
        return argumentsJson;
    }

    public void setArgumentsJson(String argumentsJson) {
        this.argumentsJson = argumentsJson;
    }
}
