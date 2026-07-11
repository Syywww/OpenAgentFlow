param(
  [string]$MysqlExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
  [string]$MysqlDumpExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe",
  [string]$HostName = "localhost",
  [string]$UserName = "root",
  [string]$Password = "123456",
  [string]$Database = "openagentflow",
  [string]$BackupDirectory = ".\backups\dr"
)

$ErrorActionPreference = "Stop"
$startedAt = Get-Date
$stamp = $startedAt.ToString("yyyyMMdd-HHmmss")
$backupRoot = [System.IO.Path]::GetFullPath($BackupDirectory)
New-Item -ItemType Directory -Force -Path $backupRoot | Out-Null
$dumpFile = Join-Path $backupRoot "$Database-$stamp.sql"
$checksumFile = "$dumpFile.sha256"
$restoreDatabase = "${Database}_dr_${stamp}" -replace '-', '_'

# 生成一致性MySQL逻辑备份，single-transaction避免长时间锁表。
& $MysqlDumpExe "--host=$HostName" "--user=$UserName" "--password=$Password" --single-transaction --routines --events --triggers --set-gtid-purged=OFF $Database | Set-Content -Encoding utf8 $dumpFile
if ($LASTEXITCODE -ne 0) { throw "MySQL备份失败" }
$checksum = (Get-FileHash -Algorithm SHA256 -LiteralPath $dumpFile).Hash.ToLowerInvariant()
Set-Content -Encoding ascii -Path $checksumFile -Value "$checksum  $([System.IO.Path]::GetFileName($dumpFile))"

# 恢复到隔离临时库，并执行关键表冒烟查询，真实测量RTO。
& $MysqlExe "--host=$HostName" "--user=$UserName" "--password=$Password" -e "CREATE DATABASE ``$restoreDatabase`` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
Get-Content -Raw -LiteralPath $dumpFile | & $MysqlExe "--host=$HostName" "--user=$UserName" "--password=$Password" $restoreDatabase
if ($LASTEXITCODE -ne 0) { throw "MySQL恢复失败" }
$smoke = & $MysqlExe "--host=$HostName" "--user=$UserName" "--password=$Password" --batch --skip-column-names $restoreDatabase -e "SELECT COUNT(1) FROM agent; SELECT COUNT(1) FROM knowledge_base; SELECT COUNT(1) FROM async_task;"
$finishedAt = Get-Date
$rtoSeconds = [int]($finishedAt - $startedAt).TotalSeconds

# 临时库仅用于演练，不保留业务数据；备份与校验文件继续保留供审计。
& $MysqlExe "--host=$HostName" "--user=$UserName" "--password=$Password" -e "DROP DATABASE ``$restoreDatabase``;"
[pscustomobject]@{ Status = "success"; Backup = $dumpFile; Sha256 = $checksum; RtoSeconds = $rtoSeconds; Smoke = ($smoke -join ',') } | ConvertTo-Json
