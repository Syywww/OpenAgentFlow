package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 解决方案模板举报治理实体。 */
@TableName("agent_template_report")
public class AgentTemplateReportEntity {
    /** 主键ID。 */ @TableId("id") private String id;
    /** 被举报模板ID。 */ @TableField("template_id") private String templateId;
    /** 举报用户ID。 */ @TableField("reporter_user_id") private String reporterUserId;
    /** 举报类型。 */ @TableField("report_type") private String reportType;
    /** 举报原因。 */ @TableField("report_reason") private String reportReason;
    /** 举报证据JSON。 */ @TableField("evidence") private String evidence;
    /** 处理状态。 */ @TableField("status") private String status;
    /** 处理结论。 */ @TableField("resolution") private String resolution;
    /** 处理用户ID。 */ @TableField("handled_by") private String handledBy;
    /** 处理时间。 */ @TableField("handled_at") private LocalDateTime handledAt;
    /** 创建时间。 */ @TableField("created_at") private LocalDateTime createdAt;
}
