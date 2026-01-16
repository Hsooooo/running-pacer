-- Add user_id to ingest_jobs for proper user association
ALTER TABLE ingest_jobs ADD COLUMN user_id BIGINT NULL AFTER provider_ref_id;
ALTER TABLE ingest_jobs ADD CONSTRAINT fk_ingest_user FOREIGN KEY (user_id) REFERENCES users(user_id);
ALTER TABLE ingest_jobs ADD KEY idx_ingest_user (user_id);
