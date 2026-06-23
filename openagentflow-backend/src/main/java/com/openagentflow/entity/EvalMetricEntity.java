package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 评测距离度量表。
 * <p>对应数据库表：eval_metric。</p>
 */
@TableName("eval_metric")
public class EvalMetricEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 距离度量编码。 */
    @TableField("metric_code")
    private String metricCode;

    /** 距离度量名称。 */
    @TableField("metric_name")
    private String metricName;

    /** 距离度量类型。 */
    @TableField("metric_type")
    private String metricType;

    /** 描述。 */
    @TableField("description")
    private String description;

    /** 配置JSON。 */
    @TableField("config_json")
    private String configJson;

    /** 是否启用。 */
    @TableField("enabled")
    private Boolean enabled;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMetricCode() {
        return metricCode;
    }

    public void setMetricCode(String metricCode) {
        this.metricCode = metricCode;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
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
}
