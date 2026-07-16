package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 解决方案模板收藏实体。 */
@TableName("agent_template_favorite")
public class AgentTemplateFavoriteEntity {
    /** 主键ID。 */ @TableId("id") private String id;
    /** 模板ID。 */ @TableField("template_id") private String templateId;
    /** 收藏用户ID。 */ @TableField("user_id") private String userId;
    /** 收藏时间。 */ @TableField("created_at") private LocalDateTime createdAt;
}
