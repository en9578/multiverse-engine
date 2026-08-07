CREATE TABLE settlement_decision (
  id            BIGINT NOT NULL AUTO_INCREMENT,
  universe_id   BIGINT       NOT NULL,
  decision_data TEXT         NOT NULL COMMENT '决策JSON(反脆弱组合+理由)',
  is_confirmed  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '人工确认',
  trace_id      VARCHAR(64)  NOT NULL DEFAULT '',
  gmt_create    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_sd_universe_id (universe_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定居决策';