package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 工作流版本表。
 * <p>对应数据库表：workflow_version。</p>
 */
@TableName("workflow_version")
public class WorkflowVersionEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 工作流ID。 */
    @TableField("workflow_id")
    private String workflowId;

    /** 版本序号。 */
    @TableField("version_no")
    private String versionNo;

    /** 画布JSON。 */
    @TableField("graph_json")
    private String graphJson;

    /** 变量Schema。 */
    @TableField("variable_schema")
    private String variableSchema;

    /** 字段说明：PUBLISHENV。 */
    @TableField("publish_env")
    private String publishEnv;

    /** 字段说明：PUBLISHNOTE。 */
    @TableField("publish_note")
    private String publishNote;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** 创建人ID。 */
    @TableField("created_by")
    private String createdBy;

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

    public String getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(String versionNo) {
        this.versionNo = versionNo;
    }

    public String getGraphJson() {
        return graphJson;
    }

    public void setGraphJson(String graphJson) {
        this.graphJson = graphJson;
    }

    public String getVariableSchema() {
        return variableSchema;
    }

    public void setVariableSchema(String variableSchema) {
        this.variableSchema = variableSchema;
    }

    public String getPublishEnv() {
        return publishEnv;
    }

    public void setPublishEnv(String publishEnv) {
        this.publishEnv = publishEnv;
    }

    public String getPublishNote() {
        return publishNote;
    }

    public void setPublishNote(String publishNote) {
        this.publishNote = publishNote;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
