package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 生产性能与容量基线实体。 */
@TableName("capacity_baseline")
public class CapacityBaselineEntity {
    /** 主键ID。 */ @TableId("id") private String id;
    /** 压测场景编码。 */ @TableField("scenario_code") private String scenarioCode;
    /** 环境编码。 */ @TableField("environment_code") private String environmentCode;
    /** 并发级别。 */ @TableField("concurrency_level") private Integer concurrencyLevel;
    /** 每秒请求数。 */ @TableField("request_rate") private BigDecimal requestRate;
    /** P50耗时毫秒。 */ @TableField("p50_latency_ms") private Integer p50LatencyMs;
    /** P95耗时毫秒。 */ @TableField("p95_latency_ms") private Integer p95LatencyMs;
    /** P99耗时毫秒。 */ @TableField("p99_latency_ms") private Integer p99LatencyMs;
    /** 错误率。 */ @TableField("error_rate") private BigDecimal errorRate;
    /** 资源饱和度JSON。 */ @TableField("saturation_json") private String saturationJson;
    /** 数据规模JSON。 */ @TableField("dataset_scale_json") private String datasetScaleJson;
    /** 是否达到容量目标。 */ @TableField("passed") private Boolean passed;
    /** 测量时间。 */ @TableField("measured_at") private LocalDateTime measuredAt;
    /** 创建时间。 */ @TableField("created_at") private LocalDateTime createdAt;
    public String getId(){return id;} public void setId(String value){id=value;}
    public String getScenarioCode(){return scenarioCode;} public void setScenarioCode(String value){scenarioCode=value;}
    public String getEnvironmentCode(){return environmentCode;} public void setEnvironmentCode(String value){environmentCode=value;}
    public Integer getConcurrencyLevel(){return concurrencyLevel;} public void setConcurrencyLevel(Integer value){concurrencyLevel=value;}
    public BigDecimal getRequestRate(){return requestRate;} public void setRequestRate(BigDecimal value){requestRate=value;}
    public Integer getP50LatencyMs(){return p50LatencyMs;} public void setP50LatencyMs(Integer value){p50LatencyMs=value;}
    public Integer getP95LatencyMs(){return p95LatencyMs;} public void setP95LatencyMs(Integer value){p95LatencyMs=value;}
    public Integer getP99LatencyMs(){return p99LatencyMs;} public void setP99LatencyMs(Integer value){p99LatencyMs=value;}
    public BigDecimal getErrorRate(){return errorRate;} public void setErrorRate(BigDecimal value){errorRate=value;}
    public String getSaturationJson(){return saturationJson;} public void setSaturationJson(String value){saturationJson=value;}
    public String getDatasetScaleJson(){return datasetScaleJson;} public void setDatasetScaleJson(String value){datasetScaleJson=value;}
    public Boolean getPassed(){return passed;} public void setPassed(Boolean value){passed=value;}
    public LocalDateTime getMeasuredAt(){return measuredAt;} public void setMeasuredAt(LocalDateTime value){measuredAt=value;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;}
}
