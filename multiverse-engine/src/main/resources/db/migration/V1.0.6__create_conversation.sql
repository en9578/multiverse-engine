CREATE TABLE conversation (
  id           BIGINT NOT NULL AUTO_INCREMENT,
  task_id      BIGINT       NOT NULL,
  universe_id  VARCHAR(32)  NOT NULL DEFAULT '',
  session_id   VARCHAR(64)  NOT NULL DEFAULT '',
  role         VARCHAR(16)  NOT NULL COMMENT 'user/assistant/system',
  content      TEXT         NOT NULL,
  trace_id     VARCHAR(64)  NOT NULL DEFAULT '',
  is_deleted   TINYINT(1)   NOT NULL DEFAULT 0,
  gmt_create   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_task_universe_session (task_id, universe_id, session_id),
  KEY idx_trace_id (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话历史(三级隔离)';