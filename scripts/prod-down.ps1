param(
    [string]$EnvFile = ".env.prod"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $root $EnvFile

Push-Location $root
try {
    docker compose --env-file $envPath -f docker-compose.prod.yml down
} finally {
    Pop-Location
}

