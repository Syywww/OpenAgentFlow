package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运行时护栏事件表。
 * <p>对应数据库表：runtime_guardrail_event。</p>
 */
@TableName("runtime_guardrail_event")
public class RuntimeGuardrailEventEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 运行ID。 */
    @TableField("run_id")
    private String runId;

    /** 步骤ID。 */
    @TableField("step_id")
    private String stepId;

    /** 护栏类型。 */
    @TableField("guardrail_type")
    private String guardrailType;

    /** 策略编码。 */
    @TableField("policy_code")
    private String policyCode;

    /** 字段说明：ACTION。 */
    @TableField("action")
    private String action;

    /** 风险得分。 */
    @TableField("risk_score")
    private BigDecimal riskScore;

    /** 输入文本。 */
    @TableField("input_text")
    private String inputText;

    /** 输出文本。 */
    @TableField("output_text")
    private String outputText;

    /** 字段说明：DETAIL。 */
    @TableField("detail")
    private String detail;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public String getGuardrailType() {
        return guardrailType;
    }

    public void setGuardrailType(String guardrailType) {
        this.guardrailType = guardrailType;
    }

    public String getPolicyCode() {
        return policyCode;
    }

    public void setPolicyCode(String policyCode) {
        this.policyCode = policyCode;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }

    public String getInputText() {
        return inputText;
    }

    public void setInputText(String inputText) {
        this.inputText = inputText;
    }

    public String getOutputText() {
        return outputText;
    }

    public void setOutputText(String outputText) {
        this.outputText = outputText;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
