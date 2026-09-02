CREATE TABLE IF NOT EXISTS invoice_payments (
    id VARCHAR(255) PRIMARY KEY,
    invoice_id VARCHAR(255) NOT NULL REFERENCES invoices(id),
    amount DOUBLE PRECISION NOT NULL,
    payment_date DATE NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    bank_name VARCHAR(255),
    account_holder VARCHAR(255),
    account_number VARCHAR(255),
    transaction_reference VARCHAR(255),
    note VARCHAR(1000),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_invoice_payment_invoice UNIQUE (invoice_id),
    CONSTRAINT ck_invoice_payment_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_invoice_bank_transfer_details CHECK (
        payment_method <> 'BANK_TRANSFER'
        OR (bank_name IS NOT NULL AND TRIM(bank_name) <> ''
            AND transaction_reference IS NOT NULL AND TRIM(transaction_reference) <> '')
    )
);

CREATE INDEX IF NOT EXISTS idx_invoice_payments_invoice ON invoice_payments(invoice_id);
CREATE INDEX IF NOT EXISTS idx_invoice_payments_date ON invoice_payments(payment_date);

ALTER TABLE deposits
    ADD COLUMN IF NOT EXISTS bank_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS account_holder VARCHAR(255),
    ADD COLUMN IF NOT EXISTS account_number VARCHAR(255);

ALTER TABLE deposit_refunds
    ADD COLUMN IF NOT EXISTS bank_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS account_holder VARCHAR(255),
    ADD COLUMN IF NOT EXISTS account_number VARCHAR(255);
