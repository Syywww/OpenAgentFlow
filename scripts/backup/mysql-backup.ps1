param(
    [string]$MysqlDumpExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe",
    [string]$HostName = "localhost",
    [int]$Port = 3306,
    [string]$User = "root",
    [string]$Database = "openagentflow",
    [Parameter(Mandatory = $true)][string]$OutputDirectory
)

$ErrorActionPreference = "Stop"
$resolvedDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Force -Path $resolvedDirectory | Out-Null
$target = Join-Path $resolvedDirectory ("openagentflow-" + (Get-Date -Format "yyyyMMdd-HHmmss") + ".sql")

# 密码通过MYSQL_PWD临时环境变量传入，避免出现在命令行历史中。
if (-not $env:MYSQL_PWD) {
    throw "请先设置 MYSQL_PWD 环境变量"
}
& $MysqlDumpExe --host=$HostName --port=$Port --user=$User --single-transaction --routines --triggers --events --set-gtid-purged=OFF --default-character-set=utf8mb4 $Database | Set-Content -Encoding utf8 $target
if ($LASTEXITCODE -ne 0) { throw "MySQL备份失败" }
Write-Output $target
