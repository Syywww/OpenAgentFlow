package com.openagentflow.workflow;

import com.openagentflow.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * 工作流节点插件注册中心。
 */
@Component
public class WorkflowPluginRegistry {

    /** 已注册插件，键为小写插件编码。 */
    private final Map<String, WorkflowNodePlugin> plugins = new LinkedHashMap<>();

    @Autowired
    public WorkflowPluginRegistry(List<WorkflowNodePlugin> springPlugins) {
        this(springPlugins, true);
    }

    /**
     * 创建插件注册中心。
     *
     * @param springPlugins Spring容器中的插件
     * @param loadServicePlugins 是否加载外部JAR通过ServiceLoader声明的插件
     */
    public WorkflowPluginRegistry(List<WorkflowNodePlugin> springPlugins, boolean loadServicePlugins) {
        if (springPlugins != null) {
            springPlugins.forEach(this::register);
        }
        if (loadServicePlugins) {
            ServiceLoader.load(WorkflowNodePlugin.class).forEach(this::register);
        }
    }

    /** 注册插件，重复编码采用首次注册实现，防止外部JAR覆盖平台内置插件。 */
    public void register(WorkflowNodePlugin plugin) {
        if (plugin == null || !StringUtils.hasText(plugin.code())) {
            throw new IllegalArgumentException("工作流插件编码不能为空");
        }
        plugins.putIfAbsent(normalize(plugin.code()), plugin);
    }

    /** 获取必需插件，未知插件直接失败，避免静默跳过业务节点。 */
    public WorkflowNodePlugin require(String code) {
        WorkflowNodePlugin plugin = plugins.get(normalize(code));
        if (plugin == null) {
            throw new BusinessException("WORKFLOW_PLUGIN_NOT_FOUND", "工作流插件未注册：" + code);
        }
        return plugin;
    }

    /** @return 当前已注册插件编码 */
    public List<String> codes() {
        return List.copyOf(plugins.keySet());
    }

    private String normalize(String code) {
        return code == null ? "" : code.trim().toLowerCase(Locale.ROOT);
    }
}
