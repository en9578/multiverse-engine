CREATE TABLE bailian_call_log (
  id          BIGINT NOT NULL AUTO_INCREMENT,
  request_id  VARCHAR(64)  NOT NULL,
  call_type   VARCHAR(32)  NOT NULL COMMENT 'qwen/wanx/vl/r1/embedding',
  input_text  TEXT,
  output_text TEXT,
  success     TINYINT(1)   NOT NULL DEFAULT 1,
  cost_ms     BIGINT       DEFAULT NULL,
  token_count INT          DEFAULT NULL COMMENT '成本追踪',
  trace_id    VARCHAR(64)  NOT NULL DEFAULT '',
  gmt_create  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_req_type (request_id, call_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='百炼调用日志';