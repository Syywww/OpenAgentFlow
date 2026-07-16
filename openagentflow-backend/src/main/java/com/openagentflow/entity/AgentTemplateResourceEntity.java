package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 解决方案模板版本资源清单实体。 */
@TableName("agent_template_resource")
public class AgentTemplateResourceEntity {
    /** 主键ID。 */ @TableId("id") private String id;
    /** 模板版本ID。 */ @TableField("template_version_id") private String templateVersionId;
    /** 资源类型。 */ @TableField("resource_type") private String resourceType;
    /** 来源资源ID。 */ @TableField("source_resource_id") private String sourceResourceId;
    /** 资源编码。 */ @TableField("resource_code") private String resourceCode;
    /** 资源名称。 */ @TableField("resource_name") private String resourceName;
    /** 资源快照JSON。 */ @TableField("resource_snapshot") private String resourceSnapshot;
    /** 内容哈希。 */ @TableField("content_hash") private String contentHash;
    /** 父资源ID。 */ @TableField("parent_resource_id") private String parentResourceId;
    /** 依赖资源ID数组。 */ @TableField("dependency_ids") private String dependencyIds;
    /** MinIO对象清单。 */ @TableField("object_manifest") private String objectManifest;
    /** 安装顺序。 */ @TableField("sort_order") private Integer sortOrder;
    /** 是否必需。 */ @TableField("required") private Boolean required;
    /** 创建时间。 */ @TableField("created_at") private LocalDateTime createdAt;
}
