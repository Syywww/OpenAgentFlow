-- P36：演示订单汇总工具意图增强。
-- 目标：让“我有多少订单、我的订单、订单列表”等问题走订单工具，而不是进入知识库拒答。

UPDATE tool_definition
SET description = '低风险只读 REST 工具，用于查询订单状态、物流单号、预计送达时间、订单数量和订单列表摘要。',
    request_schema = JSON_OBJECT('type','object','properties',JSON_OBJECT('orderId',JSON_OBJECT('type','string','description','订单号或完整用户问题'))),
    response_schema = JSON_OBJECT('type','object','properties',JSON_OBJECT('queryType',JSON_OBJECT('type','string'),'orderCount',JSON_OBJECT('type','integer'),'orders',JSON_OBJECT('type','array'),'status',JSON_OBJECT('type','string'),'trackingNo',JSON_OBJECT('type','string'),'eta',JSON_OBJECT('type','string'))),
    updated_at = CURRENT_TIMESTAMP(3)
WHERE tool_code = 'demo_order_status_rest';

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

UPDATE workflow_node node_row
JOIN workflow_definition workflow_row ON workflow_row.id = node_row.workflow_id
SET node_row.config_json = JSON_SET(
      node_row.config_json,
      '$.promptTemplate', '用户问题：{{input}}\n已执行工具结果：{{toolResult}}\n请优先使用工具结果中的 responseJson 或 responseBody。若 queryType=order_summary，必须回答订单数量和订单摘要；若 found=true 且是单个订单，必须给出订单状态、物流单号、预计送达时间；退款要求按 refundPolicy 提醒人工复核，不要回答缺少工具结果。'
    ),
    node_row.updated_at = CURRENT_TIMESTAMP(3)
WHERE workflow_row.workflow_code = 'demo-customer-service-flow'
  AND node_row.node_key = 'llm';

UPDATE workflow_definition
SET graph_json = JSON_SET(
      graph_json,
      '$.nodes[2].data.config.runConditionEnabled', true,
      '$.nodes[2].data.config.runConditionMode', 'RUN_WHEN',
      '$.nodes[2].data.config.runConditionExpr', 'input contains OAF- || input contains 多少订单 || input contains 几个订单 || input contains 几笔订单 || input contains 订单数量 || input contains 订单数 || input contains 我的订单 || input contains 订单列表 || input contains 所有订单 || input contains 有哪些订单',
      '$.nodes[3].data.config.promptTemplate', '用户问题：{{input}}\n已执行工具结果：{{toolResult}}\n请优先使用工具结果中的 responseJson 或 responseBody。若 queryType=order_summary，必须回答订单数量和订单摘要；若 found=true 且是单个订单，必须给出订单状态、物流单号、预计送达时间；退款要求按 refundPolicy 提醒人工复核，不要回答缺少工具结果。'
    ),
    updated_at = CURRENT_TIMESTAMP(3)
WHERE workflow_code = 'demo-customer-service-flow';

UPDATE workflow_version version_row
JOIN workflow_definition workflow_row ON workflow_row.id = version_row.workflow_id
SET version_row.graph_json = workflow_row.graph_json
WHERE workflow_row.workflow_code = 'demo-customer-service-flow'
  AND version_row.version_no = 'v1';
