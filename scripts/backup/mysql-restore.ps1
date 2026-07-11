param(
    [string]$MysqlExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    [string]$HostName = "localhost",
    [int]$Port = 3306,
    [string]$User = "root",
    [string]$Database = "openagentflow",
    [Parameter(Mandatory = $true)][string]$BackupFile
)

$ErrorActionPreference = "Stop"
$resolvedFile = [System.IO.Path]::GetFullPath($BackupFile)
if (-not (Test-Path -LiteralPath $resolvedFile)) { throw "备份文件不存在：$resolvedFile" }
if (-not $env:MYSQL_PWD) { throw "请先设置 MYSQL_PWD 环境变量" }

# 使用MySQL source命令恢复，避免PowerShell对大SQL文件整体加载到内存。
$sourcePath = $resolvedFile.Replace('\', '/')
& $MysqlExe --host=$HostName --port=$Port --user=$User --default-character-set=utf8mb4 $Database --execute="source $sourcePath"
if ($LASTEXITCODE -ne 0) { throw "MySQL恢复失败" }
