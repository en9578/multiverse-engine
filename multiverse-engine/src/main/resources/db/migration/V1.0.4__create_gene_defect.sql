CREATE TABLE gene_defect (
  id          BIGINT NOT NULL AUTO_INCREMENT,
  universe_id BIGINT       NOT NULL,
  defect_name VARCHAR(64)  NOT NULL,
  frequency   VARCHAR(16)  NOT NULL COMMENT 'high/medium/low',
  severity    VARCHAR(16)  NOT NULL COMMENT 'critical/major/minor',
  solution    VARCHAR(256) NOT NULL DEFAULT '',
  source_tag  VARCHAR(16)  NOT NULL DEFAULT 'kb' COMMENT 'kb/r1_inferred',
  trace_id    VARCHAR(64)  NOT NULL DEFAULT '',
  gmt_create  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_gd_universe_id (universe_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基因缺陷';