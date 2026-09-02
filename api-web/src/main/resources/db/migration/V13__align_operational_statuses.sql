ALTER TABLE maintenances
    ADD COLUMN IF NOT EXISTS started_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;

UPDATE contracts
SET status = 'DRAFT'
WHERE status IS NULL;

UPDATE maintenances
SET started_at = maintenance_date::timestamp
WHERE status = 'IN_PROGRESS' AND started_at IS NULL;

UPDATE maintenances
SET started_at = maintenance_date::timestamp,
    completed_at = maintenance_date::timestamp
WHERE status = 'COMPLETED' AND completed_at IS NULL;
