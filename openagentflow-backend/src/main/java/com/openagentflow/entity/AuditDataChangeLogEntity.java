package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 审计数据变更日志表。
 * <p>对应数据库表：audit_data_change_log。</p>
 */
@TableName("audit_data_change_log")
public class AuditDataChangeLogEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 操作日志ID。 */
    @TableField("operation_log_id")
    private String operationLogId;

    /** 表名称。 */
    @TableField("table_name")
    private String tableName;

    /** 记录ID。 */
    @TableField("record_id")
    private String recordId;

    /** 变更类型。 */
    @TableField("change_type")
    private String changeType;

    /** BEFORE数据。 */
    @TableField("before_data")
    private String beforeData;

    /** AFTER数据。 */
    @TableField("after_data")
    private String afterData;

    /** CHANGED人。 */
    @TableField("changed_by")
    private String changedBy;

    /** CHANGED时间。 */
    @TableField("changed_at")
    private LocalDateTime changedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOperationLogId() {
        return operationLogId;
    }

    public void setOperationLogId(String operationLogId) {
        this.operationLogId = operationLogId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getBeforeData() {
        return beforeData;
    }

    public void setBeforeData(String beforeData) {
        this.beforeData = beforeData;
    }

    public String getAfterData() {
        return afterData;
    }

    public void setAfterData(String afterData) {
        this.afterData = afterData;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
