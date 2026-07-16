package com.openagentflow.service;

import com.openagentflow.entity.AsyncTaskEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/** Kafka解决方案模板安装与升级任务处理器。 */
@Service
public class TemplateInstallTaskHandler implements DistributedTaskHandler {

    /** 模板安装服务。 */
    private final TemplateInstallService installService;

    public TemplateInstallTaskHandler(TemplateInstallService installService) {
        this.installService = installService;
    }

    /** 主任务类型。 */
    @Override
    public String taskType() {
        return "TEMPLATE_INSTALL";
    }

    /** 同时承接安装和升级任务。 */
    @Override
    public Set<String> taskTypes() {
        return Set.of("TEMPLATE_INSTALL", "TEMPLATE_UPGRADE");
    }

    /** 根据任务类型执行完整安装或三方升级。 */
    @Override
    public Map<String, Object> executeDistributedTask(AsyncTaskEntity task) {
        return "TEMPLATE_UPGRADE".equals(task.getTaskType())
                ? installService.executeUpgrade(task)
                : installService.executeInstall(task);
    }
}
