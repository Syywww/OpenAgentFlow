package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 工作流连线表。
 * <p>对应数据库表：workflow_edge。</p>
 */
@TableName("workflow_edge")
public class WorkflowEdgeEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 工作流ID。 */
    @TableField("workflow_id")
    private String workflowId;

    /** 连线密钥。 */
    @TableField("edge_key")
    private String edgeKey;

    /** 来源节点密钥。 */
    @TableField("source_node_key")
    private String sourceNodeKey;

    /** TARGET节点密钥。 */
    @TableField("target_node_key")
    private String targetNodeKey;

    /** 字段说明：CONDITIONEXPR。 */
    @TableField("condition_expr")
    private String conditionExpr;

    /** 字段说明：LABEL。 */
    @TableField("label")
    private String label;

    /** 元数据JSON。 */
    @TableField("metadata")
    private String metadata;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

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

    public String getEdgeKey() {
        return edgeKey;
    }

    public void setEdgeKey(String edgeKey) {
        this.edgeKey = edgeKey;
    }

    public String getSourceNodeKey() {
        return sourceNodeKey;
    }

    public void setSourceNodeKey(String sourceNodeKey) {
        this.sourceNodeKey = sourceNodeKey;
    }

    public String getTargetNodeKey() {
        return targetNodeKey;
    }

    public void setTargetNodeKey(String targetNodeKey) {
        this.targetNodeKey = targetNodeKey;
    }

    public String getConditionExpr() {
        return conditionExpr;
    }

    public void setConditionExpr(String conditionExpr) {
        this.conditionExpr = conditionExpr;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
