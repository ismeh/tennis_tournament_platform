ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS email_confirmation_token_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS email_confirmation_expires_at TIMESTAMPTZ;

UPDATE users
SET email_verified = TRUE
WHERE email_confirmation_token_hash IS NULL;
