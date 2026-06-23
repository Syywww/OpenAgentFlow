package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 资源标签表。
 * <p>对应数据库表：resource_tag。</p>
 */
@TableName("resource_tag")
public class ResourceTagEntity {

    /** 资源类型。 */
    @TableField("resource_type")
    private String resourceType;

    /** 资源ID。 */
    @TableField("resource_id")
    private String resourceId;

    /** 标签ID。 */
    @TableField("tag_id")
    private String tagId;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getTagId() {
        return tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
