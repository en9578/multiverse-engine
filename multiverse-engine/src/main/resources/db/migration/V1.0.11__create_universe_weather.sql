CREATE TABLE universe_weather (
  id               BIGINT NOT NULL AUTO_INCREMENT,
  universe_id      BIGINT       NOT NULL,
  weather          VARCHAR(16)  NOT NULL COMMENT '晴/多云/雨/风暴',
  search_signal    VARCHAR(64)  NOT NULL DEFAULT '',
  sentiment_signal VARCHAR(64)  NOT NULL DEFAULT '',
  price_signal     VARCHAR(64)  NOT NULL DEFAULT '',
  policy_signal    VARCHAR(64)  NOT NULL DEFAULT '',
  forecast_7d      VARCHAR(16)  NOT NULL DEFAULT '',
  forecast_30d     VARCHAR(16)  NOT NULL DEFAULT '',
  forecast_90d     VARCHAR(16)  NOT NULL DEFAULT '',
  trace_id         VARCHAR(64)  NOT NULL DEFAULT '',
  gmt_create       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_uw_universe_id (universe_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宇宙市场气象';
