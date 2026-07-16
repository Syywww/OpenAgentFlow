package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 上传文件安全扫描实体。 */
@TableName("file_security_scan")
public class FileSecurityScanEntity {
    /** 主键ID。 */ @TableId("id") private String id;
    /** 工作空间ID。 */ @TableField("workspace_id") private String workspaceId;
    /** 知识文档ID。 */ @TableField("document_id") private String documentId;
    /** 对象存储桶。 */ @TableField("object_bucket") private String objectBucket;
    /** 对象存储键。 */ @TableField("object_key") private String objectKey;
    /** 文件哈希。 */ @TableField("file_hash") private String fileHash;
    /** 真实文件类型。 */ @TableField("detected_type") private String detectedType;
    /** 扫描引擎。 */ @TableField("scan_engine") private String scanEngine;
    /** 扫描状态。 */ @TableField("scan_status") private String scanStatus;
    /** 威胁名称。 */ @TableField("threat_name") private String threatName;
    /** 扫描详情JSON。 */ @TableField("detail_json") private String detailJson;
    /** 扫描完成时间。 */ @TableField("scanned_at") private LocalDateTime scannedAt;
    /** 创建时间。 */ @TableField("created_at") private LocalDateTime createdAt;
    public String getId(){return id;} public void setId(String value){id=value;}
    public String getWorkspaceId(){return workspaceId;} public void setWorkspaceId(String value){workspaceId=value;}
    public String getDocumentId(){return documentId;} public void setDocumentId(String value){documentId=value;}
    public String getObjectBucket(){return objectBucket;} public void setObjectBucket(String value){objectBucket=value;}
    public String getObjectKey(){return objectKey;} public void setObjectKey(String value){objectKey=value;}
    public String getFileHash(){return fileHash;} public void setFileHash(String value){fileHash=value;}
    public String getDetectedType(){return detectedType;} public void setDetectedType(String value){detectedType=value;}
    public String getScanEngine(){return scanEngine;} public void setScanEngine(String value){scanEngine=value;}
    public String getScanStatus(){return scanStatus;} public void setScanStatus(String value){scanStatus=value;}
    public String getThreatName(){return threatName;} public void setThreatName(String value){threatName=value;}
    public String getDetailJson(){return detailJson;} public void setDetailJson(String value){detailJson=value;}
    public LocalDateTime getScannedAt(){return scannedAt;} public void setScannedAt(LocalDateTime value){scannedAt=value;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;}
}
