-- FMS test data set A: complete operating flow.
-- All dates are relative to CURRENT_DATE so the data remains useful over time.
-- Every account in this file uses password: Test@12345

\set ON_ERROR_STOP on

BEGIN;

-- Remove only data belonging to this test set, including records later created
-- from the UI for the seeded customer, contract, trips, vehicles, or drivers.
DELETE FROM invoice_payments
WHERE invoice_id IN (
    SELECT id FROM invoices
    WHERE id LIKE 'test-a-%'
       OR customer_id LIKE 'test-a-%'
       OR trip_id IN (
           SELECT id FROM trips
           WHERE id LIKE 'test-a-%'
              OR customer_id LIKE 'test-a-%'
              OR contract_id LIKE 'test-a-%'
       )
);

DELETE FROM invoice_deposit_allocations
WHERE invoice_id IN (
        SELECT id FROM invoices
        WHERE id LIKE 'test-a-%' OR customer_id LIKE 'test-a-%'
    )
   OR deposit_id IN (
        SELECT id FROM deposits
        WHERE id LIKE 'test-a-%' OR customer_id LIKE 'test-a-%'
    );

DELETE FROM deposit_refunds
WHERE deposit_id IN (
    SELECT id FROM deposits
    WHERE id LIKE 'test-a-%' OR customer_id LIKE 'test-a-%'
);

DELETE FROM invoices
WHERE id LIKE 'test-a-%'
   OR customer_id LIKE 'test-a-%'
   OR trip_id IN (
       SELECT id FROM trips
       WHERE id LIKE 'test-a-%'
          OR customer_id LIKE 'test-a-%'
          OR contract_id LIKE 'test-a-%'
   );

DELETE FROM deposits
WHERE id LIKE 'test-a-%'
   OR customer_id LIKE 'test-a-%'
   OR contract_id LIKE 'test-a-%'
   OR trip_id IN (
       SELECT id FROM trips
       WHERE id LIKE 'test-a-%'
          OR customer_id LIKE 'test-a-%'
          OR contract_id LIKE 'test-a-%'
   );

DELETE FROM expenses
WHERE id LIKE 'test-a-%'
   OR trip_id IN (
       SELECT id FROM trips
       WHERE id LIKE 'test-a-%'
          OR customer_id LIKE 'test-a-%'
          OR contract_id LIKE 'test-a-%'
   );

DELETE FROM maintenances
WHERE id LIKE 'test-a-%' OR vehicle_id LIKE 'test-a-%';

DELETE FROM trips
WHERE id LIKE 'test-a-%'
   OR customer_id LIKE 'test-a-%'
   OR contract_id LIKE 'test-a-%'
   OR vehicle_id LIKE 'test-a-%'
   OR driver_id LIKE 'test-a-%';

DELETE FROM contracts
WHERE id LIKE 'test-a-%' OR customer_id LIKE 'test-a-%';

DELETE FROM customers
WHERE id LIKE 'test-a-%' OR user_id LIKE 'test-a-%';

DELETE FROM drivers
WHERE id LIKE 'test-a-%' OR user_id LIKE 'test-a-%';

DELETE FROM vehicles
WHERE id LIKE 'test-a-%';

DELETE FROM users
WHERE id LIKE 'test-a-%';

-- The BCrypt value below is the verified hash for Test@12345.
INSERT INTO users (
    id, username, password, role, employee_code, full_name, phone, email,
    address, id_number, dob, gender, position, hire_date, active,
    created_at, updated_at
)
VALUES
    (
        'test-a-user-admin', 'testa_admin',
        '$2a$10$zssPA6dPryJJiWqHZw7VAuhPvutHIsvuIcb26VBda7.h1.sWwSLme',
        'ADMIN', 'TESTA-ADM-01', 'Nguyễn Minh Quản', '0901000001',
        'testa.admin@fms.local', '12 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh',
        '079090000001', DATE '1990-03-15', 'MALE', 'Quản trị hệ thống',
        CURRENT_DATE - 900, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'test-a-user-manager', 'testa_manager',
        '$2a$10$zssPA6dPryJJiWqHZw7VAuhPvutHIsvuIcb26VBda7.h1.sWwSLme',
        'MANAGER', 'TESTA-MGR-01', 'Trần Thu Hà', '0901000002',
        'testa.manager@fms.local', '85 Điện Biên Phủ, Bình Thạnh, TP. Hồ Chí Minh',
        '079091000002', DATE '1991-07-20', 'FEMALE', 'Quản lý vận hành',
        CURRENT_DATE - 700, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'test-a-user-accountant', 'testa_account',
        '$2a$10$zssPA6dPryJJiWqHZw7VAuhPvutHIsvuIcb26VBda7.h1.sWwSLme',
        'ACCOUNTANT', 'TESTA-ACC-01', 'Lê Hoàng Anh', '0901000003',
        'testa.account@fms.local', '22 Phan Xích Long, Phú Nhuận, TP. Hồ Chí Minh',
        '079092000003', DATE '1992-11-08', 'OTHER', 'Kế toán vận tải',
        CURRENT_DATE - 500, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'test-a-user-driver', 'testa_driver',
        '$2a$10$zssPA6dPryJJiWqHZw7VAuhPvutHIsvuIcb26VBda7.h1.sWwSLme',
        'DRIVER', NULL, 'Phạm Quốc Bảo', '0901000004', NULL,
        '120 Lê Trọng Tấn, Tân Phú, TP. Hồ Chí Minh',
        '079093000004', DATE '1988-05-12', 'MALE', 'Tài xế',
        CURRENT_DATE - 1000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'test-a-user-customer', 'testa_customer',
        '$2a$10$zssPA6dPryJJiWqHZw7VAuhPvutHIsvuIcb26VBda7.h1.sWwSLme',
        'CUSTOMER', NULL, 'Công ty An Phát', '0901000005', NULL,
        '4418 Nguyễn Cửu Phú, Tân Tạo, Bình Tân, TP. Hồ Chí Minh',
        '079094000005', DATE '1989-09-18', NULL, NULL,
        NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    );

INSERT INTO customers (
    id, name, phone, id_number, dob, address, user_id, created_at, updated_at
)
VALUES (
    'test-a-customer-001', 'Công ty An Phát', '0901000005', '079094000005',
    DATE '1989-09-18',
    '4418 Nguyễn Cửu Phú, Tân Tạo, Bình Tân, TP. Hồ Chí Minh',
    'test-a-user-customer', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO drivers (
    id, user_id, name, dob, phone, license_number, license_expiration,
    address, avatar_url, created_at, updated_at
)
VALUES (
    'test-a-driver-001', 'test-a-user-driver', 'Phạm Quốc Bảo',
    DATE '1988-05-12', '0901000004', '790012345678', CURRENT_DATE + 730,
    '120 Lê Trọng Tấn, Tân Phú, TP. Hồ Chí Minh', NULL,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO vehicles (
    id, license_plate, vehicle_type, capacity, status, created_at, updated_at
)
VALUES
    (
        'test-a-vehicle-001', '51C-123.45', 'BOX_TRUCK', 8.0, 'AVAILABLE',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'test-a-vehicle-002', '51D-234.56', 'REFRIGERATED_TRUCK', 5.0, 'AVAILABLE',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    );

INSERT INTO contracts (
    id, contract_code, customer_id, signed_date, start_date, end_date,
    cargo_description, cargo_type, freight_rate_per_ton_km,
    estimated_distance_km, estimated_cargo_weight_ton, value_mode, contract_value,
    deposit_required, deposit_scope, deposit_type, deposit_value,
    deposit_usage, deposit_due_days, deposit_terms, status,
    created_at, updated_at
)
VALUES (
    'test-a-contract-001', 'HD-TEST-A-001', 'test-a-customer-001',
    CURRENT_DATE - 45, CURRENT_DATE - 30, CURRENT_DATE + 90,
    'Linh kiện điện tử đóng thùng carton, cần tránh nước và va đập mạnh.',
    'DRY', 20000, 42.5, 5.0, 'AGREED_VALUE', 50000000,
    TRUE, 'CONTRACT', 'PERCENTAGE', 20,
    'APPLY_TO_INVOICE', 0,
    'Cọc 20% giá trị hợp đồng; được cấn trừ vào hóa đơn vận chuyển.',
    'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO trips (
    id, vehicle_id, driver_id, customer_id, contract_id,
    start_location, end_location, start_time, end_time,
    distance_km, cargo_weight_ton, freight_rate_per_ton_km,
    freight_amount, status, created_at, updated_at
)
VALUES
    (
        'test-a-trip-completed', 'test-a-vehicle-001', 'test-a-driver-001',
        'test-a-customer-001', 'test-a-contract-001',
        '4418 Nguyễn Cửu Phú, Tân Tạo, Bình Tân, TP. Hồ Chí Minh',
        '742 Hương lộ 2, Bình Trị Đông, Bình Tân, TP. Hồ Chí Minh',
        to_char(CURRENT_TIMESTAMP - INTERVAL '10 days', 'YYYY-MM-DD"T"08:00'),
        to_char(CURRENT_TIMESTAMP - INTERVAL '10 days', 'YYYY-MM-DD"T"11:30'),
        42.5, 5.0, 20000, 4250000, 'COMPLETED',
        CURRENT_TIMESTAMP - INTERVAL '12 days', CURRENT_TIMESTAMP - INTERVAL '10 days'
    ),
    (
        'test-a-trip-upcoming', 'test-a-vehicle-001', 'test-a-driver-001',
        'test-a-customer-001', 'test-a-contract-001',
        'Kho An Phát, 4418 Nguyễn Cửu Phú, Bình Tân, TP. Hồ Chí Minh',
        'Khu công nghiệp Sóng Thần, Dĩ An, Bình Dương',
        to_char(CURRENT_TIMESTAMP + INTERVAL '7 days', 'YYYY-MM-DD"T"07:30'),
        to_char(CURRENT_TIMESTAMP + INTERVAL '7 days', 'YYYY-MM-DD"T"12:00'),
        60.0, 4.0, 20000, 4800000, 'CREATED',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    );

INSERT INTO maintenances (
    id, vehicle_id, maintenance_type, maintenance_types, description, cost,
    maintenance_date, next_maintenance_date, started_at, completed_at,
    status, created_at, updated_at
)
VALUES (
    'test-a-maintenance-001', 'test-a-vehicle-002', 'PERIODIC',
    'PERIODIC,OIL_CHANGE,TIRE',
    'Bảo dưỡng định kỳ, thay dầu động cơ và kiểm tra lốp.',
    3200000, CURRENT_DATE - 20, CURRENT_DATE + 70,
    CURRENT_TIMESTAMP - INTERVAL '20 days',
    CURRENT_TIMESTAMP - INTERVAL '19 days',
    'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '21 days',
    CURRENT_TIMESTAMP - INTERVAL '19 days'
);

INSERT INTO expenses (
    id, expense_type, expense_types, amount, description, expense_date,
    trip_id, created_at, updated_at
)
VALUES
    (
        'test-a-expense-001', 'FUEL', 'FUEL,TOLL', 1100000,
        'Dầu diesel và phí cầu đường cho chuyến hoàn tất.',
        CURRENT_DATE - 10, 'test-a-trip-completed',
        CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days'
    ),
    (
        'test-a-expense-002', 'OTHER', 'OTHER', 250000,
        'Bốc xếp hàng tại điểm giao.', CURRENT_DATE - 10,
        'test-a-trip-completed', CURRENT_TIMESTAMP - INTERVAL '10 days',
        CURRENT_TIMESTAMP - INTERVAL '10 days'
    );

INSERT INTO deposits (
    id, receipt_number, customer_id, contract_id, trip_id, amount,
    allocated_amount, refunded_amount, received_date, payment_method,
    bank_name, account_holder, account_number, reference_number, note,
    status, created_at, updated_at
)
VALUES (
    'test-a-deposit-001', 'PC-TEST-A-001', 'test-a-customer-001',
    'test-a-contract-001', NULL, 10000000, 1000000, 0,
    CURRENT_DATE - 25, 'BANK_TRANSFER', 'Vietcombank',
    'CONG TY AN PHAT', '0123456789', 'VCB-TEST-A-001',
    'Cọc theo hợp đồng, đã cấn một phần vào hóa đơn đầu tiên.',
    'PARTIALLY_APPLIED', CURRENT_TIMESTAMP - INTERVAL '25 days', CURRENT_TIMESTAMP
);

INSERT INTO invoices (
    id, invoice_number, customer_id, trip_id, total_amount,
    deposit_applied_amount, paid_amount, issue_date, due_date, status,
    created_at, updated_at
)
VALUES (
    'test-a-invoice-001', 'HDON-TEST-A-001', 'test-a-customer-001',
    'test-a-trip-completed', 4250000, 1000000, 4250000,
    CURRENT_DATE - 9, CURRENT_DATE + 6, 'PAID',
    CURRENT_TIMESTAMP - INTERVAL '9 days', CURRENT_TIMESTAMP - INTERVAL '8 days'
);

INSERT INTO invoice_deposit_allocations (
    id, deposit_id, invoice_id, amount, created_at, updated_at
)
VALUES (
    'test-a-allocation-001', 'test-a-deposit-001', 'test-a-invoice-001',
    1000000, CURRENT_TIMESTAMP - INTERVAL '9 days', CURRENT_TIMESTAMP - INTERVAL '9 days'
);

INSERT INTO invoice_payments (
    id, invoice_id, amount, payment_date, payment_method, bank_name,
    account_holder, account_number, transaction_reference, note,
    created_at, updated_at
)
VALUES (
    'test-a-payment-001', 'test-a-invoice-001', 3250000,
    CURRENT_DATE - 8, 'BANK_TRANSFER', 'Vietcombank',
    'CONG TY AN PHAT', '0123456789', 'VCB-PAY-TEST-A-001',
    'Thanh toán phần còn lại sau khi cấn cọc.',
    CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '8 days'
);

\if :{?FMS_TEST_ROLLBACK}
ROLLBACK;
\else
COMMIT;
\endif
