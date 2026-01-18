-- strava_user_links: OAuth 연동 후 users 테이블과 Strava athlete_id 매핑
CREATE TABLE IF NOT EXISTS strava_user_links (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL,
  athlete_id BIGINT NOT NULL UNIQUE,
  scope VARCHAR(255) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, DISABLED
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (user_id, athlete_id),
  CONSTRAINT fk_strava_links_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_athlete_id ON strava_user_links(athlete_id);
