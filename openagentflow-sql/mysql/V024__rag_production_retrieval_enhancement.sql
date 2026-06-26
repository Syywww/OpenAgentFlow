USE openagentflow;

-- P27：RAG 生产级召回增强，补充检索质量治理字段。
ALTER TABLE knowledge_retrieval_log
  ADD COLUMN search_mode varchar(32) NULL COMMENT '检索模式：vector向量、keyword关键词、hybrid混合' AFTER milvus_search_params,
  ADD COLUMN candidate_k int NULL COMMENT '候选召回数量' AFTER top_k,
  ADD COLUMN metadata_filter json NULL COMMENT '元数据过滤条件JSON，包含文档、页码和关键词过滤' AFTER candidate_k,
  ADD COLUMN confidence_score decimal(8,6) NULL COMMENT '最佳置信得分' AFTER result_count,
  ADD COLUMN low_confidence tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否低置信结果' AFTER confidence_score,
  ADD COLUMN quality_advice varchar(1200) NULL COMMENT '检索质量建议' AFTER low_confidence;

ALTER TABLE knowledge_retrieval_log MODIFY COLUMN search_mode varchar(32) NULL COMMENT '检索模式：vector向量、keyword关键词、hybrid混合';
ALTER TABLE knowledge_retrieval_log MODIFY COLUMN candidate_k int NULL COMMENT '候选召回数量';
ALTER TABLE knowledge_retrieval_log MODIFY COLUMN metadata_filter json NULL COMMENT '元数据过滤条件JSON，包含文档、页码和关键词过滤';
ALTER TABLE knowledge_retrieval_log MODIFY COLUMN confidence_score decimal(8,6) NULL COMMENT '最佳置信得分';
ALTER TABLE knowledge_retrieval_log MODIFY COLUMN low_confidence tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否低置信结果';
ALTER TABLE knowledge_retrieval_log MODIFY COLUMN quality_advice varchar(1200) NULL COMMENT '检索质量建议';

CREATE INDEX idx_retrieval_quality_mode ON knowledge_retrieval_log(search_mode, low_confidence, created_at);
CREATE INDEX idx_retrieval_confidence ON knowledge_retrieval_log(confidence_score, created_at);
