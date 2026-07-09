-- P37：知识库递归结构化切片默认策略。
-- 目标：新建知识库默认使用 parent_child 切片策略，优先按标题、段落、列表、句子边界切片，固定窗口只作为兜底。

ALTER TABLE knowledge_base
  MODIFY COLUMN chunk_strategy varchar(64) NOT NULL DEFAULT 'parent_child' COMMENT '分片STRATEGY';
