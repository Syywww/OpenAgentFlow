package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 本地模型部署表。
 * <p>对应数据库表：local_model_deployment。</p>
 */
@TableName("local_model_deployment")
public class LocalModelDeploymentEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 运行时ID。 */
    @TableField("runtime_id")
    private String runtimeId;

    /** 模型ID。 */
    @TableField("model_id")
    private String modelId;

    /** 部署名称。 */
    @TableField("deployment_name")
    private String deploymentName;

    /** IMAGE名称。 */
    @TableField("image_name")
    private String imageName;

    /** 模型路径。 */
    @TableField("model_path")
    private String modelPath;

    /** 资源请求。 */
    @TableField("resource_request")
    private String resourceRequest;

    /** 字段说明：ENVVARS。 */
    @TableField("env_vars")
    private String envVars;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** DEPLOYED时间。 */
    @TableField("deployed_at")
    private LocalDateTime deployedAt;

    /** 创建人ID。 */
    @TableField("created_by")
    private String createdBy;

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

    public String getRuntimeId() {
        return runtimeId;
    }

    public void setRuntimeId(String runtimeId) {
        this.runtimeId = runtimeId;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public String getResourceRequest() {
        return resourceRequest;
    }

    public void setResourceRequest(String resourceRequest) {
        this.resourceRequest = resourceRequest;
    }

    public String getEnvVars() {
        return envVars;
    }

    public void setEnvVars(String envVars) {
        this.envVars = envVars;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDeployedAt() {
        return deployedAt;
    }

    public void setDeployedAt(LocalDateTime deployedAt) {
        this.deployedAt = deployedAt;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
