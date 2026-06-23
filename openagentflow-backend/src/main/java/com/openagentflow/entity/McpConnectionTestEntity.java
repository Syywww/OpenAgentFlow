package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * MCP连接测试表。
 * <p>对应数据库表：mcp_connection_test。</p>
 */
@TableName("mcp_connection_test")
public class McpConnectionTestEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 服务ID。 */
    @TableField("server_id")
    private String serverId;

    /** 成功。 */
    @TableField("success")
    private Boolean success;

    /** 耗时毫秒。 */
    @TableField("latency_ms")
    private Integer latencyMs;

    /** TOOLS数量。 */
    @TableField("tools_count")
    private Integer toolsCount;

    /** PROMPTS数量。 */
    @TableField("prompts_count")
    private Integer promptsCount;

    /** RESOURCES数量。 */
    @TableField("resources_count")
    private Integer resourcesCount;

    /** 请求载荷。 */
    @TableField("request_payload")
    private String requestPayload;

    /** 响应载荷。 */
    @TableField("response_payload")
    private String responsePayload;

    /** 错误信息。 */
    @TableField("error_message")
    private String errorMessage;

    /** TESTED人。 */
    @TableField("tested_by")
    private String testedBy;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Integer getToolsCount() {
        return toolsCount;
    }

    public void setToolsCount(Integer toolsCount) {
        this.toolsCount = toolsCount;
    }

    public Integer getPromptsCount() {
        return promptsCount;
    }

    public void setPromptsCount(Integer promptsCount) {
        this.promptsCount = promptsCount;
    }

    public Integer getResourcesCount() {
        return resourcesCount;
    }

    public void setResourcesCount(Integer resourcesCount) {
        this.resourcesCount = resourcesCount;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getTestedBy() {
        return testedBy;
    }

    public void setTestedBy(String testedBy) {
        this.testedBy = testedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
