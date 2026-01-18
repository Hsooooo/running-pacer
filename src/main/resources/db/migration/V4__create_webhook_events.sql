-- webhook_events: Strava webhook 이벤트 저장, idempotency 처리
CREATE TABLE IF NOT EXISTS webhook_events (
  event_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  object_type VARCHAR(20) NOT NULL,
  aspect_type VARCHAR(20) NOT NULL,
  object_id BIGINT NOT NULL,
  owner_id BIGINT NOT NULL,
  event_time BIGINT NOT NULL,
  event_json JSONB NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_webhook_object_id ON webhook_events(object_id);
CREATE INDEX idx_webhook_event_time ON webhook_events(event_time);
