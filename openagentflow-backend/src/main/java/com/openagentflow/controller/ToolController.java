package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.domain.tool.AgentToolBindingRequest;
import com.openagentflow.domain.tool.AgentToolBindingSummary;
import com.openagentflow.domain.tool.ToolDefinitionRequest;
import com.openagentflow.domain.tool.ToolDefinitionSummary;
import com.openagentflow.domain.tool.ToolExecutionResult;
import com.openagentflow.domain.tool.ToolTestRequest;
import com.openagentflow.service.ToolService;
import jakarta.validation.Valid;
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
 * 工具中心与 Agent 工具绑定接口。
 */
@RestController
@RequestMapping
public class ToolController {

    /** 工具中心应用服务。 */
    private final ToolService toolService;

    public ToolController(ToolService toolService) {
        this.toolService = toolService;
    }

    /**
     * 查询工具列表。
     *
     * @return 工具摘要列表
     */
    @GetMapping("/tools")
    public ApiResponse<List<ToolDefinitionSummary>> listTools() {
        return ApiResponse.ok(toolService.listTools());
    }

    /**
     * 查询工具详情。
     *
     * @param id 工具 ID
     * @return 工具详情
     */
    @GetMapping("/tools/{id}")
    public ApiResponse<ToolDefinitionSummary> getTool(@PathVariable String id) {
        return ApiResponse.ok(toolService.getTool(id));
    }

    /**
     * 创建工具。
     *
     * @param request 保存请求
     * @return 工具详情
     */
    @PostMapping("/tools")
    public ApiResponse<ToolDefinitionSummary> createTool(@Valid @RequestBody ToolDefinitionRequest request) {
        return ApiResponse.ok(toolService.createTool(request));
    }

    /**
     * 更新工具。
     *
     * @param id 工具 ID
     * @param request 保存请求
     * @return 工具详情
     */
    @PutMapping("/tools/{id}")
    public ApiResponse<ToolDefinitionSummary> updateTool(@PathVariable String id,
                                                         @Valid @RequestBody ToolDefinitionRequest request) {
        return ApiResponse.ok(toolService.updateTool(id, request));
    }

    /**
     * 删除工具。
     *
     * @param id 工具 ID
     * @return 空响应
     */
    @DeleteMapping("/tools/{id}")
    public ApiResponse<Void> deleteTool(@PathVariable String id) {
        toolService.deleteTool(id);
        return ApiResponse.ok(null);
    }

    /**
     * 测试工具。
     *
     * @param id 工具 ID
     * @param request 测试请求
     * @return 执行结果
     */
    @PostMapping("/tools/{id}/test")
    public ApiResponse<ToolExecutionResult> testTool(@PathVariable String id,
                                                     @RequestBody ToolTestRequest request) {
        return ApiResponse.ok(toolService.testTool(id, request));
    }

    /**
     * 查询 Agent 工具绑定。
     *
     * @param agentId Agent ID
     * @return 绑定列表
     */
    @GetMapping("/agents/{agentId}/tools")
    public ApiResponse<List<AgentToolBindingSummary>> listAgentToolBindings(@PathVariable String agentId) {
        return ApiResponse.ok(toolService.listAgentToolBindings(agentId));
    }

    /**
     * 保存 Agent 工具绑定。
     *
     * @param agentId Agent ID
     * @param request 绑定请求
     * @return 绑定列表
     */
    @PutMapping("/agents/{agentId}/tools")
    public ApiResponse<List<AgentToolBindingSummary>> saveAgentToolBindings(@PathVariable String agentId,
                                                                            @RequestBody AgentToolBindingRequest request) {
        return ApiResponse.ok(toolService.saveAgentToolBindings(agentId, request));
    }
}
