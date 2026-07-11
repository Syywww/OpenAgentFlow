$ErrorActionPreference = "Stop"
$env:JAVA_HOME = 'D:\kfhj\jdk\jdk-21.0.11'
$env:Path = "$env:JAVA_HOME\bin;D:\kfhj\maven\apache-maven-3.9.16\bin;$env:Path"

# 生成CycloneDX JSON和XML SBOM，输出到后端target目录。
mvn -f openagentflow-backend/pom.xml "-Dmaven.repo.local=D:\kfhj\maven\mavenopenagent" org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom
if ($LASTEXITCODE -ne 0) { throw "SBOM生成失败" }
