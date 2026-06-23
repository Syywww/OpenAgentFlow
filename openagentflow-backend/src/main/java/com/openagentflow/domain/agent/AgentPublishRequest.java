package com.openagentflow.domain.agent;

/**
 * Agent 发布请求。
 */
public class AgentPublishRequest {

    /** 发布版本号，不传时由后端自动生成。 */
    private String versionNo;

    /** 发布说明。 */
    private String publishNote;

    public String getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(String versionNo) {
        this.versionNo = versionNo;
    }

    public String getPublishNote() {
        return publishNote;
    }

    public void setPublishNote(String publishNote) {
        this.publishNote = publishNote;
    }
}
