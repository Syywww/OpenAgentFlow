param(
    [string]$MysqlHost = "127.0.0.1",
    [int]$MysqlPort = 3306,
    [string]$Database = "openagentflow",
    [string]$User = "root",
    [string]$Password = "123456",
    [string]$MysqlExe = "",
    [string]$DemoApiKey = ""
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$reliabilitySqlFile = Join-Path $root "openagentflow-sql\mysql\V028__workflow_execution_reliability_final.sql"
$sqlFile = Join-Path $root "openagentflow-sql\mysql\V029__demo_data_package.sql"
$demoEnhancementSqlFiles = @(
    (Join-Path $root "openagentflow-sql\mysql\V030__customer_service_intent_guard_coupon_policy.sql"),
    (Join-Path $root "openagentflow-sql\mysql\V031__customer_service_product_policy.sql"),
    (Join-Path $root "openagentflow-sql\mysql\V032__demo_workflow_node_conditions.sql"),
    (Join-Path $root "openagentflow-sql\mysql\V033__demo_order_summary_tool_intent.sql")
)

function Resolve-MysqlClient {
    param([string]$ExplicitPath)

    if ($ExplicitPath -and (Test-Path $ExplicitPath)) {
        return (Resolve-Path $ExplicitPath).Path
    }

    $command = Get-Command "mysql.exe" -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    throw "mysql.exe was not found. Install MySQL Client or pass the full mysql.exe path with -MysqlExe."
}

function New-MysqlArgs {
    param([string]$SqlText)

    # Pass MySQL arguments as an array to avoid PowerShell string re-parsing.
    $args = @(
        "--default-character-set=utf8mb4",
        "--host=$MysqlHost",
        "--port=$MysqlPort",
        "--user=$User"
    )
    if ($Password -ne "") {
        $args += "--password=$Password"
    }
    $args += $Database
    $args += "--execute=$SqlText"
    return $args
}

function Invoke-MysqlSql {
    param(
        [string]$Client,
        [string]$SqlText
    )

    $args = New-MysqlArgs -SqlText $SqlText
    & $Client @args
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL command failed. Exit code: $LASTEXITCODE"
    }
}

function Invoke-MysqlScalar {
    param(
        [string]$Client,
        [string]$SqlText
    )

    $args = @("--batch", "--skip-column-names") + (New-MysqlArgs -SqlText $SqlText)
    $output = & $Client @args
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL scalar query failed. Exit code: $LASTEXITCODE"
    }
    return ($output | Select-Object -First 1)
}

function Escape-SqlText {
    param([string]$Value)

    # Escape single quotes only. The API key is written to local DB, not repo files.
    return $Value.Replace("'", "''")
}

if (!(Test-Path $sqlFile)) {
    throw "Demo SQL file not found: $sqlFile"
}
if (!(Test-Path $reliabilitySqlFile)) {
    throw "Workflow reliability SQL file not found: $reliabilitySqlFile"
}
foreach ($enhancementFile in $demoEnhancementSqlFiles) {
    if (!(Test-Path $enhancementFile)) {
        throw "Demo enhancement SQL file not found: $enhancementFile"
    }
}

$mysql = Resolve-MysqlClient -ExplicitPath $MysqlExe
$reliabilityPath = (Resolve-Path $reliabilitySqlFile).Path.Replace("\", "/")
$sourcePath = (Resolve-Path $sqlFile).Path.Replace("\", "/")

Write-Host "Initializing OpenAgentFlow demo data..."
Write-Host "MySQL: $MysqlHost`:$MysqlPort/$Database"
Write-Host "Checking workflow reliability migration..."

$lockedByColumnCount = Invoke-MysqlScalar -Client $mysql -SqlText "select count(1) from information_schema.columns where table_schema = database() and table_name = 'workflow_run' and column_name = 'locked_by'"
if ([int]$lockedByColumnCount -eq 0) {
    Write-Host "Applying V028 workflow reliability migration..."
    Invoke-MysqlSql -Client $mysql -SqlText "source $reliabilityPath"
} else {
    Write-Host "V028 workflow reliability migration already exists."
}

Write-Host "SQL: $sourcePath"

Invoke-MysqlSql -Client $mysql -SqlText "source $sourcePath"

foreach ($enhancementFile in $demoEnhancementSqlFiles) {
    $enhancementPath = (Resolve-Path $enhancementFile).Path.Replace("\", "/")
    Write-Host "SQL: $enhancementPath"
    Invoke-MysqlSql -Client $mysql -SqlText "source $enhancementPath"
}

if ($DemoApiKey -and $DemoApiKey.Trim().Length -gt 0) {
    $trimmedKey = $DemoApiKey.Trim()
    $mask = if ($trimmedKey.Length -le 8) {
        "****"
    } else {
        $trimmedKey.Substring(0, 4) + "****" + $trimmedKey.Substring($trimmedKey.Length - 4)
    }
    $escapedKey = Escape-SqlText -Value $trimmedKey
    $escapedMask = Escape-SqlText -Value $mask
    $apiKeySql = @"
INSERT INTO model_api_key (
  id, provider_id, key_name, key_cipher, key_mask, status, quota_used, created_by
) VALUES (
  '91000000-0000-0000-0000-000000000901',
  '10000000-0000-0000-0000-000000000005',
  'demo-local-key',
  '$escapedKey',
  '$escapedMask',
  'enabled',
  0,
  '00000000-0000-0000-0000-000000000100'
) ON DUPLICATE KEY UPDATE
  key_cipher = VALUES(key_cipher),
  key_mask = VALUES(key_mask),
  status = 'enabled',
  updated_at = CURRENT_TIMESTAMP(3);
"@
    Invoke-MysqlSql -Client $mysql -SqlText $apiKeySql
    Write-Host "Local demo model key has been saved to the local database. The plain key is not printed."
} else {
    Write-Host "No -DemoApiKey was provided. Model key setup was skipped."
}

Write-Host "P33 demo data initialization completed."
Write-Host "Recommended prompt: order OAF-DEMO-1001 status and refund handling policy."
