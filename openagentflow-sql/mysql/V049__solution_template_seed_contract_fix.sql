USE openagentflow;

-- P74补充：修复首批公开模板版本的兼容性声明，并确保样例版本均满足不可变发布契约。
UPDATE agent_template_version v
JOIN agent_template t ON t.id=v.template_id
SET v.compatibility_statement=COALESCE(NULLIF(v.compatibility_statement,''),
    'OpenAgentFlow-Java 0.1+，Java 21，MySQL 8，Redis 7，Milvus 2.4'),
    v.change_log=COALESCE(NULLIF(v.change_log,''),'提供可直接安装的基础解决方案包。')
WHERE t.id LIKE '74000000-%' AND v.version_no='1.0.0';
