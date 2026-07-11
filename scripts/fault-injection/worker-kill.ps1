param([Parameter(Mandatory = $true)][string]$Container)

# 强制终止一个Worker实例，用于检查Fencing Token、心跳接管和幂等提交。
docker kill $Container
