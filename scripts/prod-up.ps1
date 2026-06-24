param(
    [string]$EnvFile = ".env.prod",
    [switch]$Build
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $root $EnvFile

if (-not (Test-Path $envPath)) {
    throw "未找到 $envPath。请先复制 .env.prod.example 为 .env.prod，并替换所有 CHANGE_ME。"
}

$content = Get-Content -Raw -Path $envPath
if ($content -match "CHANGE_ME") {
    throw "$envPath 仍包含 CHANGE_ME，占位密钥不能用于生产启动。"
}

$args = @("compose", "--env-file", $envPath, "-f", "docker-compose.prod.yml", "up", "-d")
if ($Build) {
    $args += "--build"
}

Push-Location $root
try {
    docker @args
} finally {
    Pop-Location
}

