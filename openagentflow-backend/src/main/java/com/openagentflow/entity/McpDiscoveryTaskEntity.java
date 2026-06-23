package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * MCP发现任务表。
 * <p>对应数据库表：mcp_discovery_task。</p>
 */
@TableName("mcp_discovery_task")
public class McpDiscoveryTaskEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 服务ID。 */
    @TableField("server_id")
    private String serverId;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** 字段说明：DISCOVEREDTOOLS。 */
    @TableField("discovered_tools")
    private Integer discoveredTools;

    /** 字段说明：DISCOVEREDPROMPTS。 */
    @TableField("discovered_prompts")
    private Integer discoveredPrompts;

    /** 字段说明：DISCOVEREDRESOURCES。 */
    @TableField("discovered_resources")
    private Integer discoveredResources;

    /** 错误信息。 */
    @TableField("error_message")
    private String errorMessage;

    /** 开始时间。 */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /** 完成时间。 */
    @TableField("finished_at")
    private LocalDateTime finishedAt;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getDiscoveredTools() {
        return discoveredTools;
    }

    public void setDiscoveredTools(Integer discoveredTools) {
        this.discoveredTools = discoveredTools;
    }

    public Integer getDiscoveredPrompts() {
        return discoveredPrompts;
    }

    public void setDiscoveredPrompts(Integer discoveredPrompts) {
        this.discoveredPrompts = discoveredPrompts;
    }

    public Integer getDiscoveredResources() {
        return discoveredResources;
    }

    public void setDiscoveredResources(Integer discoveredResources) {
        this.discoveredResources = discoveredResources;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
