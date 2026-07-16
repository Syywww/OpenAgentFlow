-- P35：演示客服工作流节点执行条件示例。
-- 目标：让知识检索和订单工具节点在工作流画布中直接展示可学习的节点级执行条件。

UPDATE workflow_node node_row
JOIN workflow_definition workflow_row ON workflow_row.id = node_row.workflow_id
SET node_row.config_json = JSON_SET(
      node_row.config_json,
      '$.runConditionEnabled', true,
      '$.runConditionMode', 'RUN_WHEN',
      '$.runConditionExpr', 'input contains 产品 || input contains 优惠券 || input contains 优惠卷 || input contains 活动 || input contains 价格 || input contains 会员 || input contains 退款 || input contains 政策 || input contains 服务'
    ),
    node_row.updated_at = CURRENT_TIMESTAMP(3)
WHERE workflow_row.workflow_code = 'demo-customer-service-flow'
  AND node_row.node_key = 'rag';

UPDATE workflow_node node_row
JOIN workflow_definition workflow_row ON workflow_row.id = node_row.workflow_id
SET node_row.config_json = JSON_SET(
      node_row.config_json,
      '$.runConditionEnabled', true,
      '$.runConditionMode', 'RUN_WHEN',
      '$.runConditionExpr', 'input contains OAF- || input contains 多少订单 || input contains 几个订单 || input contains 几笔订单 || input contains 订单数量 || input contains 订单数 || input contains 我的订单 || input contains 订单列表 || input contains 所有订单 || input contains 有哪些订单'
    ),
    node_row.updated_at = CURRENT_TIMESTAMP(3)
WHERE workflow_row.workflow_code = 'demo-customer-service-flow'
  AND node_row.node_key = 'tool';

UPDATE workflow_definition
SET graph_json = JSON_SET(
      graph_json,
      '$.nodes[1].data.config.runConditionEnabled', true,
      '$.nodes[1].data.config.runConditionMode', 'RUN_WHEN',
      '$.nodes[1].data.config.runConditionExpr', 'input contains 产品 || input contains 优惠券 || input contains 优惠卷 || input contains 活动 || input contains 价格 || input contains 会员 || input contains 退款 || input contains 政策 || input contains 服务',
      '$.nodes[2].data.config.runConditionEnabled', true,
      '$.nodes[2].data.config.runConditionMode', 'RUN_WHEN',
      '$.nodes[2].data.config.runConditionExpr', 'input contains OAF- || input contains 多少订单 || input contains 几个订单 || input contains 几笔订单 || input contains 订单数量 || input contains 订单数 || input contains 我的订单 || input contains 订单列表 || input contains 所有订单 || input contains 有哪些订单'
    ),
    updated_at = CURRENT_TIMESTAMP(3)
WHERE workflow_code = 'demo-customer-service-flow';

UPDATE workflow_version version_row
JOIN workflow_definition workflow_row ON workflow_row.id = version_row.workflow_id
SET version_row.graph_json = workflow_row.graph_json
WHERE workflow_row.workflow_code = 'demo-customer-service-flow'
  AND version_row.version_no = 'v1';
