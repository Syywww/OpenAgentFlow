package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 生产组件灾备目标实体。 */
@TableName("disaster_recovery_target")
public class DisasterRecoveryTargetEntity {
    /** 主键ID。 */ @TableId("id") private String id;
    /** 组件编码。 */ @TableField("component_code") private String componentCode;
    /** 高可用部署模式。 */ @TableField("deployment_mode") private String deploymentMode;
    /** 目标RPO秒数。 */ @TableField("target_rpo_seconds") private Long targetRpoSeconds;
    /** 目标RTO秒数。 */ @TableField("target_rto_seconds") private Long targetRtoSeconds;
    /** 最小副本数。 */ @TableField("min_replicas") private Integer minReplicas;
    /** 备份策略。 */ @TableField("backup_strategy") private String backupStrategy;
    /** 故障切换策略。 */ @TableField("failover_strategy") private String failoverStrategy;
    /** 是否启用。 */ @TableField("enabled") private Boolean enabled;
    /** 创建时间。 */ @TableField("created_at") private LocalDateTime createdAt;
    /** 更新时间。 */ @TableField("updated_at") private LocalDateTime updatedAt;
    public String getId(){return id;} public void setId(String value){id=value;}
    public String getComponentCode(){return componentCode;} public void setComponentCode(String value){componentCode=value;}
    public String getDeploymentMode(){return deploymentMode;} public void setDeploymentMode(String value){deploymentMode=value;}
    public Long getTargetRpoSeconds(){return targetRpoSeconds;} public void setTargetRpoSeconds(Long value){targetRpoSeconds=value;}
    public Long getTargetRtoSeconds(){return targetRtoSeconds;} public void setTargetRtoSeconds(Long value){targetRtoSeconds=value;}
    public Integer getMinReplicas(){return minReplicas;} public void setMinReplicas(Integer value){minReplicas=value;}
    public String getBackupStrategy(){return backupStrategy;} public void setBackupStrategy(String value){backupStrategy=value;}
    public String getFailoverStrategy(){return failoverStrategy;} public void setFailoverStrategy(String value){failoverStrategy=value;}
    public Boolean getEnabled(){return enabled;} public void setEnabled(Boolean value){enabled=value;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime value){updatedAt=value;}
}
