CREATE TABLE entanglement_edge (
  id                 BIGINT NOT NULL AUTO_INCREMENT,
  source_universe_id BIGINT       NOT NULL,
  target_universe_id BIGINT       NOT NULL,
  weight             DECIMAL(4,2) NOT NULL DEFAULT 0.50,
  trace_id           VARCHAR(64)  NOT NULL DEFAULT '',
  gmt_create         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_source (source_universe_id),
  KEY idx_target (target_universe_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宇宙关联边';