package com.openagentflow.workflow;

import com.openagentflow.entity.AgentEntity;
import com.openagentflow.entity.RuntimeRunEntity;
import com.openagentflow.entity.WorkflowNodeEntity;

import java.util.Map;

/**
 * 工作流节点插件扩展契约。
 */
public interface WorkflowNodePlugin {

    /** @return 插件唯一编码 */
    String code();

    /**
     * 执行插件节点。
     *
     * @param context 插件执行上下文
     * @return 可写入工作流上下文的插件输出
     */
    Object execute(PluginContext context);

    /**
     * 插件执行上下文。
     *
     * @param input 上一个节点输出
     * @param workflowContext 工作流上下文只读快照
     * @param config 节点配置只读快照
     * @param node 当前节点
     * @param agent 当前Agent
     * @param runtimeRun Runtime运行信息
     */
    record PluginContext(Object input,
                         Map<String, Object> workflowContext,
                         Map<String, Object> config,
                         WorkflowNodeEntity node,
                         AgentEntity agent,
                         RuntimeRunEntity runtimeRun) {
    }
}

