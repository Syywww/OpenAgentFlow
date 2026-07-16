param([switch]$Check)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$source = Join-Path $root 'openagentflow-sql\mysql'
$target = Join-Path $root 'openagentflow-backend\src\main\resources\db\migration'
$files = @(Get-ChildItem -LiteralPath $source -Filter 'V*.sql' | Sort-Object Name)

if ($Check) {
    $problems = @()
    foreach ($file in $files) {
        $copy = Join-Path $target $file.Name
        if (-not (Test-Path -LiteralPath $copy)) {
            $problems += "Missing: $($file.Name)"
        } else {
            $sourceHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $file.FullName).Hash
            $targetHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $copy).Hash
            if ($sourceHash -ne $targetHash) {
                $problems += "Content mismatch: $($file.Name)"
            }
        }
    }
    $extra = @(Get-ChildItem -LiteralPath $target -Filter 'V*.sql' -ErrorAction SilentlyContinue |
        Where-Object { -not (Test-Path -LiteralPath (Join-Path $source $_.Name)) })
    foreach ($file in $extra) {
        $problems += "Unexpected: $($file.Name)"
    }
    if ($problems.Count -gt 0) {
        $problems | ForEach-Object { Write-Error $_ }
        exit 1
    }
    Write-Host "Flyway migration copies match the canonical SQL directory: $($files.Count) files."
    exit 0
}

New-Item -ItemType Directory -Path $target -Force | Out-Null
Get-ChildItem -LiteralPath $target -Filter 'V*.sql' -ErrorAction SilentlyContinue | Remove-Item -Force
foreach ($file in $files) {
    Copy-Item -LiteralPath $file.FullName -Destination (Join-Path $target $file.Name)
}
Write-Host "Synced $($files.Count) Flyway migration files."
