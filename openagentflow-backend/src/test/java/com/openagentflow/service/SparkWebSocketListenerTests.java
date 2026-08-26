package com.openagentflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 讯飞星火 WebSocket 监听器帧解析测试。
 *
 * <p>直接向监听器喂响应帧，验证 WebSocket 流式收口最易错的解析逻辑：
 * 中间帧累积、status=2 收口、usage 统计、错误码中断、异常帧、onClose 兜底、取消中断。
 * 不依赖网络与真实密钥。</p>
 */
class SparkWebSocketListenerTests {

    private final ObjectMapper mapper = new ObjectMapper();
    private final WebSocket ws = Mockito.mock(WebSocket.class);

    private SparkChatClient.SparkWebSocketListener listener(Consumer<String> onDelta) {
        return new SparkChatClient.SparkWebSocketListener("{}", onDelta, mapper);
    }

    @Test
    void shouldCollectFramesAndCompleteOnDoneStatus() throws Exception {
        SparkChatClient.SparkWebSocketListener listener = listener(null);

        listener.onText(ws, frame(0, 0, "你好", false), true);
        assertThat(listener.done()).isNotDone();

        // 收口帧 status=2 且携带 usage。
        listener.onText(ws, frame(0, 2, "世界", true), true);
        assertThat(listener.done()).isCompletedWithValue("你好世界");
        assertThat(listener.promptTokens()).isEqualTo(10);
        assertThat(listener.completionTokens()).isEqualTo(5);
        assertThat(listener.totalTokens()).isEqualTo(15);
    }

    @Test
    void shouldInvokeOnDeltaPerContentFrame() throws Exception {
        List<String> deltas = new ArrayList<>();
        SparkChatClient.SparkWebSocketListener listener = listener(deltas::add);

        listener.onText(ws, frame(0, 0, "你好", false), true);
        listener.onText(ws, frame(0, 2, "世界", false), true);

        assertThat(deltas).containsExactly("你好", "世界");
    }

    @Test
    void shouldCompleteExceptionallyOnNonZeroHeaderCode() throws Exception {
        SparkChatClient.SparkWebSocketListener listener = listener(null);
        listener.onText(ws, frame(11200, 0, null, false), true);
        assertThat(listener.done()).isCompletedExceptionally();
    }

    @Test
    void shouldCompleteExceptionallyOnMalformedFrame() throws Exception {
        SparkChatClient.SparkWebSocketListener listener = listener(null);
        listener.onText(ws, "not-json{", true);
        assertThat(listener.done()).isCompletedExceptionally();
    }

    @Test
    void shouldCompleteWithCollectedContentOnCloseWithoutDoneStatus() throws Exception {
        SparkChatClient.SparkWebSocketListener listener = listener(null);
        listener.onText(ws, frame(0, 0, "部分", false), true);
        assertThat(listener.done()).isNotDone();

        // 服务端未发 status=2 直接关闭：按已收集内容收口，而非永久挂起。
        listener.onClose(ws, 1000, "normal");
        assertThat(listener.done()).isCompletedWithValue("部分");
    }

    @Test
    void shouldCompleteExceptionallyOnCancel() {
        SparkChatClient.SparkWebSocketListener listener = listener(null);
        listener.cancel();
        assertThat(listener.done()).isCompletedExceptionally();
    }

    /**
     * 构造一条星火响应帧。
     *
     * <p>注意 {@code ObjectNode.putObject} 是 put 语义（同名 key 直接替换），
     * 因此 payload 只能创建一次，choices 与 usage 都挂在同一节点下，否则后创建者覆盖先创建者。</p>
     */
    private String frame(int headerCode, int choiceStatus, String content, boolean withUsage) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.putObject("header").put("code", headerCode).put("status", choiceStatus);
        ObjectNode payload = root.putObject("payload");
        ObjectNode choices = payload.putObject("choices");
        choices.put("status", choiceStatus);
        ArrayNode text = choices.putArray("text");
        if (content != null) {
            ObjectNode item = text.addObject();
            item.put("content", content);
            item.put("role", "assistant");
        }
        if (withUsage) {
            ObjectNode usage = payload.putObject("usage").putObject("text");
            usage.put("prompt_tokens", 10);
            usage.put("completion_tokens", 5);
            usage.put("total_tokens", 15);
        }
        return mapper.writeValueAsString(root);
    }
}