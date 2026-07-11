param([string]$Container = "openagentflow-kafka", [int]$Seconds = 30)

# 暂停Kafka以检查Outbox积压、恢复发送和任务重试是否符合预期。
docker pause $Container
Start-Sleep -Seconds $Seconds
docker unpause $Container
