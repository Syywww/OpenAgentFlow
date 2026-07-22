package com.openagentflow.service;

import com.openagentflow.workflow.WorkflowNodePlugin;
import com.openagentflow.workflow.WorkflowPluginRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 工作流节点插件注册中心测试。 */
class WorkflowPluginRegistryTests {

    /** Spring 插件应按编码注册并执行。 */
    @Test
    void shouldResolveAndExecuteRegisteredPlugin() {
        WorkflowNodePlugin plugin = new WorkflowNodePlugin() {
            @Override public String code() { return "uppercase"; }
            @Override public Object execute(PluginContext context) { return String.valueOf(context.input()).toUpperCase(); }
        };
        WorkflowPluginRegistry registry = new WorkflowPluginRegistry(List.of(plugin), false);

        Object result = registry.require("uppercase").execute(
                new WorkflowNodePlugin.PluginContext("hello", Map.of(), Map.of(), null, null, null));

        assertThat(result).isEqualTo("HELLO");
        assertThatThrownBy(() -> registry.require("missing"))
                .hasMessageContaining("未注册");
    }
}

