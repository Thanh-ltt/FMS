-- FMS test data set B: operational states and edge cases.
-- All dates are relative to CURRENT_DATE so filters and warnings remain testable.
-- Every active account in this file uses password: Test@12345

\set ON_ERROR_STOP on

BEGIN;

-- Remove only data belonging to this test set, including records later created
-- from the UI for the seeded customer, contracts, trips, vehicles, or drivers.
DELETE FROM invoice_payments
WHERE invoice_id IN (
    SELECT id FROM invoices
    WHERE id LIKE 'test-b-%'
       OR customer_id LIKE 'test-b-%'
       OR trip_id IN (
           SELECT id FROM trips
           WHERE id LIKE 'test-b-%'
              OR customer_id LIKE 'test-b-%'
              OR contract_id LIKE 'test-b-%'
       )
);

DELETE FROM invoice_deposit_allocations
WHERE invoice_id IN (
        SELECT id FROM invoices
        WHERE id LIKE 'test-b-%' OR customer_id LIKE 'test-b-%'
    )
   OR deposit_id IN (
        SELECT id FROM deposits
        WHERE id LIKE 'test-b-%' OR customer_id LIKE 'test-b-%'
    );

DELETE FROM deposit_refunds
WHERE deposit_id IN (
    SELECT id FROM deposits
    WHERE id LIKE 'test-b-%' OR customer_id LIKE 'test-b-%'
);

DELETE FROM invoices
WHERE id LIKE 'test-b-%'
   OR customer_id LIKE 'test-b-%'
   OR trip_id IN (
       SELECT id FROM trips
       WHERE id LIKE 'test-b-%'
          OR customer_id LIKE 'test-b-%'
          OR contract_id LIKE 'test-b-%'
   );

DELETE FROM deposits
WHERE id LIKE 'test-b-%'
   OR customer_id LIKE 'test-b-%'
   OR contract_id LIKE 'test-b-%'
   OR trip_id IN (
       SELECT id FROM trips
       WHERE id LIKE 'test-b-%'
          OR customer_id LIKE 'test-b-%'
          OR contract_id LIKE 'test-b-%'
   );

DELETE FROM expenses
WHERE id LIKE 'test-b-%'
   OR trip_id IN (
       SELECT id FROM trips
       WHERE id LIKE 'test-b-%'
          OR customer_id LIKE 'test-b-%'
          OR contract_id LIKE 'test-b-%'
   );

DELETE FROM maintenances
WHERE id LIKE 'test-b-%' OR vehicle_id LIKE 'test-b-%';

DELETE FROM trips
WHERE id LIKE 'test-b-%'
   OR customer_id LIKE 'test-b-%'
   OR contract_id LIKE 'test-b-%'
   OR vehicle_id LIKE 'test-b-%'
   OR driver_id LIKE 'test-b-%';

DELETE FROM contracts
WHERE id LIKE 'test-b-%' OR customer_id LIKE 'test-b-%';

DELETE FROM customers
WHERE id LIKE 'test-b-%' OR user_id LIKE 'test-b-%';

DELETE FROM drivers
WHERE id LIKE 'test-b-%' OR user_id LIKE 'test-b-%';

DELETE FROM vehicles
WHERE id LIKE 'test-b-%';

DELETE FROM users
WHERE id LIKE 'test-b-%';

-- The BCrypt value below is the verified hash for Test@12345.
INSERT INTO users (
    id, username, password, role, employee_code, full_name, phone, email,
    address, id_number, dob, gender, position, hire_date, active,
    created_at, updated_at
)
VALUES
    (
        'test-b-user-admin', 'testb_admin',
        '$2a$10$zssPA6dPryJJiWqHZw7VAuhPvutHIsvuIcb26VBda7.h1.sWwSLme',
        'ADMIN', 'TESTB-ADM-01', 'Võ Đức Thành', '0902000001',
        'testb.admin@fms.local', '18 Pasteur, Quận 1, TP. Hồ Chí Minh',
        '079080000001', DATE '1987-02-10', 'MALE', 'Quản trị kiểm thử',
        CURRENT_DATE - 1200, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'test-b-user-manager', 'testb_manager',
        '$2a$10$zssPA6dPryJJiWqHZw7VAuhPvutHIsvuIcb26VBda7.h1.sWwSLme',
        'MANAGER', 'TESTB-MGR-01', 'Đặng Ngọc Lan', '0902000002',
        'testb.manager@fms.local', '36 Cộng Hòa, Tân Bình, TP. Hồ Chí Minh',
        '079081000002', DATE '1991-06-24', 'FEMALE', 'Điều phối trưởng',
        CURRENT_DATE - 800, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'test-b-user-accountant', 'testb_account',
        '$2a$10$zssPA6dPryJJiWqHZw7VAuhPvutHIsvuIcb26VBda7.h1.sWwSLme',
        'ACCOUNTANT', 'TESTB-ACC-01', 'Bùi Thanh Tâm', '0902000003',
        'testb.account@fms.local', '91 Hoàng Văn Thụ, Phú Nhuận, TP. Hồ Chí Minh',
        '079082000003', DATE '1993-12-02', 'OTHER', 'Kế toán công nợ',
        CURRENT_DATE - 600, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'test-b-user-inactive', 'testb_inactive',
        '$2a$10$zssPA6dPryJJiWqHZw7VAuhPvutHIsvuIcb26VBda7.h1.sWwSLme',
        'MANAGER', 'TESTB-MGR-LOCK', 'Nhân viên đã khóa', '0902000004',
        'testb.inactive@fms.local', '10 Trường Sơn, Tân Bình, TP. Hồ Chí Minh',
        '079083000004', DATE '1990-08-14', 'MALE', 'Tài khoản ngưng hoạt động',
        CURRENT_DATE - 300, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'test-b-user-driver-1', 'testb_driver1',
        '$2a$10$zssPA6dPryJJiWqHZw7VAuhPvutHIsvuIcb26VBda7.h1.sWwSLme',
        'DRIVER', NULL, 'Nguyễn Văn Dũng', '0902000005', NULL,
        '62 Kinh Dương Vương, Bình Tân, TP. Hồ Chí Minh',
        '079084000005', DATE '1985-04-19', 'MALE', 'Tài xế',
        CURRENT_DATE - 1400, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'test-b-user-driver-2', 'testb_driver2',
        '$2a$10$zssPA6dPryJJiWqHZw7VAuhPvutHIsvuIcb26VBda7.h1.sWwSLme',
        'DRIVER', NULL, 'Trương Hải Nam', '0902000006', NULL,
        '54 Quốc lộ 1A, Quận 12, TP. Hồ Chí Minh',
        '079085000006', DATE '1989-10-30', 'MALE', 'Tài xế',
        CURRENT_DATE - 1000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'test-b-user-customer', 'testb_customer',
        '$2a$10$zssPA6dPryJJiWqHZw7VAuhPvutHIsvuIcb26VBda7.h1.sWwSLme',
        'CUSTOMER', NULL, 'Công ty Minh Long', '0902000007', NULL,
        '15 Quốc lộ 13, Thủ Đức, TP. Hồ Chí Minh',
        '079086000007', DATE '1988-01-25', NULL, NULL,
        NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    );

INSERT INTO customers (
    id, name, phone, id_number, dob, address, user_id, created_at, updated_at
)
VALUES (
    'test-b-customer-001', 'Công ty Minh Long', '0902000007', '079086000007',
    DATE '1988-01-25', '15 Quốc lộ 13, Thủ Đức, TP. Hồ Chí Minh',
    'test-b-user-customer', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO drivers (
    id, user_id, name, dob, phone, license_number, license_expiration,
    address, avatar_url, created_at, updated_at
)
VALUES
    (
        'test-b-driver-001', 'test-b-user-driver-1', 'Nguyễn Văn Dũng',
        DATE '1985-04-19', '0902000005', '790098765432', CURRENT_DATE + 365,
        '62 Kinh Dương Vương, Bình Tân, TP. Hồ Chí Minh', NULL,
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'test-b-driver-002', 'test-b-user-driver-2', 'Trương Hải Nam',
        DATE '1989-10-30', '0902000006', '790087654321', CURRENT_DATE + 45,
        '54 Quốc lộ 1A, Quận 12, TP. Hồ Chí Minh', NULL,
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    );

INSERT INTO vehicles (
    id, license_plate, vehicle_type, capacity, status, created_at, updated_at
)
VALUES
    (
        'test-b-vehicle-in-trip', '51C-987.65', 'HEAVY_DUTY_TRUCK', 15.0,
        'IN_TRIP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'test-b-vehicle-maintenance', '50H-345.67', 'REFRIGERATED_TRUCK', 8.0,
        'MAINTENANCE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'test-b-vehicle-inactive', '51D-876.54', 'CARGO_VAN', 1.2,
        'INACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'test-b-vehicle-available', '51C-456.78', 'FLATBED_TRUCK', 12.0,
        'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    );

INSERT INTO contracts (
    id, contract_code, customer_id, signed_date, start_date, end_date,
    cargo_description, cargo_type, freight_rate_per_ton_km,
    estimated_distance_km, estimated_cargo_weight_ton, value_mode, contract_value,
    deposit_required, deposit_scope, deposit_type, deposit_value,
    deposit_usage, deposit_due_days, deposit_terms, status,
    created_at, updated_at
)
VALUES
    (
        'test-b-contract-future', 'HD-TEST-B-FUTURE', 'test-b-customer-001',
        CURRENT_DATE, CURRENT_DATE + 10, CURRENT_DATE + 90,
        'Thực phẩm đông lạnh từ -18°C đến -15°C.', 'COLD', 32000,
        55.0, 6.0, 'PER_TRIP', NULL,
        TRUE, 'TRIP', 'FIXED', 1500000, 'SECURITY_HOLD', 3,
        'Cọc bảo đảm 1.500.000đ cho từng chuyến, không cấn vào hóa đơn.',
        'DRAFT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'test-b-contract-active', 'HD-TEST-B-ACTIVE', 'test-b-customer-001',
        CURRENT_DATE - 60, CURRENT_DATE - 45, CURRENT_DATE + 45,
        'Máy móc công nghiệp có kiện gỗ và điểm nâng hạ riêng.',
        'MACHINERY', 30000, 80.0, 10.0, 'PER_TRIP', NULL,
        TRUE, 'TRIP', 'PERCENTAGE', 25, 'APPLY_TO_INVOICE', 0,
        'Mỗi chuyến phải nhận đủ cọc 25% cước trước khi khởi hành.',
        'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP
    ),
    (
        'test-b-contract-expired', 'HD-TEST-B-EXPIRED', 'test-b-customer-001',
        CURRENT_DATE - 200, CURRENT_DATE - 180, CURRENT_DATE - 30,
        'Thiết bị thủy tinh đóng kiện chống sốc.', 'FRAGILE', 28000,
        8.2, 2.0, 'AGREED_VALUE', 25000000,
        FALSE, NULL, NULL, NULL, NULL, NULL, NULL,
        'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '200 days', CURRENT_TIMESTAMP
    ),
    (
        'test-b-contract-cancelled', 'HD-TEST-B-CANCELLED', 'test-b-customer-001',
        CURRENT_DATE - 90, CURRENT_DATE - 80, CURRENT_DATE + 30,
        'Hàng tổng hợp, hợp đồng đã hủy để kiểm tra thao tác xóa.',
        'OTHER', 20000, 30.0, 3.0, 'PER_TRIP', NULL,
        FALSE, NULL, NULL, NULL, NULL, NULL, NULL,
        'CANCELLED', CURRENT_TIMESTAMP - INTERVAL '90 days', CURRENT_TIMESTAMP
    );

INSERT INTO trips (
    id, vehicle_id, driver_id, customer_id, contract_id,
    start_location, end_location, start_time, end_time,
    distance_km, cargo_weight_ton, freight_rate_per_ton_km,
    freight_amount, status, created_at, updated_at
)
VALUES
    (
        'test-b-trip-in-progress', 'test-b-vehicle-in-trip', 'test-b-driver-001',
        'test-b-customer-001', 'test-b-contract-active',
        'Cảng Cát Lái, Thủ Đức, TP. Hồ Chí Minh',
        'Khu công nghiệp Tân Tạo, Bình Tân, TP. Hồ Chí Minh',
        to_char(CURRENT_TIMESTAMP - INTERVAL '1 hour', 'YYYY-MM-DD"T"HH24:MI'),
        to_char(CURRENT_TIMESTAMP + INTERVAL '4 hours', 'YYYY-MM-DD"T"HH24:MI'),
        35.8, 10.0, 30000, 10740000, 'IN_PROGRESS',
        CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP
    ),
    (
        'test-b-trip-missing-deposit', 'test-b-vehicle-available', 'test-b-driver-002',
        'test-b-customer-001', 'test-b-contract-active',
        'Khu công nghiệp Sóng Thần, Dĩ An, Bình Dương',
        'Cảng Hiệp Phước, Nhà Bè, TP. Hồ Chí Minh',
        to_char(CURRENT_TIMESTAMP + INTERVAL '7 days', 'YYYY-MM-DD"T"08:00'),
        to_char(CURRENT_TIMESTAMP + INTERVAL '7 days', 'YYYY-MM-DD"T"13:00'),
        80.0, 6.0, 30000, 14400000, 'CREATED',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'test-b-trip-overdue', 'test-b-vehicle-available', 'test-b-driver-002',
        'test-b-customer-001', 'test-b-contract-expired',
        'Kho Minh Long, Thủ Đức, TP. Hồ Chí Minh',
        '120 Nguyễn Văn Linh, Quận 7, TP. Hồ Chí Minh',
        to_char(CURRENT_TIMESTAMP - INTERVAL '40 days', 'YYYY-MM-DD"T"08:00'),
        to_char(CURRENT_TIMESTAMP - INTERVAL '40 days', 'YYYY-MM-DD"T"10:00'),
        8.2, 2.0, 28000, 459200, 'COMPLETED',
        CURRENT_TIMESTAMP - INTERVAL '42 days', CURRENT_TIMESTAMP - INTERVAL '40 days'
    ),
    (
        'test-b-trip-reinvoice', 'test-b-vehicle-available', 'test-b-driver-002',
        'test-b-customer-001', 'test-b-contract-active',
        '15 Quốc lộ 13, Thủ Đức, TP. Hồ Chí Minh',
        'Kho Tân Tạo, Bình Tân, TP. Hồ Chí Minh',
        to_char(CURRENT_TIMESTAMP - INTERVAL '5 days', 'YYYY-MM-DD"T"07:00'),
        to_char(CURRENT_TIMESTAMP - INTERVAL '5 days', 'YYYY-MM-DD"T"10:30'),
        25.0, 3.0, 30000, 2250000, 'COMPLETED',
        CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '5 days'
    ),
    (
        'test-b-trip-cancelled', 'test-b-vehicle-available', 'test-b-driver-002',
        'test-b-customer-001', 'test-b-contract-active',
        'Bình Tân, TP. Hồ Chí Minh', 'Long An',
        to_char(CURRENT_TIMESTAMP + INTERVAL '3 days', 'YYYY-MM-DD"T"09:00'),
        to_char(CURRENT_TIMESTAMP + INTERVAL '3 days', 'YYYY-MM-DD"T"12:00'),
        30.0, 4.0, 30000, 3600000, 'CANCELLED',
        CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '1 day'
    );

INSERT INTO maintenances (
    id, vehicle_id, maintenance_type, maintenance_types, description, cost,
    maintenance_date, next_maintenance_date, started_at, completed_at,
    status, created_at, updated_at
)
VALUES
    (
        'test-b-maintenance-active', 'test-b-vehicle-maintenance', 'OIL_CHANGE',
        'OIL_CHANGE,BRAKE,BATTERY_ELECTRIC',
        'Thay dầu, kiểm tra phanh và hệ thống điện/ắc quy.',
        5800000, CURRENT_DATE, CURRENT_DATE + 180,
        CURRENT_TIMESTAMP - INTERVAL '3 hours', NULL, 'IN_PROGRESS',
        CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP
    ),
    (
        'test-b-maintenance-pending', 'test-b-vehicle-available', 'PERIODIC',
        'PERIODIC,INSPECTION', 'Bảo dưỡng định kỳ kết hợp chuẩn bị đăng kiểm.',
        1800000, CURRENT_DATE + 14, CURRENT_DATE + 194,
        NULL, NULL, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'test-b-maintenance-cancelled', 'test-b-vehicle-inactive', 'CLEANING',
        'CLEANING', 'Phiếu đã hủy để kiểm tra quyền xóa của quản trị viên.',
        0, CURRENT_DATE + 7, NULL, NULL, NULL, 'CANCELLED',
        CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '1 day'
    );

INSERT INTO expenses (
    id, expense_type, expense_types, amount, description, expense_date,
    trip_id, created_at, updated_at
)
VALUES
    (
        'test-b-expense-in-progress', 'FUEL', 'FUEL,TOLL', 2300000,
        'Nhiên liệu và cầu đường của chuyến đang vận chuyển.',
        CURRENT_DATE, 'test-b-trip-in-progress', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'test-b-expense-overdue', 'FUEL', 'FUEL,MAINTENANCE', 180000,
        'Nhiên liệu và xử lý kỹ thuật nhỏ cho chuyến cũ.',
        CURRENT_DATE - 40, 'test-b-trip-overdue',
        CURRENT_TIMESTAMP - INTERVAL '40 days', CURRENT_TIMESTAMP - INTERVAL '40 days'
    ),
    (
        'test-b-expense-reinvoice', 'FUEL', 'FUEL', 450000,
        'Nhiên liệu chuyến có hóa đơn đã hủy.',
        CURRENT_DATE - 5, 'test-b-trip-reinvoice',
        CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days'
    );

INSERT INTO deposits (
    id, receipt_number, customer_id, contract_id, trip_id, amount,
    allocated_amount, refunded_amount, received_date, payment_method,
    bank_name, account_holder, account_number, reference_number, note,
    status, created_at, updated_at
)
VALUES
    (
        'test-b-deposit-trip', 'PC-TEST-B-TRIP', 'test-b-customer-001',
        'test-b-contract-active', 'test-b-trip-in-progress',
        2685000, 0, 0, CURRENT_DATE - 1, 'BANK_TRANSFER',
        'BIDV', 'CONG TY MINH LONG', '9988776655', 'BIDV-TEST-B-TRIP',
        'Đủ 25% cước của chuyến đang vận chuyển.', 'AVAILABLE',
        CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day'
    ),
    (
        'test-b-deposit-refunded', 'PC-TEST-B-REFUND', 'test-b-customer-001',
        'test-b-contract-future', NULL, 1500000, 0, 500000,
        CURRENT_DATE - 5, 'CASH', NULL, NULL, NULL, NULL,
        'Cọc bảo đảm đã hoàn lại một phần.', 'PARTIALLY_REFUNDED',
        CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '2 days'
    );

INSERT INTO deposit_refunds (
    id, deposit_id, amount, refund_date, payment_method, bank_name,
    account_holder, account_number, reference_number, note,
    created_at, updated_at
)
VALUES (
    'test-b-refund-001', 'test-b-deposit-refunded', 500000,
    CURRENT_DATE - 2, 'CASH', NULL, NULL, NULL, NULL,
    'Hoàn một phần cọc theo thỏa thuận.',
    CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days'
);

INSERT INTO invoices (
    id, invoice_number, customer_id, trip_id, total_amount,
    deposit_applied_amount, paid_amount, issue_date, due_date, status,
    created_at, updated_at
)
VALUES
    (
        'test-b-invoice-overdue', 'HDON-TEST-B-OVERDUE', 'test-b-customer-001',
        'test-b-trip-overdue', 459200, 0, 0,
        CURRENT_DATE - 39, CURRENT_DATE - 20, 'PENDING',
        CURRENT_TIMESTAMP - INTERVAL '39 days', CURRENT_TIMESTAMP - INTERVAL '39 days'
    ),
    (
        'test-b-invoice-cancelled', 'HDON-TEST-B-CANCELLED', 'test-b-customer-001',
        'test-b-trip-reinvoice', 2250000, 0, 0,
        CURRENT_DATE - 4, CURRENT_DATE + 10, 'CANCELLED',
        CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '3 days'
    );

\if :{?FMS_TEST_ROLLBACK}
ROLLBACK;
\else
COMMIT;
\endif
