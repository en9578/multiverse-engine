CREATE TABLE universe (
  id              BIGINT NOT NULL AUTO_INCREMENT,
  task_id         BIGINT       NOT NULL,
  universe_index  INT          NOT NULL COMMENT '宇宙序号1-5',
  product_name    VARCHAR(128) NOT NULL DEFAULT '',
  target_market   VARCHAR(32)  NOT NULL DEFAULT '',
  rating          VARCHAR(2)   NOT NULL DEFAULT '' COMMENT 'A/B/C/D/F',
  sub_state       VARCHAR(20)  NOT NULL DEFAULT 'GENERATED',
  evolution_data  TEXT         COMMENT '90天演化JSON(含evidences)',
  survival_rate   DECIMAL(4,2) DEFAULT NULL,
  strategy_package TEXT        COMMENT '策略包JSON',
  trace_id        VARCHAR(64)  NOT NULL DEFAULT '',
  gmt_create      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平行宇宙';