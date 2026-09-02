ALTER TABLE contracts
    ADD COLUMN IF NOT EXISTS freight_rate_per_ton_km DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS estimated_distance_km DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS estimated_cargo_weight_ton DOUBLE PRECISION;
