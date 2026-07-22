package com.openagentflow.service;

import com.openagentflow.workflow.WorkflowGrayReleasePolicy;
import com.openagentflow.workflow.WorkflowParallelPolicy;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 工作流 P0 执行策略测试。 */
class WorkflowP0PolicyTests {

    /** 并行分支应按分支键稳定合并，并保留发生冲突的分支值。 */
    @Test
    void shouldMergeParallelBranchContextsDeterministically() {
        Map<String, Object> base = new LinkedHashMap<>(Map.of("input", "hello", "shared", "base"));
        Map<String, Map<String, Object>> branches = new LinkedHashMap<>();
        branches.put("branch-b", Map.of("shared", "b", "answerB", 2));
        branches.put("branch-a", Map.of("shared", "a", "answerA", 1));

        Map<String, Object> merged = WorkflowParallelPolicy.mergeContexts(base, branches);

        assertThat(merged.get("input")).isEqualTo("hello");
        assertThat(merged.get("answerA")).isEqualTo(1);
        assertThat(merged.get("answerB")).isEqualTo(2);
        assertThat(merged.get("shared")).isEqualTo("base");
        assertThat(((Map<?, ?>) merged.get("parallelBranches")).keySet().stream().map(String::valueOf).toList())
                .containsExactlyInAnyOrder("branch-a", "branch-b");
    }

    /** 同一个灰度键必须稳定命中，零比例和全量比例必须走固定版本。 */
    @Test
    void shouldSelectGrayVersionByStableHash() {
        assertThat(WorkflowGrayReleasePolicy.useCurrentVersion("wf-1", "user-1", "request-1", 0)).isFalse();
        assertThat(WorkflowGrayReleasePolicy.useCurrentVersion("wf-1", "user-1", "request-1", 100)).isTrue();
        boolean first = WorkflowGrayReleasePolicy.useCurrentVersion("wf-1", "user-1", "request-1", 35);
        assertThat(List.of(first, first, first)).allMatch(value -> value == WorkflowGrayReleasePolicy
                .useCurrentVersion("wf-1", "user-1", "request-1", 35));
    }
}
