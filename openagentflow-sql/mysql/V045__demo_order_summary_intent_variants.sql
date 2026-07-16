USE openagentflow;

-- V045：补充订单汇总查询的口语表达，并按节点 ID 修复演示工作流图中的条件归属。
SET @demo_workflow_id = '80000000-0000-0000-0000-000000000101';
SET @order_summary_condition = 'intent:order_runtime';
SET @rag_condition = 'input contains 产品 || input contains 优惠券 || input contains 优惠卷 || input contains 活动 || input contains 价格 || input contains 会员 || input contains 退款 || input contains 政策 || input contains 服务';
SET @llm_prompt = '用户问题：{{input}}\n已执行工具结果：{{toolResult}}\n请优先使用工具结果中的 responseJson 或 responseBody。若 queryType=order_summary，必须回答订单数量和订单摘要；若 found=true 且是单个订单，必须给出订单状态、物流单号、预计送达时间；退款要求按 refundPolicy 提醒人工复核，不要回答缺少工具结果。';

-- 更新后端执行引擎直接读取的工具节点配置。
UPDATE workflow_node
SET config_json = JSON_SET(
        config_json,
        '$.runConditionEnabled', TRUE,
        '$.runConditionMode', 'RUN_WHEN',
        '$.runConditionExpr', @order_summary_condition
    ),
    updated_at = CURRENT_TIMESTAMP(3)
WHERE workflow_id = @demo_workflow_id
  AND node_key = 'tool';

-- 按节点 ID 更新当前工作流图，避免依赖 JSON 数组位置而把配置写入相邻节点。
UPDATE workflow_definition
SET graph_json = JSON_REMOVE(
        JSON_SET(
            graph_json,
            REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'rag', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionEnabled'), TRUE,
            REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'rag', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionMode'), 'RUN_WHEN',
            REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'rag', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionExpr'), @rag_condition,
            REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'tool', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionEnabled'), TRUE,
            REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'tool', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionMode'), 'RUN_WHEN',
            REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'tool', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionExpr'), @order_summary_condition,
            REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'llm', NULL, '$.nodes[*].id')), '.id', '.data.config.promptTemplate'), @llm_prompt
        ),
        REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'llm', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionEnabled'),
        REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'llm', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionMode'),
        REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'llm', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionExpr'),
        REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'output', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionEnabled'),
        REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'output', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionMode'),
        REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'output', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionExpr'),
        REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'rag', NULL, '$.nodes[*].id')), '.id', '.data.config.promptTemplate')
    ),
    updated_at = CURRENT_TIMESTAMP(3)
WHERE id = @demo_workflow_id
  AND JSON_SEARCH(graph_json, 'one', 'rag', NULL, '$.nodes[*].id') IS NOT NULL
  AND JSON_SEARCH(graph_json, 'one', 'tool', NULL, '$.nodes[*].id') IS NOT NULL
  AND JSON_SEARCH(graph_json, 'one', 'llm', NULL, '$.nodes[*].id') IS NOT NULL
  AND JSON_SEARCH(graph_json, 'one', 'output', NULL, '$.nodes[*].id') IS NOT NULL;

-- 同步全部已发布版本快照，防止切换版本后重新出现旧条件。
UPDATE workflow_version
SET graph_json = JSON_REMOVE(
        JSON_SET(
            graph_json,
            REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'rag', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionEnabled'), TRUE,
            REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'rag', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionMode'), 'RUN_WHEN',
            REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'rag', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionExpr'), @rag_condition,
            REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'tool', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionEnabled'), TRUE,
            REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'tool', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionMode'), 'RUN_WHEN',
            REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'tool', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionExpr'), @order_summary_condition,
            REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'llm', NULL, '$.nodes[*].id')), '.id', '.data.config.promptTemplate'), @llm_prompt
        ),
        REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'llm', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionEnabled'),
        REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'llm', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionMode'),
        REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'llm', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionExpr'),
        REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'output', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionEnabled'),
        REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'output', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionMode'),
        REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'output', NULL, '$.nodes[*].id')), '.id', '.data.config.runConditionExpr'),
        REPLACE(JSON_UNQUOTE(JSON_SEARCH(graph_json, 'one', 'rag', NULL, '$.nodes[*].id')), '.id', '.data.config.promptTemplate')
    )
WHERE workflow_id = @demo_workflow_id
  AND JSON_SEARCH(graph_json, 'one', 'rag', NULL, '$.nodes[*].id') IS NOT NULL
  AND JSON_SEARCH(graph_json, 'one', 'tool', NULL, '$.nodes[*].id') IS NOT NULL
  AND JSON_SEARCH(graph_json, 'one', 'llm', NULL, '$.nodes[*].id') IS NOT NULL
  AND JSON_SEARCH(graph_json, 'one', 'output', NULL, '$.nodes[*].id') IS NOT NULL;
