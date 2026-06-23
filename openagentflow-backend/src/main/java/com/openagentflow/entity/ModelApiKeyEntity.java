package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 模型API密钥表。
 * <p>对应数据库表：model_api_key。</p>
 */
@TableName("model_api_key")
public class ModelApiKeyEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 服务商ID。 */
    @TableField("provider_id")
    private String providerId;

    /** 密钥名称。 */
    @TableField("key_name")
    private String keyName;

    /** 密钥CIPHER。 */
    @TableField("key_cipher")
    private String keyCipher;

    /** 密钥MASK。 */
    @TableField("key_mask")
    private String keyMask;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** 配额LIMIT。 */
    @TableField("quota_limit")
    private Long quotaLimit;

    /** 配额USED。 */
    @TableField("quota_used")
    private Long quotaUsed;

    /** 过期时间。 */
    @TableField("expired_at")
    private LocalDateTime expiredAt;

    /** 创建人ID。 */
    @TableField("created_by")
    private String createdBy;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getKeyCipher() {
        return keyCipher;
    }

    public void setKeyCipher(String keyCipher) {
        this.keyCipher = keyCipher;
    }

    public String getKeyMask() {
        return keyMask;
    }

    public void setKeyMask(String keyMask) {
        this.keyMask = keyMask;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getQuotaLimit() {
        return quotaLimit;
    }

    public void setQuotaLimit(Long quotaLimit) {
        this.quotaLimit = quotaLimit;
    }

    public Long getQuotaUsed() {
        return quotaUsed;
    }

    public void setQuotaUsed(Long quotaUsed) {
        this.quotaUsed = quotaUsed;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
