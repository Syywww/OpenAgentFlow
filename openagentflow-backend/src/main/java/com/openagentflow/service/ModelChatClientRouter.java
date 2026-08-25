package com.openagentflow.service;

import com.openagentflow.domain.chat.ChatRunContext;
import com.openagentflow.domain.chat.ModelChatClient;
import com.openagentflow.entity.ModelProviderEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
}