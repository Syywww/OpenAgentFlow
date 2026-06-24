package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.domain.agent.AgentTeamDtos;
import com.openagentflow.service.AgentTeamService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 多 Agent 协作团队接口。
 */
@RestController
public class AgentTeamController {

    /** 多 Agent 协作团队服务。 */
    private final AgentTeamService agentTeamService;

    public AgentTeamController(AgentTeamService agentTeamService) {
        this.agentTeamService = agentTeamService;
    }

    /**
     * 查询协作团队列表。
     *
     * @return 协作团队摘要列表
     */
    @GetMapping("/agent-teams")
    public ApiResponse<List<AgentTeamDtos.TeamSummary>> listTeams() {
        return ApiResponse.ok(agentTeamService.listTeams());
    }

    /**
     * 查询协作团队详情。
     *
     * @param id 团队 ID
     * @return 协作团队详情
     */
    @GetMapping("/agent-teams/{id}")
    public ApiResponse<AgentTeamDtos.TeamDetail> getTeam(@PathVariable String id) {
        return ApiResponse.ok(agentTeamService.getTeam(id));
    }

    /**
     * 创建协作团队。
     *
     * @param request 保存请求
     * @return 创建后的协作团队详情
     */
    @PostMapping("/agent-teams")
    public ApiResponse<AgentTeamDtos.TeamDetail> createTeam(@RequestBody AgentTeamDtos.TeamRequest request) {
        return ApiResponse.ok(agentTeamService.createTeam(request));
    }

    /**
     * 更新协作团队。
     *
     * @param id 团队 ID
     * @param request 保存请求
     * @return 更新后的协作团队详情
     */
    @PutMapping("/agent-teams/{id}")
    public ApiResponse<AgentTeamDtos.TeamDetail> updateTeam(@PathVariable String id,
                                                            @RequestBody AgentTeamDtos.TeamRequest request) {
        return ApiResponse.ok(agentTeamService.updateTeam(id, request));
    }

    /**
     * 发布协作团队。
     *
     * @param id 团队 ID
     * @return 发布后的协作团队详情
     */
    @PostMapping("/agent-teams/{id}/publish")
    public ApiResponse<AgentTeamDtos.TeamDetail> publishTeam(@PathVariable String id) {
        return ApiResponse.ok(agentTeamService.publishTeam(id));
    }

    /**
     * 删除协作团队。
     *
     * @param id 团队 ID
     * @return 空响应
     */
    @DeleteMapping("/agent-teams/{id}")
    public ApiResponse<Void> deleteTeam(@PathVariable String id) {
        agentTeamService.deleteTeam(id);
        return ApiResponse.ok(null);
    }

    /**
     * 运行一次多 Agent 协作。
     *
     * @param id 团队 ID
     * @param request 运行请求
     * @return 协作运行结果
     */
    @PostMapping("/agent-teams/{id}/run")
    public ApiResponse<AgentTeamDtos.RunResult> runTeam(@PathVariable String id,
                                                        @RequestBody AgentTeamDtos.RunRequest request) {
        return ApiResponse.ok(agentTeamService.runTeam(id, request));
    }
}
