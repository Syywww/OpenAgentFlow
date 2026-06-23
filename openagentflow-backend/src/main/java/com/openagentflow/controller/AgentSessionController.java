package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.domain.session.AgentMessageSummary;
import com.openagentflow.domain.session.AgentSessionCreateRequest;
import com.openagentflow.domain.session.AgentSessionSummary;
import com.openagentflow.domain.session.AgentSessionUpdateRequest;
import com.openagentflow.service.AgentSessionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Agent 历史会话接口。
 */
@RestController
@RequestMapping("/agents/{agentId}/sessions")
public class AgentSessionController {

    /** Agent 历史会话服务。 */
    private final AgentSessionService agentSessionService;

    public AgentSessionController(AgentSessionService agentSessionService) {
        this.agentSessionService = agentSessionService;
    }

    /**
     * 查询当前用户在指定 Agent 下的历史会话。
     *
     * @param agentId Agent ID
     * @return 会话列表
     */
    @GetMapping
    public ApiResponse<List<AgentSessionSummary>> listSessions(@PathVariable String agentId) {
        return ApiResponse.ok(agentSessionService.listSessions(agentId));
    }

    /**
     * 创建新会话。
     *
     * @param agentId Agent ID
     * @param request 创建请求
     * @return 新会话摘要
     */
    @PostMapping
    public ApiResponse<AgentSessionSummary> createSession(@PathVariable String agentId,
                                                          @RequestBody(required = false) AgentSessionCreateRequest request) {
        return ApiResponse.ok(agentSessionService.createSession(agentId, request));
    }

    /**
     * 更新会话标题或状态。
     *
     * @param agentId Agent ID
     * @param sessionId 会话 ID
     * @param request 更新请求
     * @return 会话摘要
     */
    @PutMapping("/{sessionId}")
    public ApiResponse<AgentSessionSummary> updateSession(@PathVariable String agentId,
                                                          @PathVariable String sessionId,
                                                          @RequestBody AgentSessionUpdateRequest request) {
        return ApiResponse.ok(agentSessionService.updateSession(agentId, sessionId, request));
    }

    /**
     * 删除会话。
     *
     * @param agentId Agent ID
     * @param sessionId 会话 ID
     * @return 空响应
     */
    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> deleteSession(@PathVariable String agentId,
                                           @PathVariable String sessionId) {
        agentSessionService.deleteSession(agentId, sessionId);
        return ApiResponse.ok(null);
    }

    /**
     * 查询会话消息列表。
     *
     * @param agentId Agent ID
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    @GetMapping("/{sessionId}/messages")
    public ApiResponse<List<AgentMessageSummary>> listMessages(@PathVariable String agentId,
                                                               @PathVariable String sessionId) {
        return ApiResponse.ok(agentSessionService.listMessages(agentId, sessionId));
    }
}
