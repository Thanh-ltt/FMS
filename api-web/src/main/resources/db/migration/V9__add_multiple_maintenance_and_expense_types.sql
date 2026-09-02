ALTER TABLE maintenances
    ADD COLUMN IF NOT EXISTS maintenance_types VARCHAR(1000);

UPDATE maintenances
SET maintenance_types = maintenance_type
WHERE maintenance_types IS NULL
  AND maintenance_type IS NOT NULL;

ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS expense_types VARCHAR(1000);

UPDATE expenses
SET expense_types = expense_type
WHERE expense_types IS NULL
  AND expense_type IS NOT NULL;
