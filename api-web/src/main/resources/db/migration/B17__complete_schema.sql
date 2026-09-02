-- Baseline migration for a brand-new FMS database.
--
-- Existing installations already migrated through V17 ignore this file.
-- New installations apply this baseline directly, then continue with V18+.

CREATE TABLE users (
    id VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255),
    role VARCHAR(255),
    employee_code VARCHAR(255),
    full_name VARCHAR(255),
    phone VARCHAR(255),
    email VARCHAR(255),
    address VARCHAR(255),
    id_number VARCHAR(255),
    dob DATE,
    gender VARCHAR(255),
    "position" VARCHAR(255),
    hire_date DATE,
    avatar_url TEXT,
    active BOOLEAN DEFAULT TRUE,
    must_change_password BOOLEAN DEFAULT FALSE NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT ck_users_role CHECK (
        role IS NULL OR role IN ('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER', 'CUSTOMER')
    ),
    CONSTRAINT ck_users_gender CHECK (
        gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER')
    )
);

CREATE UNIQUE INDEX uk_users_employee_code
    ON users (employee_code)
    WHERE employee_code IS NOT NULL;

CREATE TABLE customers (
    id VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    name VARCHAR(255),
    phone VARCHAR(255),
    id_number VARCHAR(255),
    dob DATE,
    address VARCHAR(255),
    user_id VARCHAR(255),
    CONSTRAINT uk_customers_user UNIQUE (user_id),
    CONSTRAINT fk_customers_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE drivers (
    id VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    user_id VARCHAR(255),
    name VARCHAR(100),
    dob DATE,
    phone VARCHAR(255),
    license_number VARCHAR(255),
    license_expiration DATE,
    address VARCHAR(255),
    avatar_url TEXT,
    account_provisioned_by_user_id VARCHAR(255),
    account_provisioned_by_name VARCHAR(255),
    account_provisioned_by_role VARCHAR(255),
    account_provisioned_at TIMESTAMP,
    CONSTRAINT ck_drivers_provisioned_role CHECK (
        account_provisioned_by_role IS NULL
        OR account_provisioned_by_role IN ('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER', 'CUSTOMER')
    )
);

CREATE TABLE vehicles (
    id VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    license_plate VARCHAR(255) NOT NULL,
    vehicle_type VARCHAR(50),
    capacity DOUBLE PRECISION,
    status VARCHAR(255),
    CONSTRAINT uk_vehicles_license_plate UNIQUE (license_plate),
    CONSTRAINT ck_vehicles_status CHECK (
        status IS NULL OR status IN ('AVAILABLE', 'IN_TRIP', 'MAINTENANCE', 'INACTIVE')
    )
);

CREATE TABLE contracts (
    id VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    contract_code VARCHAR(255),
    customer_id VARCHAR(255),
    signed_date DATE,
    start_date DATE,
    end_date DATE,
    cargo_description VARCHAR(1000),
    cargo_type VARCHAR(255),
    freight_rate_per_ton_km DOUBLE PRECISION,
    estimated_distance_km DOUBLE PRECISION,
    estimated_cargo_weight_ton DOUBLE PRECISION,
    value_mode VARCHAR(255) DEFAULT 'PER_TRIP' NOT NULL,
    contract_value DOUBLE PRECISION,
    deposit_required BOOLEAN DEFAULT FALSE,
    deposit_scope VARCHAR(255),
    deposit_type VARCHAR(255),
    deposit_value DOUBLE PRECISION,
    deposit_usage VARCHAR(255),
    deposit_due_days INTEGER,
    deposit_terms VARCHAR(1000),
    status VARCHAR(255),
    CONSTRAINT fk_contracts_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT ck_contracts_value_mode CHECK (value_mode IN ('PER_TRIP', 'AGREED_VALUE')),
    CONSTRAINT ck_contracts_deposit_scope CHECK (
        deposit_scope IS NULL OR deposit_scope IN ('CONTRACT', 'TRIP')
    ),
    CONSTRAINT ck_contracts_deposit_type CHECK (
        deposit_type IS NULL OR deposit_type IN ('FIXED', 'PERCENTAGE')
    ),
    CONSTRAINT ck_contracts_deposit_usage CHECK (
        deposit_usage IS NULL OR deposit_usage IN ('APPLY_TO_INVOICE', 'SECURITY_HOLD')
    ),
    CONSTRAINT ck_contracts_status CHECK (
        status IS NULL OR status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED')
    )
);

CREATE INDEX idx_contracts_customer ON contracts(customer_id);

CREATE TABLE trips (
    id VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    vehicle_id VARCHAR(255),
    driver_id VARCHAR(255),
    customer_id VARCHAR(255),
    contract_id VARCHAR(255),
    start_location VARCHAR(255),
    end_location VARCHAR(255),
    start_time VARCHAR(255),
    end_time VARCHAR(255),
    distance_km DOUBLE PRECISION,
    cargo_weight_ton DOUBLE PRECISION,
    freight_rate_per_ton_km DOUBLE PRECISION,
    freight_amount DOUBLE PRECISION,
    status VARCHAR(255),
    CONSTRAINT fk_trips_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    CONSTRAINT fk_trips_driver FOREIGN KEY (driver_id) REFERENCES drivers(id),
    CONSTRAINT fk_trips_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_trips_contract FOREIGN KEY (contract_id) REFERENCES contracts(id),
    CONSTRAINT ck_trips_status CHECK (
        status IS NULL OR status IN ('CREATED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')
    )
);

CREATE INDEX idx_trips_vehicle ON trips(vehicle_id);
CREATE INDEX idx_trips_driver ON trips(driver_id);
CREATE INDEX idx_trips_customer ON trips(customer_id);
CREATE INDEX idx_trips_contract ON trips(contract_id);
CREATE INDEX idx_trips_status ON trips(status);

CREATE TABLE maintenances (
    id VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    vehicle_id VARCHAR(255),
    maintenance_type VARCHAR(255),
    maintenance_types VARCHAR(1000),
    description VARCHAR(255),
    cost DOUBLE PRECISION,
    maintenance_date DATE,
    next_maintenance_date DATE,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    status VARCHAR(255),
    CONSTRAINT fk_maintenances_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    CONSTRAINT ck_maintenances_status CHECK (
        status IS NULL OR status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')
    )
);

CREATE INDEX idx_maintenances_vehicle ON maintenances(vehicle_id);
CREATE INDEX idx_maintenances_status ON maintenances(status);

CREATE TABLE expenses (
    id VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    expense_type VARCHAR(255),
    expense_types VARCHAR(1000),
    amount DOUBLE PRECISION,
    description VARCHAR(500),
    receipt_image_url TEXT,
    expense_date DATE,
    trip_id VARCHAR(255),
    status VARCHAR(255) DEFAULT 'PENDING' NOT NULL,
    recorded_by_user_id VARCHAR(255),
    recorded_by_name VARCHAR(255),
    recorded_by_role VARCHAR(255),
    reviewed_by_user_id VARCHAR(255),
    reviewed_by_name VARCHAR(255),
    reviewed_at TIMESTAMP,
    review_note VARCHAR(500),
    CONSTRAINT fk_expenses_trip FOREIGN KEY (trip_id) REFERENCES trips(id),
    CONSTRAINT ck_expenses_type CHECK (
        expense_type IS NULL OR expense_type IN ('FUEL', 'TOLL', 'MAINTENANCE', 'OTHER')
    ),
    CONSTRAINT ck_expenses_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_expenses_recorded_role CHECK (
        recorded_by_role IS NULL
        OR recorded_by_role IN ('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER', 'CUSTOMER')
    )
);

CREATE INDEX idx_expenses_trip ON expenses(trip_id);
CREATE INDEX idx_expenses_status ON expenses(status);

CREATE TABLE invoices (
    id VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    invoice_number VARCHAR(255),
    customer_id VARCHAR(255),
    trip_id VARCHAR(255),
    total_amount DOUBLE PRECISION,
    deposit_applied_amount DOUBLE PRECISION DEFAULT 0,
    paid_amount DOUBLE PRECISION DEFAULT 0,
    issue_date DATE,
    due_date DATE,
    status VARCHAR(255),
    CONSTRAINT uk_invoices_number UNIQUE (invoice_number),
    CONSTRAINT fk_invoices_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_invoices_trip FOREIGN KEY (trip_id) REFERENCES trips(id),
    CONSTRAINT ck_invoices_status CHECK (
        status IS NULL OR status IN ('PENDING', 'PAID', 'OVERDUE', 'CANCELLED')
    )
);

CREATE INDEX idx_invoices_customer ON invoices(customer_id);
CREATE INDEX idx_invoices_trip ON invoices(trip_id);
CREATE INDEX idx_invoices_status ON invoices(status);

CREATE TABLE cargo_rates (
    id VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    cargo_type VARCHAR(50) NOT NULL,
    cargo_label VARCHAR(255) NOT NULL,
    rate_per_ton_km DOUBLE PRECISION NOT NULL,
    CONSTRAINT uk_cargo_rates_type UNIQUE (cargo_type)
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
    ('cargo-rate-other', 'OTHER', 'Khác', 20000);

CREATE TABLE deposits (
    id VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    receipt_number VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    contract_id VARCHAR(255),
    trip_id VARCHAR(255),
    amount DOUBLE PRECISION NOT NULL,
    allocated_amount DOUBLE PRECISION DEFAULT 0 NOT NULL,
    refunded_amount DOUBLE PRECISION DEFAULT 0 NOT NULL,
    received_date DATE NOT NULL,
    payment_method VARCHAR(255) NOT NULL,
    bank_name VARCHAR(255),
    account_holder VARCHAR(255),
    account_number VARCHAR(255),
    reference_number VARCHAR(255),
    note VARCHAR(1000),
    status VARCHAR(255) NOT NULL,
    CONSTRAINT uk_deposits_receipt_number UNIQUE (receipt_number),
    CONSTRAINT fk_deposits_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_deposits_contract FOREIGN KEY (contract_id) REFERENCES contracts(id),
    CONSTRAINT fk_deposits_trip FOREIGN KEY (trip_id) REFERENCES trips(id),
    CONSTRAINT ck_deposits_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_deposits_balances_non_negative CHECK (
        allocated_amount >= 0 AND refunded_amount >= 0
    ),
    CONSTRAINT ck_deposits_balances_total CHECK (
        allocated_amount + refunded_amount <= amount
    ),
    CONSTRAINT ck_deposits_payment_method CHECK (
        payment_method IN ('CASH', 'BANK_TRANSFER', 'CARD', 'OTHER')
    ),
    CONSTRAINT ck_deposits_status CHECK (
        status IN ('AVAILABLE', 'PARTIALLY_APPLIED', 'APPLIED', 'PARTIALLY_REFUNDED', 'REFUNDED', 'CANCELLED')
    )
);

CREATE INDEX idx_deposits_customer ON deposits(customer_id);
CREATE INDEX idx_deposits_contract ON deposits(contract_id);
CREATE INDEX idx_deposits_trip ON deposits(trip_id);

CREATE TABLE invoice_deposit_allocations (
    id VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    deposit_id VARCHAR(255) NOT NULL,
    invoice_id VARCHAR(255) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    CONSTRAINT uk_deposit_invoice UNIQUE (deposit_id, invoice_id),
    CONSTRAINT fk_invoice_allocations_deposit FOREIGN KEY (deposit_id) REFERENCES deposits(id),
    CONSTRAINT fk_invoice_allocations_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    CONSTRAINT ck_invoice_allocation_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_deposit_allocations_invoice
    ON invoice_deposit_allocations(invoice_id);

CREATE TABLE deposit_refunds (
    id VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    deposit_id VARCHAR(255) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    refund_date DATE NOT NULL,
    payment_method VARCHAR(255) NOT NULL,
    bank_name VARCHAR(255),
    account_holder VARCHAR(255),
    account_number VARCHAR(255),
    reference_number VARCHAR(255),
    note VARCHAR(1000),
    CONSTRAINT fk_deposit_refunds_deposit FOREIGN KEY (deposit_id) REFERENCES deposits(id),
    CONSTRAINT ck_deposit_refunds_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_deposit_refunds_payment_method CHECK (
        payment_method IN ('CASH', 'BANK_TRANSFER', 'CARD', 'OTHER')
    )
);

CREATE INDEX idx_deposit_refunds_deposit ON deposit_refunds(deposit_id);

CREATE TABLE invoice_payments (
    id VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    invoice_id VARCHAR(255) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    payment_date DATE NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    bank_name VARCHAR(255),
    account_holder VARCHAR(255),
    account_number VARCHAR(255),
    transaction_reference VARCHAR(255),
    note VARCHAR(1000),
    CONSTRAINT uk_invoice_payment_invoice UNIQUE (invoice_id),
    CONSTRAINT fk_invoice_payments_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    CONSTRAINT ck_invoice_payment_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_invoice_payment_method CHECK (
        payment_method IN ('CASH', 'BANK_TRANSFER', 'CARD', 'OTHER')
    ),
    CONSTRAINT ck_invoice_bank_transfer_details CHECK (
        payment_method <> 'BANK_TRANSFER'
        OR (
            bank_name IS NOT NULL
            AND TRIM(bank_name) <> ''
            AND transaction_reference IS NOT NULL
            AND TRIM(transaction_reference) <> ''
        )
    )
);

CREATE INDEX idx_invoice_payments_invoice ON invoice_payments(invoice_id);
CREATE INDEX idx_invoice_payments_date ON invoice_payments(payment_date);
