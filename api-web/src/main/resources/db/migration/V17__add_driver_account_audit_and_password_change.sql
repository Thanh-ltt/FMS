ALTER TABLE users
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN DEFAULT FALSE;

UPDATE users
SET must_change_password = FALSE
WHERE must_change_password IS NULL;

ALTER TABLE users
    ALTER COLUMN must_change_password SET DEFAULT FALSE,
    ALTER COLUMN must_change_password SET NOT NULL;

ALTER TABLE drivers
    ADD COLUMN IF NOT EXISTS account_provisioned_by_user_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS account_provisioned_by_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS account_provisioned_by_role VARCHAR(30),
    ADD COLUMN IF NOT EXISTS account_provisioned_at TIMESTAMP;
