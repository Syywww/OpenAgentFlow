param(
    [string]$BaseUrl = "http://localhost:8080/api",
    [string]$Token = "",
    [int[]]$Concurrency = @(100, 500, 1000),
    [string]$OutputDirectory = "artifacts/capacity"
)

$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null

# 依次执行三档并发基线，结果单独保存，便于容量回归比较与扩容预测。
foreach ($level in $Concurrency) {
    $env:OAF_BASE_URL = $BaseUrl
    $env:OAF_TOKEN = $Token
    $env:OAF_VUS = [string]$level
    $env:OAF_DURATION = if ($level -ge 1000) { "10m" } else { "5m" }
    $output = Join-Path $OutputDirectory "capacity-$level.json"
    & k6 run --summary-export $output (Join-Path $PSScriptRoot "runtime-capacity.js")
    if ($LASTEXITCODE -ne 0) { throw "并发 $level 的容量基线未通过" }
}
