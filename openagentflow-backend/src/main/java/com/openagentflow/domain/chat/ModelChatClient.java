package com.openagentflow.domain.chat;

import java.util.function.Consumer;

/**
 * 模型聊天客户端抽象，屏蔽不同供应商的协议差异。
 *
 * <p>当前有两类实现：{@code OpenAiCompatibleClient}（HTTP /chat/completions）
 * 与 {@code SparkChatClient}（讯飞星火 WebSocket + HMAC-SHA256 动态签名鉴权）。
 * 调用方通过 {@code ModelChatClientRouter} 按服务商类型分发，无需感知底层协议差异。</p>
 */
public interface ModelChatClient {

    /**
     * 普通非流式聊天补全。
     *
     * @param context 聊天运行上下文
     * @param temperature 温度参数，null 时使用供应商默认值
     * @param maxTokens 最大输出 Token 数，null 或非正数时使用供应商默认值
     * @return LLM 调用结果
     */
    LlmCallResult complete(ChatRunContext context, Double temperature, Integer maxTokens);

    /**
     * 携带工具定义执行非流式聊天补全。
     *
     * @param context 聊天运行上下文
     * @param temperature 温度参数
     * @param maxTokens 最大输出 Token 数
     * @return LLM 调用结果，可能包含 tool_calls
     */
    LlmCallResult completeWithTools(ChatRunContext context, Double temperature, Integer maxTokens);

    /**
     * 流式聊天补全，增量文本通过 onDelta 回调实时推送。
     *
     * @param context 聊天运行上下文
     * @param temperature 温度参数
     * @param maxTokens 最大输出 Token 数
     * @param onDelta 流式片段回调
     * @return LLM 调用结果
     */
    LlmCallResult completeStream(ChatRunContext context, Double temperature, Integer maxTokens,
                                 Consumer<String> onDelta);
}