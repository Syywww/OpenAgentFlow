package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.domain.chat.ChatCompletionRequest;
import com.openagentflow.domain.chat.ChatCompletionResponse;
import com.openagentflow.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 聊天调试接口。
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    /** 聊天调试服务。 */
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 普通非流式聊天补全。
     *
     * @param request 聊天补全请求
     * @return 聊天补全响应
     */
    @PostMapping("/completions")
    public ApiResponse<ChatCompletionResponse> complete(@Valid @RequestBody ChatCompletionRequest request) {
        return ApiResponse.ok(chatService.complete(request));
    }

    /**
     * SSE 流式聊天补全。
     *
     * @param request 聊天补全请求
     * @return SSE 发射器
     */
    @PostMapping(value = "/completions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter completeStream(@Valid @RequestBody ChatCompletionRequest request) {
        return chatService.completeStream(request);
    }
}
