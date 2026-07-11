package com.openagentflow.service;

/**
 * Embedding 下游达到并发或速率上限异常。
 * <p>该异常必须交给 Kafka 任务重试，不能降级为本地模拟向量。</p>
 */
public class EmbeddingBackpressureException extends RuntimeException {

    public EmbeddingBackpressureException(String message) {
        super(message);
    }
}
