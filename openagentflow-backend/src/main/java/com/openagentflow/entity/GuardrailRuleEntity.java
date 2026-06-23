package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 护栏RULE表。
 * <p>对应数据库表：guardrail_rule。</p>
 */
@TableName("guardrail_rule")
public class GuardrailRuleEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 策略ID。 */
    @TableField("policy_id")
    private String policyId;

    /** RULE编码。 */
    @TableField("rule_code")
    private String ruleCode;

    /** RULE名称。 */
    @TableField("rule_name")
    private String ruleName;

    /** 字段说明：RULEEXPR。 */
    @TableField("rule_expr")
    private String ruleExpr;

    /** 字段说明：KEYWORDS。 */
    @TableField("keywords")
    private String keywords;

    /** 风险级别。 */
    @TableField("risk_level")
    private String riskLevel;

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

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleExpr() {
        return ruleExpr;
    }

    public void setRuleExpr(String ruleExpr) {
        this.ruleExpr = ruleExpr;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
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
