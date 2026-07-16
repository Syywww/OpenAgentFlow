package com.openagentflow.service;

import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;

/**
 * Prompt 实验分流与自动选优策略。
 *
 * <p>该类只包含确定性算法，不依赖数据库，便于运行时、治理服务和测试复用同一套规则。</p>
 */
public final class PromptExperimentPolicy {

    private PromptExperimentPolicy() {
    }

    /**
     * 按稳定哈希和流量权重选择实验变体。
     *
     * @param routingKey 分流键，通常使用会话编号或用户编号
     * @param variants 带权重的实验变体
     * @return 命中的变体编号，无可用变体时返回 {@code null}
     */
    public static String selectVariant(String routingKey, List<WeightedVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        List<WeightedVariant> available = variants.stream()
                .filter(item -> item != null && StringUtils.hasText(item.id()) && item.weight() > 0D)
                .toList();
        if (available.isEmpty()) {
            return null;
        }
        double totalWeight = available.stream().mapToDouble(WeightedVariant::weight).sum();
        double target = stableBucket(routingKey) * totalWeight;
        double cumulative = 0D;
        for (WeightedVariant variant : available) {
            cumulative += variant.weight();
            if (target < cumulative) {
                return variant.id();
            }
        }
        return available.getLast().id();
    }

    /**
     * 在所有候选变体达到最小样本量后，按质量分和成功率选择胜出者。
     *
     * @param candidates 候选变体指标
     * @param minimumSamples 最小样本量
     * @return 胜出变体编号，样本不足时返回 {@code null}
     */
    public static String selectWinner(List<Candidate> candidates, long minimumSamples) {
        if (candidates == null || candidates.size() < 2) {
            return null;
        }
        long threshold = Math.max(1L, minimumSamples);
        if (candidates.stream().anyMatch(item -> item == null || item.sampleCount() < threshold)) {
            return null;
        }
        return candidates.stream()
                .max(Comparator.comparingDouble(Candidate::qualityScore)
                        .thenComparingDouble(Candidate::successRate)
                        .thenComparingLong(Candidate::sampleCount))
                .map(Candidate::id)
                .orElse(null);
    }

    /** 将任意分流键稳定映射到 [0, 1) 区间。 */
    private static double stableBucket(String routingKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(String.valueOf(routingKey).getBytes(StandardCharsets.UTF_8));
            long value = 0L;
            for (int index = 0; index < Long.BYTES; index++) {
                value = (value << 8) | (hash[index] & 0xffL);
            }
            return (value & Long.MAX_VALUE) / (double) Long.MAX_VALUE;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256", exception);
        }
    }

    /**
     * 带流量权重的实验变体。
     *
     * @param id 变体编号
     * @param weight 流量权重
     */
    public record WeightedVariant(String id, double weight) {
    }

    /**
     * 自动选优候选指标。
     *
     * @param id 变体编号
     * @param sampleCount 样本量
     * @param qualityScore 平均质量分
     * @param successRate 成功率
     */
    public record Candidate(String id, long sampleCount, double qualityScore, double successRate) {
    }
}
