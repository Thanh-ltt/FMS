-- V19: Add missing updated_at column to notifications table
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
-- Also fix created_at to be nullable (to match @CreationTimestamp behavior with BaseEntity)
ALTER TABLE notifications ALTER COLUMN created_at DROP NOT NULL;
