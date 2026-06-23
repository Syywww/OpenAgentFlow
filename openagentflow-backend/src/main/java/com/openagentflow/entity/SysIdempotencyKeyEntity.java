package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 系统IDEMPOTENCY密钥表。
 * <p>对应数据库表：sys_idempotency_key。</p>
 */
@TableName("sys_idempotency_key")
public class SysIdempotencyKeyEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** IDEM密钥。 */
    @TableField("idem_key")
    private String idemKey;

    /** 请求哈希。 */
    @TableField("request_hash")
    private String requestHash;

    /** 响应BODY。 */
    @TableField("response_body")
    private String responseBody;

    /** 状态编码。 */
    @TableField("status_code")
    private Integer statusCode;

    /** 字段说明：LOCKEDUNTIL。 */
    @TableField("locked_until")
    private LocalDateTime lockedUntil;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 过期时间。 */
    @TableField("expired_at")
    private LocalDateTime expiredAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdemKey() {
        return idemKey;
    }

    public void setIdemKey(String idemKey) {
        this.idemKey = idemKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }
}
