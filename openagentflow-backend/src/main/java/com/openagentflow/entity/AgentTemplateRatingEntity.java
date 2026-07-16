package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 解决方案模板用户评分实体。 */
@TableName("agent_template_rating")
public class AgentTemplateRatingEntity {
    /** 主键ID。 */ @TableId("id") private String id;
    /** 模板ID。 */ @TableField("template_id") private String templateId;
    /** 评分用户ID。 */ @TableField("user_id") private String userId;
    /** 成功安装ID。 */ @TableField("install_id") private String installId;
    /** 评分1到5分。 */ @TableField("rating") private Integer rating;
    /** 创建时间。 */ @TableField("created_at") private LocalDateTime createdAt;
    /** 更新时间。 */ @TableField("updated_at") private LocalDateTime updatedAt;
}
