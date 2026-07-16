package com.openagentflow.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** AI黄金评测基线回归判定规则。 */
public final class EvaluationRegressionPolicy {

    /** 数值越低越好的指标。 */
    private static final Set<String> LOWER_IS_BETTER = Set.of(
            "tool_false_call_rate", "memory_duplicate_rate", "memory_conflict_rate",
            "hallucination_rate", "failure_rate", "latency_p95");

    private EvaluationRegressionPolicy() { }

    /** 比较候选指标与黄金基线，返回超过容忍值的指标。 */
    public static Result compare(Map<String, Double> baseline,
                                 Map<String, Double> candidate,
                                 double maxRegression) {
        List<Map<String, Object>> regressions = new ArrayList<>();
        for (Map.Entry<String, Double> entry : baseline.entrySet()) {
            String metric = entry.getKey();
            double baselineValue = entry.getValue() == null ? 0D : entry.getValue();
            Double candidateValue = candidate.get(metric);
            if (candidateValue == null) {
                regressions.add(detail(metric, baselineValue, null, "候选评测缺少指标"));
                continue;
            }
            double regression = LOWER_IS_BETTER.contains(metric)
                    ? candidateValue - baselineValue : baselineValue - candidateValue;
            if (regression > Math.max(0D, maxRegression)) {
                regressions.add(detail(metric, baselineValue, candidateValue, "指标退化超过允许值"));
            }
        }
        return new Result(regressions.isEmpty(), List.copyOf(regressions));
    }

    /** 构造单项回归明细。 */
    private static Map<String, Object> detail(String metric,
                                              double baseline,
                                              Double candidate,
                                              String reason) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("metric", metric);
        result.put("baseline", baseline);
        result.put("candidate", candidate);
        result.put("reason", reason);
        return result;
    }

    /**
     * 回归比较结果。
     *
     * @param passed 是否通过
     * @param regressions 回归指标明细
     */
    public record Result(boolean passed, List<Map<String, Object>> regressions) { }
}
