package com.openagentflow.service;

import com.openagentflow.entity.AsyncTaskEntity;

import java.util.Map;
import java.util.Set;

/**
 * Kafka 分布式任务处理器扩展点。
 */
public interface DistributedTaskHandler {

    /**
     * 返回当前处理器支持的任务类型。
     *
     * @return 任务类型编码
     */
    String taskType();

    /**
     * 返回当前处理器支持的全部任务类型。
     *
     * <p>默认仅返回主任务类型；DAG 处理器可覆盖该方法，在同一个服务中承接多个阶段任务。</p>
     *
     * @return 任务类型集合
     */
    default Set<String> taskTypes() {
        return Set.of(taskType());
    }

    /**
     * 执行任务业务逻辑。
     *
     * @param task 已被当前 Worker 领取的任务
     * @return 任务结果数据
     */
    Map<String, Object> executeDistributedTask(AsyncTaskEntity task);
}
