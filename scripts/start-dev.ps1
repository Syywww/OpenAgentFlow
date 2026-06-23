param(
    [string]$JavaHome = "D:\kfhj\jdk\jdk-21.0.11",
    [string]$MavenHome = "D:\kfhj\maven\apache-maven-3.9.16",
    [string]$MavenRepo = "D:\kfhj\maven\mavenopenagent",
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 5173
)

$ErrorActionPreference = "Stop"

function Stop-PortProcess {
    param([int]$Port)
    $processIds = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue |
        Where-Object { $_.OwningProcess -gt 0 } |
        Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($processId in $processIds) {
        # 先释放旧端口，避免本地重复启动时新进程绑定失败。
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }
}

$root = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $root "openagentflow-backend"
$frontendDir = Join-Path $root "openagentflow-frontend"

Stop-PortProcess -Port $BackendPort
Stop-PortProcess -Port $FrontendPort

$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;$MavenHome\bin;$env:Path"

$backendLog = Join-Path $root "backend-dev.log"
$backendErr = Join-Path $root "backend-dev.err.log"
$frontendLog = Join-Path $root "frontend-dev.log"
$frontendErr = Join-Path $root "frontend-dev.err.log"

$milvusConnection = Get-NetTCPConnection -LocalPort 19530 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($milvusConnection) {
    # 检测到本机 Milvus 端口后启用真实 Milvus 客户端。
    $env:OAF_MILVUS_ENABLED = "true"
    Write-Host "Milvus detected on localhost:19530, backend will use Milvus."
} else {
    # 本地未启动 Milvus 时允许后端降级启动，知识库向量仍保留 MySQL 兜底记录。
    $env:OAF_MILVUS_ENABLED = "false"
    Write-Host "Milvus is not listening on localhost:19530, backend will start with MySQL vector fallback."
}

Start-Process -FilePath (Join-Path $MavenHome "bin\mvn.cmd") `
    -ArgumentList @("-Dmaven.repo.local=$MavenRepo", "spring-boot:run") `
    -WorkingDirectory $backendDir `
    -RedirectStandardOutput $backendLog `
    -RedirectStandardError $backendErr `
    -WindowStyle Hidden

Start-Process -FilePath "npm.cmd" `
    -ArgumentList @("run", "dev", "--", "--host", "0.0.0.0") `
    -WorkingDirectory $frontendDir `
    -RedirectStandardOutput $frontendLog `
    -RedirectStandardError $frontendErr `
    -WindowStyle Hidden

Write-Host "OpenAgentFlow local services are starting..."
Write-Host "Backend:  http://localhost:$BackendPort/api"
Write-Host "Frontend: http://localhost:$FrontendPort"
Write-Host "Backend log:  $backendLog"
Write-Host "Frontend log: $frontendLog"
