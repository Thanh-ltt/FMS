CREATE TABLE IF NOT EXISTS cargo_rates (
    id VARCHAR(255) PRIMARY KEY,
    cargo_type VARCHAR(50) NOT NULL UNIQUE,
    cargo_label VARCHAR(255) NOT NULL,
    rate_per_ton_km DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

INSERT INTO cargo_rates (id, cargo_type, cargo_label, rate_per_ton_km)
VALUES
    ('cargo-rate-dry', 'DRY', 'Hàng khô', 20000),
    ('cargo-rate-cold', 'COLD', 'Hàng lạnh/đông lạnh', 32000),
    ('cargo-rate-fragile', 'FRAGILE', 'Hàng dễ vỡ', 28000),
    ('cargo-rate-dangerous', 'DANGEROUS', 'Hàng nguy hiểm', 45000),
    ('cargo-rate-construction', 'CONSTRUCTION', 'Vật liệu xây dựng', 18000),
    ('cargo-rate-machinery', 'MACHINERY', 'Máy móc/thiết bị', 30000),
    ('cargo-rate-agriculture', 'AGRICULTURE', 'Nông sản/thực phẩm', 22000),
    ('cargo-rate-oversized', 'OVERSIZED', 'Hàng quá khổ/quá tải', 50000),
    ('cargo-rate-other', 'OTHER', 'Khác', 20000)
ON CONFLICT (cargo_type) DO NOTHING;
