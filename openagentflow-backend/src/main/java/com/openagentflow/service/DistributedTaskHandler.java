package com.openagentflow.service;

import com.openagentflow.entity.AsyncTaskEntity;

import java.util.Map;

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
     * 执行任务业务逻辑。
     *
     * @param task 已被当前 Worker 领取的任务
     * @return 任务结果数据
     */
    Map<String, Object> executeDistributedTask(AsyncTaskEntity task);
}
