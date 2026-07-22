package com.openagentflow.workflow;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工作流并行分支上下文合并策略。
 */
public final class WorkflowParallelPolicy {

    private WorkflowParallelPolicy() {
    }

    /**
     * 按分支键稳定合并上下文。
     *
     * <p>基础上下文拥有最高优先级；分支新增字段进入主上下文，冲突字段保留在
     * {@code parallelBranches} 命名空间，避免并发完成顺序改变最终结果。</p>
     *
     * @param baseContext 并行节点执行前的基础上下文
     * @param branchContexts 分支键与分支执行后上下文
     * @return 合并后的新上下文
     */
    public static Map<String, Object> mergeContexts(Map<String, Object> baseContext,
                                                    Map<String, Map<String, Object>> branchContexts) {
        Map<String, Object> merged = new LinkedHashMap<>(baseContext == null ? Map.of() : baseContext);
        Map<String, Object> orderedBranches = new LinkedHashMap<>();
        if (branchContexts != null) {
            branchContexts.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.nullsLast(String::compareTo)))
                    .forEach(entry -> {
                        Map<String, Object> branch = new LinkedHashMap<>(entry.getValue() == null ? Map.of() : entry.getValue());
                        orderedBranches.put(entry.getKey(), branch);
                        branch.forEach(merged::putIfAbsent);
                    });
        }
        merged.put("parallelBranches", orderedBranches);
        merged.put("parallelBranchCount", orderedBranches.size());
        return merged;
    }
}

