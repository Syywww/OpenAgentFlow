package com.openagentflow.service;

/**
 * 当前线程的分布式任务执行租约上下文。
 */
public final class AsyncTaskExecutionContext {

    /** Worker 线程本地租约。 */
    private static final ThreadLocal<Lease> CURRENT = new ThreadLocal<>();

    private AsyncTaskExecutionContext() {
    }

    /**
     * 绑定当前 Worker 获取的执行代次。
     */
    public static void bind(String taskId, String workerId, long lockVersion) {
        CURRENT.set(new Lease(taskId, workerId, lockVersion));
    }

    /**
     * 获取当前执行租约。
     */
    public static Lease current() {
        return CURRENT.get();
    }

    /**
     * 清理线程租约，避免线程复用污染其他任务。
     */
    public static void clear() {
        CURRENT.remove();
    }

    /**
     * Worker 执行租约。
     *
     * @param taskId 任务ID
     * @param workerId Worker ID
     * @param lockVersion 执行代次
     */
    public record Lease(String taskId, String workerId, long lockVersion) {
    }
}
