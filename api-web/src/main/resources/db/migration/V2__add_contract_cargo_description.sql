ALTER TABLE contracts
    ADD COLUMN IF NOT EXISTS cargo_description VARCHAR(1000);
