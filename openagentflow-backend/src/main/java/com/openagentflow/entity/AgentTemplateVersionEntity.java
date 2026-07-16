package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 解决方案模板不可变版本实体。 */
@TableName("agent_template_version")
public class AgentTemplateVersionEntity {
    /** 主键ID。 */ @TableId("id") private String id;
    /** 模板ID。 */ @TableField("template_id") private String templateId;
    /** 语义化版本号。 */ @TableField("version_no") private String versionNo;
    /** 版本展示名称。 */ @TableField("version_name") private String versionName;
    /** 版本更新说明。 */ @TableField("change_log") private String changeLog;
    /** 兼容性声明。 */ @TableField("compatibility_statement") private String compatibilityStatement;
    /** 是否破坏性升级。 */ @TableField("breaking_change") private Boolean breakingChange;
    /** 资源清单JSON。 */ @TableField("resource_manifest") private String resourceManifest;
    /** 依赖图JSON。 */ @TableField("dependency_graph") private String dependencyGraph;
    /** 安全检查结果JSON。 */ @TableField("security_scan_result") private String securityScanResult;
    /** 最小运行检查结果JSON。 */ @TableField("runtime_check_result") private String runtimeCheckResult;
    /** MinIO存储桶。 */ @TableField("package_bucket") private String packageBucket;
    /** MinIO对象键。 */ @TableField("package_key") private String packageKey;
    /** 包SHA-256。 */ @TableField("package_hash") private String packageHash;
    /** 包大小。 */ @TableField("package_size") private Long packageSize;
    /** 版本状态。 */ @TableField("status") private String status;
    /** 提交审核用户ID。 */ @TableField("submitted_by") private String submittedBy;
    /** 提交审核时间。 */ @TableField("submitted_at") private LocalDateTime submittedAt;
    /** 发布用户ID。 */ @TableField("published_by") private String publishedBy;
    /** 发布时间。 */ @TableField("published_at") private LocalDateTime publishedAt;
    /** 创建用户ID。 */ @TableField("created_by") private String createdBy;
    /** 创建时间。 */ @TableField("created_at") private LocalDateTime createdAt;
}
