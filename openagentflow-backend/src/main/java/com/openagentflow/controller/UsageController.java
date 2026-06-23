package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.api.PageResult;
import com.openagentflow.domain.usage.UsageDtos;
import com.openagentflow.service.UsageCostService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * 成本与用量中心接口。
 */
@RestController
@RequestMapping("/usage")
public class UsageController {

    /** 成本与用量服务。 */
    private final UsageCostService usageCostService;

    public UsageController(UsageCostService usageCostService) {
        this.usageCostService = usageCostService;
    }

    /**
     * 查询成本中心聚合数据。
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 聚合数据
     */
    @GetMapping("/console")
    public ApiResponse<UsageDtos.ConsoleData> getConsole(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(usageCostService.getConsole(startDate, endDate));
    }

    /**
     * 查询用量总览。
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 总览数据
     */
    @GetMapping("/overview")
    public ApiResponse<UsageDtos.Overview> getOverview(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(usageCostService.getOverview(startDate, endDate));
    }

    /**
     * 查询每日用量趋势。
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 趋势列表
     */
    @GetMapping("/daily")
    public ApiResponse<List<UsageDtos.DailyUsage>> listDaily(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(usageCostService.listDailyUsage(startDate, endDate));
    }

    /**
     * 查询维度拆分。
     *
     * @param dimension 维度名称
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param limit 返回数量
     * @return 拆分列表
     */
    @GetMapping("/breakdown")
    public ApiResponse<List<UsageDtos.BreakdownItem>> listBreakdown(@RequestParam(defaultValue = "model") String dimension,
                                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                    @RequestParam(defaultValue = "10") Integer limit) {
        return ApiResponse.ok(usageCostService.listBreakdown(dimension, startDate, endDate, limit));
    }

    /**
     * 分页查询调用明细。
     *
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param providerId 服务商筛选
     * @param modelId 模型筛选
     * @param agentId Agent 筛选
     * @param keyword 关键词
     * @return 调用明细分页
     */
    @GetMapping("/calls")
    public ApiResponse<PageResult<UsageDtos.CallDetail>> listCalls(@RequestParam(defaultValue = "1") Integer pageNo,
                                                                   @RequestParam(defaultValue = "20") Integer pageSize,
                                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                   @RequestParam(required = false) String providerId,
                                                                   @RequestParam(required = false) String modelId,
                                                                   @RequestParam(required = false) String agentId,
                                                                   @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(usageCostService.listCallDetails(pageNo, pageSize, startDate, endDate, providerId, modelId, agentId, keyword));
    }

    /**
     * 导出调用明细 CSV。
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param providerId 服务商筛选
     * @param modelId 模型筛选
     * @param agentId Agent 筛选
     * @param keyword 关键词
     * @return CSV 文件响应
     */
    @GetMapping("/calls/export")
    public ResponseEntity<byte[]> exportCalls(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                             @RequestParam(required = false) String providerId,
                                             @RequestParam(required = false) String modelId,
                                             @RequestParam(required = false) String agentId,
                                             @RequestParam(required = false) String keyword) {
        PageResult<UsageDtos.CallDetail> page = usageCostService.listCallDetails(1, 1000, startDate, endDate, providerId, modelId, agentId, keyword);
        byte[] bytes = usageCostService.toCsv(page.getRecords()).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("openagentflow-usage-calls.csv", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(bytes);
    }

    /**
     * 按当前模型单价重算历史成本。
     *
     * @return 更新记录数
     */
    @PostMapping("/recalculate-costs")
    public ApiResponse<Integer> recalculateCosts() {
        return ApiResponse.ok(usageCostService.recalculateHistoricalCosts());
    }

    /**
     * 查询配额规则。
     *
     * @return 配额规则列表
     */
    @GetMapping("/quotas")
    public ApiResponse<List<UsageDtos.QuotaSummary>> listQuotas() {
        return ApiResponse.ok(usageCostService.listQuotas());
    }

    /**
     * 创建配额规则。
     *
     * @param request 保存请求
     * @return 配额摘要
     */
    @PostMapping("/quotas")
    public ApiResponse<UsageDtos.QuotaSummary> createQuota(@RequestBody UsageDtos.QuotaRequest request) {
        return ApiResponse.ok(usageCostService.createQuota(request));
    }

    /**
     * 更新配额规则。
     *
     * @param id 配额 ID
     * @param request 保存请求
     * @return 配额摘要
     */
    @PutMapping("/quotas/{id}")
    public ApiResponse<UsageDtos.QuotaSummary> updateQuota(@PathVariable String id,
                                                           @RequestBody UsageDtos.QuotaRequest request) {
        return ApiResponse.ok(usageCostService.updateQuota(id, request));
    }

    /**
     * 删除配额规则。
     *
     * @param id 配额 ID
     * @return 空响应
     */
    @DeleteMapping("/quotas/{id}")
    public ApiResponse<Void> deleteQuota(@PathVariable String id) {
        usageCostService.deleteQuota(id);
        return ApiResponse.ok(null);
    }
}
