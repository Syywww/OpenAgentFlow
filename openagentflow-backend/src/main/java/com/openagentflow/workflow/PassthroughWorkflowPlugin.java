package com.openagentflow.workflow;

import org.springframework.stereotype.Component;

/** 平台内置透传插件，可作为自定义插件接入样例。 */
@Component
public class PassthroughWorkflowPlugin implements WorkflowNodePlugin {

    @Override
    public String code() {
        return "passthrough";
    }

    @Override
    public Object execute(PluginContext context) {
        return context.input();
    }
}

