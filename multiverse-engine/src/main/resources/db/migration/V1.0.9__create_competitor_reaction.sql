CREATE TABLE competitor_reaction (
  id              BIGINT NOT NULL AUTO_INCREMENT,
  universe_id     BIGINT       NOT NULL,
  competitor_name VARCHAR(128) NOT NULL DEFAULT '',
  reaction_type   VARCHAR(16)  NOT NULL COMMENT '跟价/跟款/差异化/无视',
  probability     DECIMAL(4,2) NOT NULL DEFAULT 0.50,
  impact          VARCHAR(64)  NOT NULL DEFAULT '',
  source          VARCHAR(16)  NOT NULL DEFAULT 'kb' COMMENT 'kb/r1_inferred',
  evidence        TEXT         COMMENT '反应依据',
  trace_id        VARCHAR(64)  NOT NULL DEFAULT '',
  gmt_create      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_cr_universe_id (universe_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞品关联反应';
