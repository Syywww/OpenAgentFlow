package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 解决方案模板评论与回复实体。 */
@TableName("agent_template_comment")
public class AgentTemplateCommentEntity {
    /** 主键ID。 */ @TableId("id") private String id;
    /** 模板ID。 */ @TableField("template_id") private String templateId;
    /** 评论用户ID。 */ @TableField("user_id") private String userId;
    /** 成功安装ID。 */ @TableField("install_id") private String installId;
    /** 父评论ID。 */ @TableField("parent_comment_id") private String parentCommentId;
    /** 评论原文。 */ @TableField("comment_content") private String commentContent;
    /** 是否作者回复。 */ @TableField("author_reply") private Boolean authorReply;
    /** 是否管理员回复。 */ @TableField("admin_reply") private Boolean adminReply;
    /** 评论状态。 */ @TableField("status") private String status;
    /** 创建时间。 */ @TableField("created_at") private LocalDateTime createdAt;
    /** 更新时间。 */ @TableField("updated_at") private LocalDateTime updatedAt;
}
