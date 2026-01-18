-- Add user_id to ingest_jobs for proper user association
ALTER TABLE ingest_jobs ADD COLUMN user_id BIGINT NULL;
ALTER TABLE ingest_jobs ADD CONSTRAINT fk_ingest_user FOREIGN KEY (user_id) REFERENCES users(user_id);
CREATE INDEX idx_ingest_user ON ingest_jobs(user_id);
