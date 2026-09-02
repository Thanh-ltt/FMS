ALTER TABLE contracts
    ADD COLUMN IF NOT EXISTS deposit_required BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deposit_scope VARCHAR(30),
    ADD COLUMN IF NOT EXISTS deposit_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS deposit_value DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS deposit_usage VARCHAR(30),
    ADD COLUMN IF NOT EXISTS deposit_due_days INTEGER,
    ADD COLUMN IF NOT EXISTS deposit_terms VARCHAR(1000);

UPDATE contracts
SET deposit_required = FALSE
WHERE deposit_required IS NULL;

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS deposit_applied_amount DOUBLE PRECISION DEFAULT 0,
    ADD COLUMN IF NOT EXISTS paid_amount DOUBLE PRECISION DEFAULT 0;

UPDATE invoices
SET deposit_applied_amount = 0
WHERE deposit_applied_amount IS NULL;

UPDATE invoices
SET paid_amount = CASE WHEN status = 'PAID' THEN COALESCE(total_amount, 0) ELSE 0 END
WHERE paid_amount IS NULL OR (status = 'PAID' AND paid_amount = 0);

CREATE TABLE IF NOT EXISTS deposits (
    id VARCHAR(255) PRIMARY KEY,
    receipt_number VARCHAR(255) NOT NULL UNIQUE,
    customer_id VARCHAR(255) NOT NULL REFERENCES customers(id),
    contract_id VARCHAR(255) REFERENCES contracts(id),
    trip_id VARCHAR(255) REFERENCES trips(id),
    amount DOUBLE PRECISION NOT NULL,
    allocated_amount DOUBLE PRECISION NOT NULL DEFAULT 0,
    refunded_amount DOUBLE PRECISION NOT NULL DEFAULT 0,
    received_date DATE NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    reference_number VARCHAR(255),
    note VARCHAR(1000),
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT ck_deposits_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_deposits_balances_non_negative CHECK (allocated_amount >= 0 AND refunded_amount >= 0),
    CONSTRAINT ck_deposits_balances_total CHECK (allocated_amount + refunded_amount <= amount)
);

CREATE INDEX IF NOT EXISTS idx_deposits_customer ON deposits(customer_id);
CREATE INDEX IF NOT EXISTS idx_deposits_contract ON deposits(contract_id);
CREATE INDEX IF NOT EXISTS idx_deposits_trip ON deposits(trip_id);

CREATE TABLE IF NOT EXISTS invoice_deposit_allocations (
    id VARCHAR(255) PRIMARY KEY,
    deposit_id VARCHAR(255) NOT NULL REFERENCES deposits(id),
    invoice_id VARCHAR(255) NOT NULL REFERENCES invoices(id),
    amount DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_deposit_invoice UNIQUE (deposit_id, invoice_id),
    CONSTRAINT ck_invoice_deposit_allocation_positive CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_deposit_allocations_invoice
    ON invoice_deposit_allocations(invoice_id);

CREATE TABLE IF NOT EXISTS deposit_refunds (
    id VARCHAR(255) PRIMARY KEY,
    deposit_id VARCHAR(255) NOT NULL REFERENCES deposits(id),
    amount DOUBLE PRECISION NOT NULL,
    refund_date DATE NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    reference_number VARCHAR(255),
    note VARCHAR(1000),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT ck_deposit_refund_positive CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_deposit_refunds_deposit ON deposit_refunds(deposit_id);
