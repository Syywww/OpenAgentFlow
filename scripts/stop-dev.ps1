param(
    [int[]]$Ports = @(8080, 5173)
)

$ErrorActionPreference = "Stop"

foreach ($port in $Ports) {
    $processIds = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue |
        Where-Object { $_.OwningProcess -gt 0 } |
        Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($processId in $processIds) {
        # 根据端口停止本地开发进程，避免误杀其他 Java 或 Node 程序。
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
        Write-Host "Stopped process $processId on port $port"
    }
}
