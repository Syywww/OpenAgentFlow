package com.openagentflow.domain.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 知识文档 MinIO 直传申请参数。
 */
public class KnowledgeDirectUploadRequest {

    /** 原始文件名。 */
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /** 文件字节数。 */
    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须大于0")
    private Long fileSize;

    /** 文件 MIME 类型。 */
    private String contentType;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
