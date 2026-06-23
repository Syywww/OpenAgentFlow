package com.openagentflow.domain.chat;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 发送给模型的工具定义。
 */
public class ToolDefinitionForModel {

    /** 工具 ID。 */
    private String id;

    /** 工具函数名称。 */
    private String name;

    /** 工具描述。 */
    private String description;

    /** 工具参数 JSON Schema。 */
    private JsonNode parameters;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JsonNode getParameters() {
        return parameters;
    }

    public void setParameters(JsonNode parameters) {
        this.parameters = parameters;
    }
}
