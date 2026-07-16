package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 解决方案模板安装资源映射实体。 */
@TableName("agent_template_install_resource")
public class AgentTemplateInstallResourceEntity {
    /** 主键ID。 */ @TableId("id") private String id;
    /** 模板安装ID。 */ @TableField("install_id") private String installId;
    /** 模板资源ID。 */ @TableField("template_resource_id") private String templateResourceId;
    /** 资源类型。 */ @TableField("resource_type") private String resourceType;
    /** 来源资源ID。 */ @TableField("source_resource_id") private String sourceResourceId;
    /** 目标资源ID。 */ @TableField("target_resource_id") private String targetResourceId;
    /** 来源哈希。 */ @TableField("source_hash") private String sourceHash;
    /** 安装完成哈希。 */ @TableField("installed_hash") private String installedHash;
    /** 当前哈希。 */ @TableField("current_hash") private String currentHash;
    /** 安装状态。 */ @TableField("install_status") private String installStatus;
    /** 用户是否修改。 */ @TableField("user_modified") private Boolean userModified;
    /** 是否用户新增。 */ @TableField("user_created") private Boolean userCreated;
    /** 对象清单JSON。 */ @TableField("object_manifest") private String objectManifest;
    /** 错误原因。 */ @TableField("error_message") private String errorMessage;
    /** 安装完成时间。 */ @TableField("installed_at") private LocalDateTime installedAt;
    /** 更新时间。 */ @TableField("updated_at") private LocalDateTime updatedAt;
}
