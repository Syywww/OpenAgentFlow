package com.openagentflow.service;

import com.openagentflow.workflow.WorkflowParallelPolicy;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.openagentflow.workflow.WorkflowParallelPolicy.mergeContexts;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工作流并行分支合并策略测试。
 *
 * <p>覆盖并行分支上下文合并的确定性、冲突解决与命名空间隔离，
 * 确保并发完成顺序不会改变最终合并结果。</p>
 */
class WorkflowParallelPolicyTests {

    /** 分支上下文按分支键排序合并，结果不受传入顺序影响（确定性JOIN）。 */
    @Test
    void shouldMergeBranchesInDeterministicOrder() {
        // 乱序传入（b 在前、a 在后），各分支 key 不同，测的是排序
        Map<String, Map<String, Object>> branches = new LinkedHashMap<>();
        branches.put("b", Map.of("x", 1));
        branches.put("a", Map.of("y", 2));

        Map<String, Object> merged = mergeContexts(Map.of(), branches);

        assertThat(merged).containsEntry("parallelBranchCount", 2);
        @SuppressWarnings("unchecked")
        Map<String, Object> orderedBranches = (Map<String, Object>) merged.get("parallelBranches");
        // 按 key 排序后，a 必须在 b 之前
        assertThat(orderedBranches.keySet()).containsExactly("a", "b");
    }

    /** 并发分支写入相同字段时，先出现的分支生效，后到分支不覆盖。 */
    @Test
    void shouldKeepFirstBranchFieldOnConflict() {
        // 两个分支同 key 不同值，LinkedHashMap 保证 a 先 b 后
        Map<String, Map<String, Object>> branches = new LinkedHashMap<>();
        branches.put("a", Map.of("k", "a-value"));
        branches.put("b", Map.of("k", "b-value"));

        Map<String, Object> merged = mergeContexts(Map.of(), branches);

        // a 先处理，putIfAbsent 挡掉 b，主上下文取 a 的值
        assertThat(merged).containsEntry("k", "a-value");
    }

    /** 基础上下文拥有最高优先级，分支字段不得覆盖基础字段。 */
    @Test
    void shouldGiveBaseContextPriorityOverBranches() {
        Map<String, Object> base = Map.of("k", "base-value");
        Map<String, Map<String, Object>> branches = Map.of(
                "a", Map.of("k", "branch-value")
        );

        Map<String, Object> merged = mergeContexts(base, branches);

        assertThat(merged).containsEntry("k", "base-value");
    }

    /** 分支新增字段进入主上下文；冲突字段保留在 parallelBranches 命名空间。 */
    @Test
    void shouldIsolateConflictFieldsIntoBranchesNamespace() {
        Map<String, Map<String, Object>> branches = new LinkedHashMap<>();
        branches.put("a", Map.of("k", "a-value", "onlyA", 1));
        branches.put("b", Map.of("k", "b-value", "onlyB", 2));

        Map<String, Object> merged = mergeContexts(Map.of(), branches);

        // 主上下文：k 取 a 的值，两个分支的唯一字段都进入主上下文
        assertThat(merged).containsEntry("k", "a-value");
        assertThat(merged).containsEntry("onlyA", 1);
        assertThat(merged).containsEntry("onlyB", 2);

        // 保险柜：a、b 完整存档（含冲突字段 b 的 k）
        @SuppressWarnings("unchecked")
        Map<String, Object> orderedBranches = (Map<String, Object>) merged.get("parallelBranches");
        assertThat(orderedBranches).containsEntry("a", Map.of("k", "a-value", "onlyA", 1));
        assertThat(orderedBranches).containsEntry("b", Map.of("k", "b-value", "onlyB", 2));
    }
}