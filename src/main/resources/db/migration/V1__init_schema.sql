-- users
CREATE TABLE IF NOT EXISTS users (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nickname VARCHAR(50) NOT NULL DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- strava_tokens
CREATE TABLE IF NOT EXISTS strava_tokens (
  user_id BIGINT NOT NULL PRIMARY KEY,
  athlete_id BIGINT NULL,
  access_token VARCHAR(255) NOT NULL,
  refresh_token VARCHAR(255) NOT NULL,
  expires_at BIGINT NOT NULL,
  scope VARCHAR(255) NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_strava_tokens_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- activities
CREATE TABLE IF NOT EXISTS activities (
  activity_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,

  user_id BIGINT NOT NULL,
  source VARCHAR(20) NOT NULL,
  source_activity_id BIGINT NOT NULL,

  sport_type VARCHAR(20) NOT NULL,
  start_time_utc DATETIME NOT NULL,

  timezone VARCHAR(50) NULL,

  distance_m INT NOT NULL,
  moving_time_s INT NOT NULL,
  elapsed_time_s INT NOT NULL,

  avg_hr INT NULL,
  max_hr INT NULL,
  avg_cadence INT NULL,

  elevation_gain_m INT NULL,
  avg_pace_sec_per_km INT NULL,

  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_activity_source (source, source_activity_id),
  KEY idx_activity_user_time (user_id, start_time_utc),
  KEY idx_activity_user_dist (user_id, distance_m),

  CONSTRAINT fk_activities_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- laps
CREATE TABLE IF NOT EXISTS laps (
  lap_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,

  activity_id BIGINT NOT NULL,
  lap_index INT NOT NULL,

  distance_m INT NOT NULL,
  moving_time_s INT NOT NULL,

  avg_hr INT NULL,
  avg_cadence INT NULL,
  elevation_gain_m INT NULL,
  avg_pace_sec_per_km INT NULL,

  UNIQUE KEY uk_lap (activity_id, lap_index),
  KEY idx_lap_activity (activity_id),

  CONSTRAINT fk_laps_activity FOREIGN KEY (activity_id) REFERENCES activities(activity_id)
);

-- activity_streams (JSON raw)
CREATE TABLE IF NOT EXISTS activity_streams (
  activity_id BIGINT NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL,

  streams_json JSON NOT NULL,
  stream_keys VARCHAR(255) NOT NULL,
  sample_count INT NULL,

  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  KEY idx_stream_user (user_id),

  CONSTRAINT fk_streams_activity FOREIGN KEY (activity_id) REFERENCES activities(activity_id),
  CONSTRAINT fk_streams_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- raw_strava_payloads (debug/idempotency)
CREATE TABLE IF NOT EXISTS raw_strava_payloads (
  payload_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  provider VARCHAR(20) NOT NULL,                    -- STRAVA
  activity_id BIGINT NULL,
  payload_type VARCHAR(30) NOT NULL,                -- ACTIVITY | LAPS | STREAMS | WEBHOOK_EVENT
  payload_json JSON NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_raw_user_created (user_id, created_at),
  KEY idx_raw_activity (activity_id),
  CONSTRAINT fk_raw_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

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
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, stat_date),
  CONSTRAINT fk_daily_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- ingest_jobs (async stabilization)
CREATE TABLE IF NOT EXISTS ingest_jobs (
  job_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  provider VARCHAR(20) NOT NULL,                    -- STRAVA
  provider_ref_id BIGINT NOT NULL,                  -- activity_id
  job_type VARCHAR(30) NOT NULL,                    -- ACTIVITY_UPSERT
  status VARCHAR(20) NOT NULL,                      -- PENDING | RUNNING | DONE | FAILED
  retry_count INT NOT NULL DEFAULT 0,
  last_error TEXT NULL,
  next_run_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_provider_job (provider, provider_ref_id, job_type),
  KEY idx_jobs_status_next (status, next_run_at)
);
