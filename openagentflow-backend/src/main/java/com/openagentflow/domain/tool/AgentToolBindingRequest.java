package com.openagentflow.domain.tool;

import java.util.List;

/**
 * Agent 工具绑定保存请求。
 */
public class AgentToolBindingRequest {

    /** 绑定的工具 ID 列表。 */
    private List<String> toolIds;

    public List<String> getToolIds() {
        return toolIds;
    }

    public void setToolIds(List<String> toolIds) {
        this.toolIds = toolIds;
    }
}
