package com.openagentflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务线程池配置。
 */
@Configuration
public class AsyncTaskExecutorConfig {

    /**
     * 创建平台内部异步任务线程池。
     *
     * @return 异步任务执行器
     */
    @Bean("oafAsyncTaskExecutor")
    public Executor oafAsyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 文档解析、Embedding、导入等任务都比较耗时，独立线程池可以避免占用 Web 请求线程。
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("oaf-async-task-");
        executor.initialize();
        return executor;
    }
}

