param(
    [string]$JavaHome = "D:\kfhj\jdk\jdk-21.0.11",
    [string]$MavenHome = "D:\kfhj\maven\apache-maven-3.9.16",
    [string]$MavenRepo = "D:\kfhj\maven\mavenopenagent"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $root "openagentflow-backend"
$frontendDir = Join-Path $root "openagentflow-frontend"

$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;$MavenHome\bin;$env:Path"

Push-Location $backendDir
try {
    # 后端使用指定 JDK 和 Maven 仓库编译，和本地开发环境保持一致。
    & (Join-Path $MavenHome "bin\mvn.cmd") "-Dmaven.repo.local=$MavenRepo" -DskipTests compile
} finally {
    Pop-Location
}

Push-Location $frontendDir
try {
    # 前端执行 TypeScript 类型检查和 Vite 生产构建。
    npm run build
} finally {
    Pop-Location
}

Write-Host "Backend compile and frontend build completed."
