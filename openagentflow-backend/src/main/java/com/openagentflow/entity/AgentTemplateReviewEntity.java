package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 解决方案模板发布审核实体。 */
@TableName("agent_template_review")
public class AgentTemplateReviewEntity {
    /** 主键ID。 */ @TableId("id") private String id;
    /** 模板ID。 */ @TableField("template_id") private String templateId;
    /** 模板版本ID。 */ @TableField("template_version_id") private String templateVersionId;
    /** 审核类型。 */ @TableField("review_type") private String reviewType;
    /** 审核状态。 */ @TableField("review_status") private String reviewStatus;
    /** 风险等级。 */ @TableField("risk_level") private String riskLevel;
    /** 审核检查项JSON。 */ @TableField("checklist_result") private String checklistResult;
    /** 审核意见。 */ @TableField("review_comment") private String reviewComment;
    /** 审核用户ID。 */ @TableField("reviewer_user_id") private String reviewerUserId;
    /** 审核时间。 */ @TableField("reviewed_at") private LocalDateTime reviewedAt;
}
