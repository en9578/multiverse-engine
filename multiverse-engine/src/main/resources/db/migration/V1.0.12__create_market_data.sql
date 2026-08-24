CREATE TABLE market_data (
  id                 BIGINT NOT NULL AUTO_INCREMENT,
  task_id            BIGINT       NOT NULL COMMENT '关联 multiverse_task.id',
  category           VARCHAR(32)  NOT NULL COMMENT 'EXCHANGE_RATE|PAIN_POINT|POLICY|COMPETITOR_STRATEGY',
  source             VARCHAR(32)  NOT NULL COMMENT 'frankfurter|kb|kb_stale|tavily|r1_inferred',
  raw_data           TEXT         COMMENT '原始采集数据 JSON（汇率 rates / KB 条目列表）',
  last_verified      DATE         COMMENT '最后验证日期',
  freshness_ttl_days INT          NOT NULL DEFAULT 30 COMMENT 'TTL 天数',
  freshness_status   VARCHAR(16)  NOT NULL DEFAULT 'FRESH' COMMENT 'FRESH|STALE|MISSING',
  weight             DECIMAL(3,2) NOT NULL DEFAULT 1.00 COMMENT 'TTL 权重 1.0/0.5/0',
  trace_id           VARCHAR(64)  NOT NULL DEFAULT '',
  gmt_create         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_md_task_category (task_id, category),
  KEY idx_md_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='市场数据采集结果与新鲜度';
