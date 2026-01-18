-- users
CREATE TABLE IF NOT EXISTS users (
  user_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nickname VARCHAR(50) NOT NULL DEFAULT '',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- strava_tokens
CREATE TABLE IF NOT EXISTS strava_tokens (
  user_id BIGINT NOT NULL PRIMARY KEY,
  athlete_id BIGINT NULL,
  access_token VARCHAR(255) NOT NULL,
  refresh_token VARCHAR(255) NOT NULL,
  expires_at BIGINT NOT NULL,
  scope VARCHAR(255) NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_strava_tokens_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- activities
CREATE TABLE IF NOT EXISTS activities (
  activity_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

  user_id BIGINT NOT NULL,
  source VARCHAR(20) NOT NULL,
  source_activity_id BIGINT NOT NULL,

  sport_type VARCHAR(20) NOT NULL,
  start_time_utc TIMESTAMP NOT NULL,

  timezone VARCHAR(50) NULL,

  distance_m INT NOT NULL,
  moving_time_s INT NOT NULL,
  elapsed_time_s INT NOT NULL,

  avg_hr INT NULL,
  max_hr INT NULL,
  avg_cadence INT NULL,

  elevation_gain_m INT NULL,
  avg_pace_sec_per_km INT NULL,

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  UNIQUE (source, source_activity_id),
  CONSTRAINT fk_activities_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_activity_user_time ON activities (user_id, start_time_utc);
CREATE INDEX idx_activity_user_dist ON activities (user_id, distance_m);

-- laps
CREATE TABLE IF NOT EXISTS laps (
  lap_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

  activity_id BIGINT NOT NULL,
  lap_index INT NOT NULL,

  distance_m INT NOT NULL,
  moving_time_s INT NOT NULL,

  avg_hr INT NULL,
  avg_cadence INT NULL,
  elevation_gain_m INT NULL,
  avg_pace_sec_per_km INT NULL,

  UNIQUE (activity_id, lap_index),
  CONSTRAINT fk_laps_activity FOREIGN KEY (activity_id) REFERENCES activities(activity_id)
);

CREATE INDEX idx_lap_activity ON laps (activity_id);

-- activity_streams (JSON raw)
CREATE TABLE IF NOT EXISTS activity_streams (
  activity_id BIGINT NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL,

  streams_json JSONB NOT NULL,
  stream_keys VARCHAR(255) NOT NULL,
  sample_count INT NULL,

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_streams_activity FOREIGN KEY (activity_id) REFERENCES activities(activity_id),
  CONSTRAINT fk_streams_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_stream_user ON activity_streams (user_id);

-- raw_strava_payloads (debug/idempotency)
CREATE TABLE IF NOT EXISTS raw_strava_payloads (
  payload_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL,
  provider VARCHAR(20) NOT NULL,                    -- STRAVA
  activity_id BIGINT NULL,
  payload_type VARCHAR(30) NOT NULL,                -- ACTIVITY | LAPS | STREAMS | WEBHOOK_EVENT
  payload_json JSONB NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_raw_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_raw_user_created ON raw_strava_payloads (user_id, created_at);
CREATE INDEX idx_raw_activity ON raw_strava_payloads (activity_id);

-- daily_stats (AI analysis core)
CREATE TABLE IF NOT EXISTS daily_stats (
  user_id BIGINT NOT NULL,
  stat_date DATE NOT NULL,
  run_count INT NOT NULL DEFAULT 0,
  total_distance_m INT NOT NULL DEFAULT 0,
  total_moving_time_s INT NOT NULL DEFAULT 0,
  avg_pace_sec_per_km INT NOT NULL DEFAULT 0,
  elevation_gain_m INT NOT NULL DEFAULT 0,
  avg_hr INT NULL,
  max_hr INT NULL,
  avg_cadence INT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, stat_date),
  CONSTRAINT fk_daily_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- ingest_jobs (async stabilization)
CREATE TABLE IF NOT EXISTS ingest_jobs (
  job_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  provider VARCHAR(20) NOT NULL,                    -- STRAVA
  provider_ref_id BIGINT NOT NULL,                  -- activity_id
  job_type VARCHAR(30) NOT NULL,                    -- ACTIVITY_UPSERT
  status VARCHAR(20) NOT NULL,                      -- PENDING | RUNNING | DONE | FAILED
  retry_count INT NOT NULL DEFAULT 0,
  last_error TEXT NULL,
  next_run_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (provider, provider_ref_id, job_type)
);

CREATE INDEX idx_jobs_status_next ON ingest_jobs (status, next_run_at);
