package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 解决方案模板三方升级冲突实体。 */
@TableName("agent_template_upgrade_conflict")
public class AgentTemplateUpgradeConflictEntity {
    /** 主键ID。 */ @TableId("id") private String id;
    /** 安装ID。 */ @TableField("install_id") private String installId;
    /** 目标版本ID。 */ @TableField("target_version_id") private String targetVersionId;
    /** 模板资源ID。 */ @TableField("template_resource_id") private String templateResourceId;
    /** 资源类型。 */ @TableField("resource_type") private String resourceType;
    /** 本地资源ID。 */ @TableField("target_resource_id") private String targetResourceId;
    /** 旧模板哈希。 */ @TableField("old_hash") private String oldHash;
    /** 本地哈希。 */ @TableField("local_hash") private String localHash;
    /** 新模板哈希。 */ @TableField("new_hash") private String newHash;
    /** 合并判定。 */ @TableField("merge_decision") private String mergeDecision;
    /** 用户选择。 */ @TableField("user_choice") private String userChoice;
    /** 冲突详情JSON。 */ @TableField("conflict_detail") private String conflictDetail;
    /** 处理用户ID。 */ @TableField("resolved_by") private String resolvedBy;
    /** 处理时间。 */ @TableField("resolved_at") private LocalDateTime resolvedAt;
    /** 创建时间。 */ @TableField("created_at") private LocalDateTime createdAt;
}
