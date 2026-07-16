package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 告警通知补偿投递实体。 */
@TableName("ops_notification_delivery")
public class OpsNotificationDeliveryEntity {
    /** 主键ID。 */ @TableId("id") private String id;
    /** 告警事件ID。 */ @TableField("alert_event_id") private String alertEventId;
    /** 通知渠道ID。 */ @TableField("channel_id") private String channelId;
    /** 通知渠道类型。 */ @TableField("channel_type") private String channelType;
    /** 投递状态。 */ @TableField("status") private String status;
    /** 投递尝试次数。 */ @TableField("attempt_count") private Integer attemptCount;
    /** 下次重试时间。 */ @TableField("next_retry_at") private LocalDateTime nextRetryAt;
    /** 渠道响应摘要。 */ @TableField("response_summary") private String responseSummary;
    /** 错误信息。 */ @TableField("error_message") private String errorMessage;
    /** 发送成功时间。 */ @TableField("sent_at") private LocalDateTime sentAt;
    /** 创建时间。 */ @TableField("created_at") private LocalDateTime createdAt;
    /** 更新时间。 */ @TableField("updated_at") private LocalDateTime updatedAt;
    public String getId(){return id;} public void setId(String value){id=value;}
    public String getAlertEventId(){return alertEventId;} public void setAlertEventId(String value){alertEventId=value;}
    public String getChannelId(){return channelId;} public void setChannelId(String value){channelId=value;}
    public String getChannelType(){return channelType;} public void setChannelType(String value){channelType=value;}
    public String getStatus(){return status;} public void setStatus(String value){status=value;}
    public Integer getAttemptCount(){return attemptCount;} public void setAttemptCount(Integer value){attemptCount=value;}
    public LocalDateTime getNextRetryAt(){return nextRetryAt;} public void setNextRetryAt(LocalDateTime value){nextRetryAt=value;}
    public String getResponseSummary(){return responseSummary;} public void setResponseSummary(String value){responseSummary=value;}
    public String getErrorMessage(){return errorMessage;} public void setErrorMessage(String value){errorMessage=value;}
    public LocalDateTime getSentAt(){return sentAt;} public void setSentAt(LocalDateTime value){sentAt=value;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime value){updatedAt=value;}
}
