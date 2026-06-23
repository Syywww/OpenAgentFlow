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

## Latest Governance Update

- `V011__organization_workspace_governance.sql` creates organization and workspace governance tables with Chinese comments for every table and column.
- Existing Agent, knowledge base, tool, workflow, and MCP server rows are assigned to the default workspace during migration.
- The backend can use workspace membership to decide whether a user can view or manage team resources, while preserving owner, ACL, and administrator permissions.

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
