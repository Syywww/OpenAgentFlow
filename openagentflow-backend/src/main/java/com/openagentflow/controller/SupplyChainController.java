package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.service.SupplyChainService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/** 软件供应链治理接口。 */
@RestController
@RequestMapping("/governance/supply-chain")
public class SupplyChainController {
    private final SupplyChainService service;
    public SupplyChainController(SupplyChainService service) { this.service = service; }
    /** 查询制品证明。 */
    @GetMapping public ApiResponse<List<Map<String,Object>>> list() { return ApiResponse.ok(service.list()); }
    /** 接收CI制品准入结果。 */
    @PostMapping("/attest")
    public ApiResponse<Map<String,Object>> attest(@RequestBody AttestationRequest request) {
        return ApiResponse.ok(service.attest(request.name,request.version,request.digest,request.sbomUri,request.signatureUri,
                request.criticalCount,request.highCount,request.licenseStatus,request.secretScanStatus));
    }
    /** CI制品证明请求。 */
    public static class AttestationRequest {
        /** 制品名称。 */ public String name;
        /** 制品版本。 */ public String version;
        /** SHA256摘要。 */ public String digest;
        /** SBOM地址。 */ public String sbomUri;
        /** 签名地址。 */ public String signatureUri;
        /** 严重漏洞数。 */ public int criticalCount;
        /** 高危漏洞数。 */ public int highCount;
        /** 许可证检查状态。 */ public String licenseStatus;
        /** 密钥扫描状态。 */ public String secretScanStatus;
    }
}
