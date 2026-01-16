-- strava_user_links: OAuth 연동 후 users 테이블과 Strava athlete_id 매핑
CREATE TABLE IF NOT EXISTS strava_user_links (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  athlete_id BIGINT NOT NULL UNIQUE,
  scope VARCHAR(255) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, DISABLED
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_strava_user (user_id, athlete_id),
  KEY idx_athlete_id (athlete_id),
  CONSTRAINT fk_strava_links_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
