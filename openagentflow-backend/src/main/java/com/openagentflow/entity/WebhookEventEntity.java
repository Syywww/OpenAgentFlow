package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * Webhook事件表。
 * <p>对应数据库表：webhook_event。</p>
 */
@TableName("webhook_event")
public class WebhookEventEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 端点ID。 */
    @TableField("endpoint_id")
    private String endpointId;

    /** 事件类型。 */
    @TableField("event_type")
    private String eventType;

    /** 载荷。 */
    @TableField("payload")
    private String payload;

    /** 字段说明：SIGNATURE。 */
    @TableField("signature")
    private String signature;

    /** 成功。 */
    @TableField("success")
    private Boolean success;

    /** 响应状态。 */
    @TableField("response_status")
    private Integer responseStatus;

    /** 响应BODY。 */
    @TableField("response_body")
    private String responseBody;

    /** 错误信息。 */
    @TableField("error_message")
    private String errorMessage;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
