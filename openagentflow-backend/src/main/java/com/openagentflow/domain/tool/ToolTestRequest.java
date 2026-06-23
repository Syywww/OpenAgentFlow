package com.openagentflow.domain.tool;

import java.util.Map;

/**
 * 工具测试请求。
 */
public class ToolTestRequest {

    /** 测试入参。 */
    private Map<String, Object> inputParams;

    public Map<String, Object> getInputParams() {
        return inputParams;
    }

    public void setInputParams(Map<String, Object> inputParams) {
        this.inputParams = inputParams;
    }
}
