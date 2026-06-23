package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.domain.mcp.McpDtos;
import com.openagentflow.service.McpServerService;
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
 * MCP Server 管理接口。
 */
@RestController
@RequestMapping("/mcp-servers")
public class McpServerController {

    /** MCP Server 应用服务。 */
    private final McpServerService mcpServerService;

    public McpServerController(McpServerService mcpServerService) {
        this.mcpServerService = mcpServerService;
    }

    /**
     * 查询 MCP Server 列表。
     *
     * @return MCP Server 摘要列表
     */
    @GetMapping
    public ApiResponse<List<McpDtos.ServerSummary>> listServers() {
        return ApiResponse.ok(mcpServerService.listServers());
    }

    /**
     * 查询 MCP Server 详情。
     *
     * @param id MCP Server ID
     * @return MCP Server 详情
     */
    @GetMapping("/{id}")
    public ApiResponse<McpDtos.ServerDetail> getServer(@PathVariable String id) {
        return ApiResponse.ok(mcpServerService.getServer(id));
    }

    /**
     * 创建 MCP Server。
     *
     * @param request 创建请求
     * @return 创建后的详情
     */
    @PostMapping
    public ApiResponse<McpDtos.ServerDetail> createServer(@Valid @RequestBody McpDtos.ServerRequest request) {
        return ApiResponse.ok(mcpServerService.createServer(request));
    }

    /**
     * 更新 MCP Server。
     *
     * @param id MCP Server ID
     * @param request 更新请求
     * @return 更新后的详情
     */
    @PutMapping("/{id}")
    public ApiResponse<McpDtos.ServerDetail> updateServer(@PathVariable String id,
                                                          @Valid @RequestBody McpDtos.ServerRequest request) {
        return ApiResponse.ok(mcpServerService.updateServer(id, request));
    }

    /**
     * 删除 MCP Server。
     *
     * @param id MCP Server ID
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteServer(@PathVariable String id) {
        mcpServerService.deleteServer(id);
        return ApiResponse.ok(null);
    }

    /**
     * 测试 MCP Server 连接。
     *
     * @param id MCP Server ID
     * @return 连接测试结果
     */
    @PostMapping("/{id}/test")
    public ApiResponse<McpDtos.ConnectionTestResult> testServer(@PathVariable String id) {
        return ApiResponse.ok(mcpServerService.testServer(id));
    }

    /**
     * 发现 MCP Server 能力并同步工具中心。
     *
     * @param id MCP Server ID
     * @return 发现结果
     */
    @PostMapping("/{id}/discover")
    public ApiResponse<McpDtos.DiscoveryResult> discoverServer(@PathVariable String id) {
        return ApiResponse.ok(mcpServerService.discoverServer(id));
    }

    /**
     * 查询 MCP Server 已发现能力。
     *
     * @param id MCP Server ID
     * @return 能力列表
     */
    @GetMapping("/{id}/capabilities")
    public ApiResponse<List<McpDtos.CapabilitySummary>> listCapabilities(@PathVariable String id) {
        return ApiResponse.ok(mcpServerService.listCapabilities(id));
    }
}
