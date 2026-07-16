USE openagentflow;

-- P72兼容升级：为启用租户拦截前创建的评测、Prompt和工具确认数据回填可信工作空间。

SET @default_workspace_id = (
  SELECT id FROM oaf_workspace WHERE status='enabled' ORDER BY created_at,id LIMIT 1
);

UPDATE prompt_template p
SET p.workspace_id=COALESCE((SELECT wm.workspace_id FROM oaf_workspace_member wm
  WHERE wm.user_id=p.owner_user_id AND wm.status='enabled' ORDER BY wm.joined_at,wm.id LIMIT 1),@default_workspace_id)
WHERE p.workspace_id IS NULL;

UPDATE eval_task t
SET t.workspace_id=COALESCE((SELECT wm.workspace_id FROM oaf_workspace_member wm
  WHERE wm.user_id=t.created_by AND wm.status='enabled' ORDER BY wm.joined_at,wm.id LIMIT 1),@default_workspace_id)
WHERE t.workspace_id IS NULL;

UPDATE tool_confirm_request c
SET c.workspace_id=COALESCE((SELECT wm.workspace_id FROM oaf_workspace_member wm
  WHERE wm.user_id=c.requester_user_id AND wm.status='enabled' ORDER BY wm.joined_at,wm.id LIMIT 1),@default_workspace_id)
WHERE c.workspace_id IS NULL;

-- 只有存在可用工作空间时才收紧非空约束，兼容尚未初始化组织空间的空环境。
DROP PROCEDURE IF EXISTS oaf_tenant_not_null_v044;
DELIMITER $$
CREATE PROCEDURE oaf_tenant_not_null_v044()
BEGIN
  IF @default_workspace_id IS NOT NULL THEN
    ALTER TABLE prompt_template MODIFY COLUMN workspace_id char(36) NOT NULL COMMENT '工作空间ID';
    ALTER TABLE eval_task MODIFY COLUMN workspace_id char(36) NOT NULL COMMENT '工作空间ID';
    ALTER TABLE tool_confirm_request MODIFY COLUMN workspace_id char(36) NOT NULL COMMENT '工作空间ID';
  END IF;
END$$
DELIMITER ;
CALL oaf_tenant_not_null_v044();
DROP PROCEDURE IF EXISTS oaf_tenant_not_null_v044;
