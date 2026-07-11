# OpenAgentFlow-Java MySQL SQL

Database target: MySQL 8.0.13+ / InnoDB / `utf8mb4`.

This is the MySQL alternative to the PostgreSQL + pgvector schema.

## Important Difference From PostgreSQL

MySQL Community Edition does not provide the same mature in-database vector index path as PostgreSQL `pgvector`.
For RAG embeddings, this schema stores:

- `embedding_json`: readable vector payload for debugging or low-volume fallback.
- `embedding_blob`: compact binary payload if the backend serializes float arrays.
- `external_vector_id`: ID for Milvus, Elasticsearch/OpenSearch, Qdrant, or another vector index.

Recommended production architecture:

```text
MySQL: business data, auth, Agent config, workflow, trace, eval
Vector DB/Search: embeddings and ANN retrieval
Object Storage: uploaded files and parsed artifacts
Redis: cache, sessions, queue state
```

## Files

- `V001__database_common.sql`: database, common system tables.
- `V002__all_feature_tables.sql`: all functional tables.
- `V003__indexes_views_seed.sql`: indexes, dashboard views, seed data.
- `V004__milvus_integration.sql`: Milvus connection metadata, collection/partition mapping, vector sync task tables, and MySQL-to-Milvus mapping fields.
- `V005__refresh_zh_comments.sql`: refresh Chinese table and column comments for databases created before comments were added.
- `V006__refresh_admin_password.sql`: refresh the built-in `admin/123456` BCrypt password for databases initialized before authentication was implemented.
- `V007__seed_doubao_model_provider.sql`: seed Doubao Ark OpenAI-compatible provider and endpoint model without storing any API key.
- `V008__agent_crud_permissions.sql`: seed Agent CRUD, publish, copy, delete, run permissions and owner ACL for the built-in Agent.
- `V009__rag_embedding_model_and_permissions.sql`: seed Doubao multimodal embedding endpoint `ep-20260615092553-lqvch` and knowledge-base permissions.
- `V010__usage_cost_center.sql`: usage and cost-center permissions, indexes, quota seed data.
- `V011__organization_workspace_governance.sql`: organization, workspace, workspace members, resource ownership, and workspace IDs for core resources.
- `V012__async_task_center.sql`: unified async task center tables, logs, permissions, cancel and retry visibility.
- `V013__audit_risk_governance_center.sql`: risk governance event table, indexes, permissions, and unified audit/risk-center support.
- `V014__model_gateway_governance.sql`: model gateway governance fields, route-policy indexes, default Agent chat route policy, and gateway decision logging.
- `V015__knowledge_governance_enhancement.sql`: knowledge-base governance policies, quality issues, permissions, and default governance policy.
- `V016__ops_monitor_alert_center.sql`: platform operation monitoring, health checks, alert rules, alert events, notification channels, and monitoring permissions.
- `V017__prompt_template_center.sql`: Prompt template center permissions, default Prompt versions, variable comments, and seed Prompt templates.
- `V018__iam_admin_center.sql`: user, department, role, and permission administration support.
- `V019__seed_default_login_users.sql`: default login user seed data.
- `V020__multi_agent_collaboration.sql`: multi-Agent team, member, and collaboration runtime tables.
- `V021__runtime_trace_token_usage_default.sql`: runtime trace token usage defaults.
- `V022__memory_center.sql`: Agent memory center tables and indexes.
- `V023__seed_customer_support_memory_template.sql`: customer-support Agent long-term memory template seed data.
- `V024__rag_production_retrieval_enhancement.sql`: production RAG recall fields, confidence indexes, and metadata filter columns.
- `V025__evaluation_llm_as_judge.sql`: LLM-as-Judge metric setup, judge detail comments, and default score configuration.
- `V026__delivery_acceptance_center.sql`: delivery acceptance report table, permissions, and menu API access.
- `V027__workflow_production_enhancement.sql`: workflow templates, API endpoints, strategy hit logs, input/output schema, execution policy, and advanced workflow permissions.
- `V028__workflow_execution_reliability_final.sql`: workflow idempotency, heartbeat, recovery snapshot, and failed-node resume fields.
- `V029__demo_data_package.sql`: one-click demo data package with Prompt, Agents, knowledge chunks, tools, workflow, evaluation dataset, multi-Agent team, Memory, and delivery checks.
- `V030__customer_service_intent_guard_coupon_policy.sql`: customer-service tool intent boundary and coupon-policy knowledge chunk.
- `V031__customer_service_product_policy.sql`: product/service-scope knowledge chunk and knowledge-first customer-service routing prompt.
- `V032__demo_workflow_node_conditions.sql`: sample node-level run conditions for the demo RAG and order-tool nodes.
- `V033__demo_order_summary_tool_intent.sql`: demo order summary routing for "my orders" and "how many orders" questions.
- `V034__recursive_knowledge_chunking.sql`: Parent-Child recursive structured chunking becomes the default strategy for new knowledge bases.
- `V035__enterprise_rag_metadata_parent_child.sql`: enterprise RAG metadata, existing knowledge-base strategy migration, Parent-Child chunks, knowledge-base versions, and retrieval cache.
- `V036__kafka_distributed_async_tasks.sql`: Kafka queue metadata, Worker locks, heartbeat, retry scheduling, recovery indexes, and dead-letter fields for distributed async tasks.

Recommended execution order:

```text
V001__database_common.sql
V002__all_feature_tables.sql
V003__indexes_views_seed.sql
V004__milvus_integration.sql
V005__refresh_zh_comments.sql
V006__refresh_admin_password.sql
V007__seed_doubao_model_provider.sql
V008__agent_crud_permissions.sql
V009__rag_embedding_model_and_permissions.sql
V010__usage_cost_center.sql
V011__organization_workspace_governance.sql
V012__async_task_center.sql
V013__audit_risk_governance_center.sql
V014__model_gateway_governance.sql
V015__knowledge_governance_enhancement.sql
V016__ops_monitor_alert_center.sql
V017__prompt_template_center.sql
V018__iam_admin_center.sql
V019__seed_default_login_users.sql
V020__multi_agent_collaboration.sql
V021__runtime_trace_token_usage_default.sql
V022__memory_center.sql
V023__seed_customer_support_memory_template.sql
V024__rag_production_retrieval_enhancement.sql
V025__evaluation_llm_as_judge.sql
V026__delivery_acceptance_center.sql
V027__workflow_production_enhancement.sql
V028__workflow_execution_reliability_final.sql
V029__demo_data_package.sql
V030__customer_service_intent_guard_coupon_policy.sql
V031__customer_service_product_policy.sql
V032__demo_workflow_node_conditions.sql
V033__demo_order_summary_tool_intent.sql
V034__recursive_knowledge_chunking.sql
V035__enterprise_rag_metadata_parent_child.sql
V036__kafka_distributed_async_tasks.sql
```

Coverage matches the PostgreSQL version at the feature level:

- IAM, roles, permissions, ACL.
- Model providers, model configs, API keys, quotas.
- Agent, prompt, session, message, memory.
- RAG knowledge bases, documents, parse tasks, chunks, embeddings, retrieval logs, citations.
- Tool Center and high-risk confirmations.
- MCP server, capabilities, tests, discovery.
- Workflow design, versions, nodes, edges, schedules, runtime.
- Runtime Trace, LLM call logs, costs, guardrail events.
- Evaluation datasets, samples, metrics, tasks, scores, reports.
- Templates, notifications, audit, files, webhooks, tags.
- Advanced roadmap: multi-agent, Prompt A/B, model routing, guardrails, plugins, local model deployment, import jobs.

## Latest RAG Update

- `V009__rag_embedding_model_and_permissions.sql` does not store any real API key. API keys stay in local `model_api_key` rows only.
- The seeded Doubao embedding config uses `embeddingApi=multimodal` and calls `/api/v3/embeddings/multimodal`; the model code remains the endpoint ID `ep-20260615092553-lqvch`.
- Knowledge vectors are written to Milvus first. The backend also stores `embedding_json` in MySQL so low-volume retrieval and development fallback can continue when a vector service or embedding endpoint is unavailable.
- Milvus knowledge collections are separated by vector dimension, for example `oaf_knowledge_chunks_d2048`, so real 2048-dimensional vectors do not conflict with earlier local fallback vectors.
- The backend now supports knowledge-base CRUD, upload, parsing, chunking, embedding, Milvus write, retrieval test, source citations, and Agent binding.
- `V024__rag_production_retrieval_enhancement.sql` adds search mode, candidate size, metadata filter, confidence score, low-confidence flag, and quality advice fields for production recall analysis.
- The backend supports hybrid recall, vector/keyword weights, candidate expansion, rerank, document/page/metadata filtering, highlighted citations, rank reasons, and low-confidence advice.

## Latest Governance Update

- `V011__organization_workspace_governance.sql` creates organization and workspace governance tables with Chinese comments for every table and column.
- Existing Agent, knowledge base, tool, workflow, and MCP server rows are assigned to the default workspace during migration.
- The backend can use workspace membership to decide whether a user can view or manage team resources, while preserving owner, ACL, and administrator permissions.

## Latest Async Task Update

- `V012__async_task_center.sql` creates `async_task` and `async_task_log`, with Chinese comments for every table and column.
- `V036__kafka_distributed_async_tasks.sql` adds Kafka Topic/message IDs, Worker locks, heartbeat, next retry time, final failure time, and recovery indexes with Chinese comments.
- Knowledge document processing, vector rebuild, evaluation batch execution, MCP discovery, knowledge governance scans, Memory cleanup, and historical cost recalculation are submitted to Kafka and executed by distributed Workers.
- MySQL conditional updates provide idempotent task claims; five-second and thirty-second retry Topics provide delayed retries; exhausted tasks enter the dead-letter Topic and can be replayed from the task center.
- Original uploaded files are stored in MinIO so any Worker instance can parse the same document.

## Latest Governance Risk Update

- `V013__audit_risk_governance_center.sql` creates `risk_governance_event`, with Chinese comments for every table and column.
- Existing `audit_operation_log`, `tool_invocation_log`, `tool_confirm_request`, `runtime_guardrail_event`, `tool_definition`, and `mcp_capability` become the source tables for the governance center.
- The backend can automatically collect operation audit logs and aggregate high-risk tools, MCP capabilities, pending confirmations, failed tool calls, and guardrail events into one risk list.

## Latest Model Gateway Update

- `V014__model_gateway_governance.sql` extends `runtime_llm_call` with `route_policy_id`, `gateway_scene_type`, `route_decision`, and `fallback_used`, all with Chinese column comments.
- The default `AGENT_CHAT` route policy is seeded as `default-agent-chat`; existing enabled chat models are inserted as route candidates.
- The backend can route unpinned Agent chat and workflow LLM calls through the gateway, record route decisions, and fall back to the next healthy candidate when a model call fails.
- The frontend now includes a Model Gateway page for route-policy CRUD, candidate ordering, model health, recent gateway calls, failure rate, latency, and fallback visibility.

## Latest Knowledge Governance Update

- `V015__knowledge_governance_enhancement.sql` creates `knowledge_governance_policy` and `knowledge_governance_issue`, with Chinese comments for every table and column.
- The default policy checks stale documents, abnormal chunk token ranges, failed parsing, missing embeddings, Milvus sync fallback, empty knowledge bases, and unbound knowledge bases.
- The backend exposes `/knowledge-governance/overview`, `/knowledge-governance/quality`, `/knowledge-governance/scan`, `/knowledge-governance/issues`, and `/knowledge-governance/policies`.
- The frontend now includes a Knowledge Governance page for quality overview, issue scanning, issue handling, policy CRUD, and per-knowledge-base quality scores.

## Latest Ops Monitor Update

- `V016__ops_monitor_alert_center.sql` creates `ops_alert_rule`, `ops_alert_event`, `ops_health_check`, and `ops_notify_channel`, with Chinese comments for every table and column.
- The migration seeds station and Webhook notification channels, seven health checks, five default alert rules, and `ops:monitor:view` / `ops:monitor:manage` permissions.
- The backend exposes `/ops-monitor/overview`, `/ops-monitor/health`, `/ops-monitor/inspect`, `/ops-monitor/rules`, `/ops-monitor/events`, `/ops-monitor/checks`, and `/ops-monitor/channels`.
- The frontend now includes an Ops Monitor page for overview cards, health matrix, alert event handling, alert-rule CRUD, health checks, and notification channels.

## Latest Prompt Template Update

- `V017__prompt_template_center.sql` refreshes Chinese comments for `prompt_template` and `prompt_template_version`, and adds `prompt:manage` permission.
- Existing default RAG and Tool Prompt rows get an initial `v1` version snapshot; the migration also seeds default Evaluation Judge and Workflow Summary Prompt templates.
- The backend exposes `/prompt-templates/overview`, `/prompt-templates`, `/prompt-templates/{id}`, `/prompt-templates/{id}/publish`, `/prompt-templates/{id}/copy`, and `/prompt-templates/{id}/versions/{versionId}/rollback`.
- The frontend now includes a Prompt Template Center page for template CRUD, variable preview, version publishing, copying, rollback, and Agent System Prompt binding.

## Latest Evaluation Update

- `V025__evaluation_llm_as_judge.sql` adds the `llm_judge_overall` metric and marks accuracy, relevance, completeness, and hallucination control as Judge-ready metrics.
- Evaluation tasks can enable or disable LLM-as-Judge, choose a Judge model, and provide a custom Judge Prompt.
- Judge output must be JSON and is stored in `eval_score.judge_detail` with model, latency, Token, reason, strengths, risks, and fallback information.
- The frontend evaluation result page shows Judge score, Judge source type, and low-score reasons.

## Latest Delivery Acceptance Update

- `V026__delivery_acceptance_center.sql` creates `delivery_acceptance_report`, with Chinese comments for every table and column.
- The migration adds `delivery:acceptance:view` and `delivery:acceptance:manage` permissions for the delivery acceptance menu and API.
- The backend exposes `/delivery-acceptance/overview`, `/delivery-acceptance/checks`, `/delivery-acceptance/run`, and `/delivery-acceptance/reports`.
- The frontend now includes a Delivery Acceptance page for environment checks, core-chain checks, risks, delivery manifest, and generated reports.

## Latest Workflow Production Update

- `V027__workflow_production_enhancement.sql` extends `workflow_definition` with input schema, output schema, execution policy, API enabled flag, and release strategy.
- The migration creates `workflow_template`, `workflow_api_endpoint`, and `workflow_policy_hit_log`, with Chinese comments for every table and column.
- The backend exposes advanced workflow overview, templates, API endpoint publishing, human task decisions, endpoint invocation, and version diff APIs.
- The frontend workflow designer now includes production node types, retry and timeout policy, failure branch strategy, debug options, templates, API publishing, governance, human tasks, and version comparison.

## Latest Demo Package Update

- `V029__demo_data_package.sql` seeds a complete customer-service demo package for local showcase and delivery acceptance.
- The package includes customer-service Prompt templates, three Agents, a knowledge base document with four chunks, three tool types, the `demo_order` table, a published workflow, an evaluation dataset, a multi-Agent team, and long-term Memory.
- Demo HTTP tools use `mock.openagentflow.local` for display, while the backend returns built-in mock payloads for seeded demo tools.
- `V030__customer_service_intent_guard_coupon_policy.sql` adds customer-service intent boundaries and a coupon-policy knowledge chunk, so coupon or promotion questions stay on RAG instead of calling order tools.
- `V031__customer_service_product_policy.sql` adds product/service-scope knowledge for “what products do you have” questions and reinforces knowledge-first routing.
- `V032__demo_workflow_node_conditions.sql` adds sample node-level run conditions to the demo workflow RAG node and order-tool node.
- `V033__demo_order_summary_tool_intent.sql` expands demo order-tool routing for “my orders” and “how many orders” questions.
- `scripts/init-demo-data.ps1` can apply the package to an existing local MySQL database and optionally write a local model API key without storing it in repository files.
- The delivery acceptance backend checks whether the P33 demo package covers Agent, knowledge base, chunks, tools, workflow, evaluation dataset, team, Prompt, and Memory.

## Milvus Mapping

The MySQL schema now keeps Milvus operational metadata in:

- `vector_store_connection`
- `vector_collection`
- `vector_partition`
- `vector_record_mapping`
- `vector_sync_task`
- `vector_sync_error`

Knowledge and memory rows point to Milvus through:

- `knowledge_base.vector_collection_id`
- `knowledge_embedding.vector_collection_id`
- `knowledge_embedding.vector_primary_key`
- `agent_memory.vector_collection_id`
- `agent_memory.vector_primary_key`
- `knowledge_retrieval_log.milvus_result_ids`
