package com.openagentflow.domain.trace;

/**
 * 运行日志基础统计。
 */
public class RunStats {

    /** 全部运行数。 */
    private Long totalRuns;

    /** 成功运行数。 */
    private Long successRuns;

    /** 失败运行数。 */
    private Long failedRuns;

    /** 运行中数量。 */
    private Long runningRuns;

    /** 平均耗时毫秒。 */
    private Integer avgLatencyMs;

    /** 总 Token 数。 */
    private Long totalTokens;

    public Long getTotalRuns() {
        return totalRuns;
    }

    public void setTotalRuns(Long totalRuns) {
        this.totalRuns = totalRuns;
    }

    public Long getSuccessRuns() {
        return successRuns;
    }

    public void setSuccessRuns(Long successRuns) {
        this.successRuns = successRuns;
    }

    public Long getFailedRuns() {
        return failedRuns;
    }

    public void setFailedRuns(Long failedRuns) {
        this.failedRuns = failedRuns;
    }

    public Long getRunningRuns() {
        return runningRuns;
    }

    public void setRunningRuns(Long runningRuns) {
        this.runningRuns = runningRuns;
    }

    public Integer getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public void setAvgLatencyMs(Integer avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
    }

    public Long getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Long totalTokens) {
        this.totalTokens = totalTokens;
    }
}
