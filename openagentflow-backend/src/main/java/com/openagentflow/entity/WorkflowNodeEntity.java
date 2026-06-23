package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工作流节点表。
 * <p>对应数据库表：workflow_node。</p>
 */
@TableName("workflow_node")
public class WorkflowNodeEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 工作流ID。 */
    @TableField("workflow_id")
    private String workflowId;

    /** 节点密钥。 */
    @TableField("node_key")
    private String nodeKey;

    /** 节点名称。 */
    @TableField("node_name")
    private String nodeName;

    /** 节点类型。 */
    @TableField("node_type")
    private String nodeType;

    /** 字段说明：POSITIONX。 */
    @TableField("position_x")
    private BigDecimal positionX;

    /** 字段说明：POSITIONY。 */
    @TableField("position_y")
    private BigDecimal positionY;

    /** 配置JSON。 */
    @TableField("config_json")
    private String configJson;

    /** 输入Schema。 */
    @TableField("input_schema")
    private String inputSchema;

    /** 输出Schema。 */
    @TableField("output_schema")
    private String outputSchema;

    /** 重试策略。 */
    @TableField("retry_policy")
    private String retryPolicy;

    /** 是否启用。 */
    @TableField("enabled")
    private Boolean enabled;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public void setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public BigDecimal getPositionX() {
        return positionX;
    }

    public void setPositionX(BigDecimal positionX) {
        this.positionX = positionX;
    }

    public BigDecimal getPositionY() {
        return positionY;
    }

    public void setPositionY(BigDecimal positionY) {
        this.positionY = positionY;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public String getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(String inputSchema) {
        this.inputSchema = inputSchema;
    }

    public String getOutputSchema() {
        return outputSchema;
    }

    public void setOutputSchema(String outputSchema) {
        this.outputSchema = outputSchema;
    }

    public String getRetryPolicy() {
        return retryPolicy;
    }

    public void setRetryPolicy(String retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
