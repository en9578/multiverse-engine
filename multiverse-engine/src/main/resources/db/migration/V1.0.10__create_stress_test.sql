CREATE TABLE stress_test (
  id             BIGINT NOT NULL AUTO_INCREMENT,
  universe_id    BIGINT       NOT NULL,
  storm          VARCHAR(32)  NOT NULL COMMENT '价格海啸/政策地震/差评海啸/巨头入侵/汇率风暴',
  survival_rate  DECIMAL(4,2) NOT NULL DEFAULT 0.00,
  weakest_link   VARCHAR(256) NOT NULL DEFAULT '',
  fix_suggestion VARCHAR(256) NOT NULL DEFAULT '',
  trace_id       VARCHAR(64)  NOT NULL DEFAULT '',
  gmt_create     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_st_universe_id (universe_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='极端风暴压力测试';
