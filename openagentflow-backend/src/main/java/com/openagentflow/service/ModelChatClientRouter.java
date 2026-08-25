package com.openagentflow.service;

import com.openagentflow.domain.chat.ChatRunContext;
import com.openagentflow.domain.chat.ModelChatClient;
import com.openagentflow.entity.ModelProviderEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * 模型聊天客户端路由器。
 *
 * <p>按服务商 {@code provider_type} 分发到对应协议实现：
 * {@code spark} 走讯飞星火 WebSocket 适配器，其余（openai_compatible / ollama 等）
 * 统一走 OpenAI-compatible 客户端。新增协议只需实现 {@link ModelChatClient} 并在此注册。</p>
 */
@Component
public class ModelChatClientRouter {

    /** OpenAI-compatible 客户端。 */
    private final OpenAiCompatibleClient openAiCompatibleClient;

    /** 讯飞星火 WebSocket 客户端。 */
    private final SparkChatClient sparkChatClient;

    public ModelChatClientRouter(OpenAiCompatibleClient openAiCompatibleClient,
                                 SparkChatClient sparkChatClient) {
        this.openAiCompatibleClient = openAiCompatibleClient;
        this.sparkChatClient = sparkChatClient;
    }

    /**
     * 按模型服务商选择聊天客户端。
     *
     * @param provider 模型服务商
     * @return 对应的聊天客户端
     */
    public ModelChatClient route(ModelProviderEntity provider) {
        if (provider != null && "spark".equalsIgnoreCase(String.valueOf(provider.getProviderType()))) {
            return sparkChatClient;
        }
        return openAiCompatibleClient;
    }

    /**
     * 按聊天运行上下文选择聊天客户端。
     *
     * @param context 聊天运行上下文
     * @return 对应的聊天客户端
     */
    public ModelChatClient route(ChatRunContext context) {
        if (context == null) {
            return openAiCompatibleClient;
        }
        return route(context.getProvider());
    }

    /**
     * 判断服务商是否走讯飞星火适配器。
     *
     * @param provider 模型服务商
     * @return true 表示星火
     */
    public static boolean isSpark(ModelProviderEntity provider) {
        return provider != null && "spark".equalsIgnoreCase(StringUtils.trimWhitespace(
                String.valueOf(provider.getProviderType())));
    }

    /**
     * 取消所有协议客户端中指定 Runtime 运行的活动调用。
     *
     * @param runId 运行 ID
     * @return 任一客户端找到活动调用即返回 true
     */
    public boolean cancel(String runId) {
        boolean cancelled = openAiCompatibleClient.cancel(runId);
        if (sparkChatClient.cancel(runId)) {
            cancelled = true;
        }
        return cancelled;
    }

    /**
     * 返回当前 JVM 所有协议客户端的活动运行 ID 快照。
     *
     * @return 活动运行 ID 集合
     */
    public Set<String> activeRunIds() {
        Set<String> ids = new HashSet<>(openAiCompatibleClient.activeRunIds());
        ids.addAll(sparkChatClient.activeRunIds());
        return Set.copyOf(ids);
    }
}