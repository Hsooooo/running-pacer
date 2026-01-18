-- race_goals (레이스 목표)
CREATE TABLE IF NOT EXISTS race_goals (
  goal_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL,
  race_name VARCHAR(100) NOT NULL,
  race_date DATE NOT NULL,
  race_distance_m INT NOT NULL,
  target_pace_sec_per_km INT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, COMPLETED, CANCELLED
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  CONSTRAINT fk_goal_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_goal_user_status ON race_goals(user_id, status);

-- training_loads (훈련 부하 및 강도)
CREATE TABLE IF NOT EXISTS training_loads (
  load_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  training_date DATE NOT NULL,
  
  tss DECIMAL(6,2) NULL,           -- Training Stress Score
  intensity_factor DECIMAL(4,3) NULL, -- IF (Intensity Factor)
  workout_type VARCHAR(30) NULL,   -- EASY, TEMPO, INTERVAL, LONG_RUN, RECOVERY
  vdot_estimate DECIMAL(4,1) NULL, -- 해당 러닝에서 추정된 VDOT
  
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  UNIQUE (activity_id),
  
  CONSTRAINT fk_load_user FOREIGN KEY (user_id) REFERENCES users(user_id),
  CONSTRAINT fk_load_activity FOREIGN KEY (activity_id) REFERENCES activities(activity_id)
);

CREATE INDEX idx_load_user_date ON training_loads(user_id, training_date);
