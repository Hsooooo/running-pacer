-- webhook_events: Strava webhook 이벤트 저장, idempotency 처리
CREATE TABLE IF NOT EXISTS webhook_events (
  event_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  object_type VARCHAR(20) NOT NULL,
  aspect_type VARCHAR(20) NOT NULL,
  object_id BIGINT NOT NULL,
  owner_id BIGINT NOT NULL,
  event_time BIGINT NOT NULL,
  event_json JSON NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_webhook_object_id (object_id),
  KEY idx_webhook_event_time (event_time)
);
