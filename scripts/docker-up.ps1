$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root ".env"
$exampleFile = Join-Path $root ".env.example"

if (-not (Test-Path $envFile) -and (Test-Path $exampleFile)) {
    # 首次运行时自动复制示例配置，真实 API Key 仍由用户自行在界面或数据库中配置。
    Copy-Item -Path $exampleFile -Destination $envFile
}

Push-Location $root
try {
    docker compose up -d --build
    Write-Host "OpenAgentFlow Docker stack is starting."
    Write-Host "Frontend: http://localhost:5173"
    Write-Host "Backend:  http://localhost:8080/api"
} finally {
    Pop-Location
}
