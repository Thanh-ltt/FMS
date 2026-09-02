-- Hibernate may have created this check constraint for an older enum before
-- the approval workflow existed. Drop it before introducing the new values.
ALTER TABLE expenses
    DROP CONSTRAINT IF EXISTS expenses_status_check;

ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS receipt_image_url TEXT,
    ADD COLUMN IF NOT EXISTS recorded_by_user_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS recorded_by_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS recorded_by_role VARCHAR(30),
    ADD COLUMN IF NOT EXISTS reviewed_by_user_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS reviewed_by_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS review_note VARCHAR(500);

UPDATE expenses
SET status = 'APPROVED';

ALTER TABLE expenses
    ALTER COLUMN status SET DEFAULT 'PENDING',
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE expenses
    ADD CONSTRAINT expenses_status_check
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'));
