package com.openagentflow.domain.session;

/**
 * Agent 会话创建请求。
 */
public class AgentSessionCreateRequest {

    /** 会话标题，不传时后端根据首条用户问题自动生成。 */
    private String sessionTitle;

    public String getSessionTitle() {
        return sessionTitle;
    }

    public void setSessionTitle(String sessionTitle) {
        this.sessionTitle = sessionTitle;
    }
}
