package com.openagentflow.service;

import com.openagentflow.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 软件制品SBOM、签名、漏洞与许可证准入服务。 */
@Service
public class SupplyChainService {

    /** JDBC工具。 */
    private final JdbcTemplate jdbcTemplate;

    public SupplyChainService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    /** 登记CI生成的制品证明。 */
    public Map<String, Object> attest(String name,
                                      String version,
                                      String digest,
                                      String sbomUri,
                                      String signatureUri,
                                      int criticalCount,
                                      int highCount,
                                      String licenseStatus,
                                      String secretScanStatus) {
        boolean passed = criticalCount == 0 && highCount == 0 && "passed".equalsIgnoreCase(licenseStatus)
                && "passed".equalsIgnoreCase(secretScanStatus) && sbomUri != null && signatureUri != null;
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO software_artifact_attestation
                  (id,artifact_name,artifact_version,artifact_digest,sbom_uri,signature_uri,vulnerability_status,
                   critical_count,high_count,license_status,secret_scan_status,provenance_json,status,created_at)
                VALUES (?,?,?,?,?,?, ?,?,?,?, ?,JSON_OBJECT('source','ci'),?,NOW(3))
                ON DUPLICATE KEY UPDATE sbom_uri=VALUES(sbom_uri),signature_uri=VALUES(signature_uri),
                  critical_count=VALUES(critical_count),high_count=VALUES(high_count),license_status=VALUES(license_status),
                  secret_scan_status=VALUES(secret_scan_status),status=VALUES(status)
                """, id, name, version, digest, sbomUri, signatureUri,
                criticalCount + highCount == 0 ? "passed" : "blocked", criticalCount, highCount,
                licenseStatus, secretScanStatus, passed ? "admitted" : "blocked");
        if (!passed) throw new BusinessException("ARTIFACT_SUPPLY_CHAIN_BLOCKED", "制品未通过漏洞、许可证、密钥或签名准入检查");
        return jdbcTemplate.queryForMap("SELECT * FROM software_artifact_attestation WHERE artifact_digest=?", digest);
    }

    /** 查询制品证明。 */
    public List<Map<String, Object>> list() {
        return jdbcTemplate.queryForList("SELECT * FROM software_artifact_attestation ORDER BY created_at DESC LIMIT 200");
    }
}
