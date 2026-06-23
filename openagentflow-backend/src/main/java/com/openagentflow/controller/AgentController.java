package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.domain.agent.AgentDetail;
import com.openagentflow.domain.agent.AgentPublishRequest;
import com.openagentflow.domain.agent.AgentRequest;
import com.openagentflow.domain.agent.AgentSummary;
import com.openagentflow.domain.chat.ChatCompletionRequest;
import com.openagentflow.domain.chat.ChatCompletionResponse;
import com.openagentflow.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Agent 管理接口。
 */
@RestController
@RequestMapping("/agents")
public class AgentController {

    /** Agent 应用服务。 */
    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * 查询当前用户可见的 Agent 列表。
     *
     * @return Agent 摘要列表
     */
    @GetMapping
    public ApiResponse<List<AgentSummary>> listAgents() {
        return ApiResponse.ok(agentService.listAgents());
    }

    /**
     * 查询 Agent 详情。
     *
     * @param id Agent ID
     * @return Agent 详情
     */
    @GetMapping("/{id}")
    public ApiResponse<AgentDetail> getAgent(@PathVariable String id) {
        return ApiResponse.ok(agentService.getAgent(id));
    }

    /**
     * 创建 Agent。
     *
     * @param request 保存请求
     * @return 创建后的 Agent 详情
     */
    @PostMapping
    public ApiResponse<AgentDetail> createAgent(@Valid @RequestBody AgentRequest request) {
        return ApiResponse.ok(agentService.createAgent(request));
    }

    /**
     * 更新 Agent。
     *
     * @param id Agent ID
     * @param request 保存请求
     * @return 更新后的 Agent 详情
     */
    @PutMapping("/{id}")
    public ApiResponse<AgentDetail> updateAgent(@PathVariable String id,
                                                @Valid @RequestBody AgentRequest request) {
        return ApiResponse.ok(agentService.updateAgent(id, request));
    }

    /**
     * 发布 Agent。
     *
     * @param id Agent ID
     * @param request 发布请求
     * @return 发布后的 Agent 详情
     */
    @PostMapping("/{id}/publish")
    public ApiResponse<AgentDetail> publishAgent(@PathVariable String id,
                                                 @RequestBody AgentPublishRequest request) {
        return ApiResponse.ok(agentService.publishAgent(id, request));
    }

    /**
     * 复制 Agent。
     *
     * @param id 来源 Agent ID
     * @return 复制后的 Agent 详情
     */
    @PostMapping("/{id}/copy")
    public ApiResponse<AgentDetail> copyAgent(@PathVariable String id) {
        return ApiResponse.ok(agentService.copyAgent(id));
    }

    /**
     * 删除 Agent。
     *
     * @param id Agent ID
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAgent(@PathVariable String id) {
        agentService.deleteAgent(id);
        return ApiResponse.ok(null);
    }

    /**
     * 运行 Agent 非流式调试。
     *
     * @param id Agent ID
     * @param request 聊天请求
     * @return 聊天响应
     */
    @PostMapping("/{id}/run")
    public ApiResponse<ChatCompletionResponse> runAgent(@PathVariable String id,
                                                        @Valid @RequestBody ChatCompletionRequest request) {
        return ApiResponse.ok(agentService.runAgent(id, request));
    }

    /**
     * 运行 Agent SSE 流式调试。
     *
     * @param id Agent ID
     * @param request 聊天请求
     * @return SSE 发射器
     */
    @PostMapping(value = "/{id}/run/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter runAgentStream(@PathVariable String id,
                                     @Valid @RequestBody ChatCompletionRequest request) {
        return agentService.runAgentStream(id, request);
    }
}
