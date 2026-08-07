CREATE TABLE multiverse_task (
  id                   BIGINT NOT NULL AUTO_INCREMENT,
  request_id           VARCHAR(64)  NOT NULL COMMENT '幂等键(不含round)',
  status               VARCHAR(20)  NOT NULL DEFAULT 'CREATED',
  last_completed_stage VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '断点恢复',
  product_name         VARCHAR(128) NOT NULL DEFAULT '',
  target_market        VARCHAR(32)  NOT NULL DEFAULT '',
  strategy_desc        VARCHAR(512) NOT NULL DEFAULT '',
  result_json          TEXT         COMMENT '最终产出JSON',
  overall_progress     INT          NOT NULL DEFAULT 0,
  trace_id             VARCHAR(64)  NOT NULL DEFAULT '',
  gmt_create           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_request_id (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多元宇宙任务';