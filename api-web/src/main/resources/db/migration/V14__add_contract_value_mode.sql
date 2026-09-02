ALTER TABLE contracts
    ADD COLUMN IF NOT EXISTS value_mode VARCHAR(30);

UPDATE contracts
SET value_mode = CASE
    WHEN contract_value IS NULL THEN 'PER_TRIP'
    ELSE 'AGREED_VALUE'
END
WHERE value_mode IS NULL;

ALTER TABLE contracts
    ALTER COLUMN value_mode SET DEFAULT 'PER_TRIP',
    ALTER COLUMN value_mode SET NOT NULL;
