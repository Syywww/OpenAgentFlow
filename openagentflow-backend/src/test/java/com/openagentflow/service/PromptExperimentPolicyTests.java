package com.openagentflow.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Prompt实验分流与自动选优规则测试。 */
class PromptExperimentPolicyTests {

    /** 同一分桶键必须稳定命中同一变体，避免用户在多轮对话间跳组。 */
    @Test
    void shouldRouteSameKeyToStableVariant() {
        List<PromptExperimentPolicy.WeightedVariant> variants = List.of(
                new PromptExperimentPolicy.WeightedVariant("A", 50D),
                new PromptExperimentPolicy.WeightedVariant("B", 50D)
        );

        String first = PromptExperimentPolicy.selectVariant("session-1001", variants);
        String second = PromptExperimentPolicy.selectVariant("session-1001", variants);

        assertThat(first).isEqualTo(second).isIn("A", "B");
    }

    /** 达到最小样本后应按质量优先、成功率次优选择胜出变体。 */
    @Test
    void shouldSelectWinnerAfterMinimumSamples() {
        List<PromptExperimentPolicy.Candidate> candidates = List.of(
                new PromptExperimentPolicy.Candidate("A", 80, 91D, 0.96D),
                new PromptExperimentPolicy.Candidate("B", 85, 86D, 0.99D)
        );

        assertThat(PromptExperimentPolicy.selectWinner(candidates, 30)).isEqualTo("A");
        assertThat(PromptExperimentPolicy.selectWinner(candidates, 100)).isNull();
    }
}
