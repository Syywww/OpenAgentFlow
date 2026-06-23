package com.openagentflow.domain.trace;

import java.util.List;
import java.util.Map;

/**
 * 运行 Trace 详情。
 */
public class RunDetail extends RunSummary {

    /** 输入载荷。 */
    private Object inputPayload;

    /** 输出载荷。 */
    private Object outputPayload;

    /** 元数据。 */
    private Object metadata;

    /** Trace 步骤列表。 */
    private List<TraceStepDetail> steps;

    /** RAG 检索日志列表。 */
    private List<Map<String, Object>> retrievalLogs;

    /** 工具调用日志列表。 */
    private List<Map<String, Object>> toolInvocations;

    /** LLM 调用日志列表。 */
    private List<Map<String, Object>> llmCalls;

    public Object getInputPayload() {
        return inputPayload;
    }

    public void setInputPayload(Object inputPayload) {
        this.inputPayload = inputPayload;
    }

    public Object getOutputPayload() {
        return outputPayload;
    }

    public void setOutputPayload(Object outputPayload) {
        this.outputPayload = outputPayload;
    }

    public Object getMetadata() {
        return metadata;
    }

    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }

    public List<TraceStepDetail> getSteps() {
        return steps;
    }

    public void setSteps(List<TraceStepDetail> steps) {
        this.steps = steps;
    }

    public List<Map<String, Object>> getRetrievalLogs() {
        return retrievalLogs;
    }

    public void setRetrievalLogs(List<Map<String, Object>> retrievalLogs) {
        this.retrievalLogs = retrievalLogs;
    }

    public List<Map<String, Object>> getToolInvocations() {
        return toolInvocations;
    }

    public void setToolInvocations(List<Map<String, Object>> toolInvocations) {
        this.toolInvocations = toolInvocations;
    }

    public List<Map<String, Object>> getLlmCalls() {
        return llmCalls;
    }

    public void setLlmCalls(List<Map<String, Object>> llmCalls) {
        this.llmCalls = llmCalls;
    }
}
