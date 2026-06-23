package com.openagentflow.domain.session;

/**
 * Agent 会话更新请求。
 */
public class AgentSessionUpdateRequest {

    /** 会话标题。 */
    private String sessionTitle;

    /** 会话状态，常用值：active、archived。 */
    private String status;

    public String getSessionTitle() {
        return sessionTitle;
    }

    public void setSessionTitle(String sessionTitle) {
        this.sessionTitle = sessionTitle;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
