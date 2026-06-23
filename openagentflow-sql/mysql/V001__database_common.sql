CREATE DATABASE IF NOT EXISTS openagentflow
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE openagentflow;

CREATE TABLE IF NOT EXISTS sys_idempotency_key (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  idem_key varchar(128) NOT NULL UNIQUE COMMENT 'IDEM密钥',
  request_hash varchar(128) NOT NULL COMMENT '请求哈希',
  response_body json COMMENT '响应BODY',
  status_code int COMMENT '状态编码',
  locked_until datetime(3) COMMENT '字段说明：LOCKEDUNTIL',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  expired_at datetime(3) NOT NULL COMMENT '过期时间'
) ENGINE=InnoDB COMMENT='系统IDEMPOTENCY密钥表';

CREATE TABLE IF NOT EXISTS sys_sequence (
  seq_name varchar(80) PRIMARY KEY COMMENT 'SEQ名称',
  current_value bigint NOT NULL DEFAULT 0 COMMENT 'CURRENT值',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='系统SEQUENCE表';

CREATE TABLE IF NOT EXISTS sys_config (
  id char(36) PRIMARY KEY DEFAULT (UUID()) COMMENT '主键ID',
  config_key varchar(160) NOT NULL UNIQUE COMMENT '配置密钥',
  config_value text COMMENT '配置值',
  value_type varchar(32) NOT NULL DEFAULT 'string' COMMENT '值类型',
  group_code varchar(80) NOT NULL DEFAULT 'default' COMMENT 'GROUP编码',
  description varchar(500) COMMENT '描述',
  encrypted tinyint(1) NOT NULL DEFAULT 0 COMMENT '字段说明：ENCRYPTED',
  editable tinyint(1) NOT NULL DEFAULT 1 COMMENT '字段说明：EDITABLE',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='系统配置表';
