param(
    [string]$Output = "artifacts/dr/ha-failover.json",
    [switch]$Execute
)

$ErrorActionPreference = "Stop"
$targets = @(
    @{ component = "mysql"; rpoSeconds = 300; rtoSeconds = 900; action = "切换只读副本并核对事务位点" },
    @{ component = "redis"; rpoSeconds = 60; rtoSeconds = 300; action = "触发 Sentinel 选主并刷新客户端拓扑" },
    @{ component = "kafka"; rpoSeconds = 0; rtoSeconds = 300; action = "停止 Broker 并检查副本重新选主" },
    @{ component = "minio"; rpoSeconds = 300; rtoSeconds = 900; action = "切换备用端点并核对对象清单" },
    @{ component = "milvus"; rpoSeconds = 900; rtoSeconds = 1800; action = "恢复集合并原子切换 Alias" }
)

# 默认只生成演练矩阵；显式传入 Execute 后由生产流水线适配各组件实际故障注入命令。
$result = @{
    mode = if ($Execute) { "execute" } else { "plan" }
    generatedAt = (Get-Date).ToString("o")
    targets = $targets
    evidenceRequired = @("故障开始时间", "业务恢复时间", "最后持久化位点", "数据一致性抽样", "告警到达时间")
}
New-Item -ItemType Directory -Force -Path (Split-Path $Output) | Out-Null
$result | ConvertTo-Json -Depth 8 | Set-Content -Path $Output -Encoding utf8
Write-Host "高可用故障切换矩阵已生成：$Output"
