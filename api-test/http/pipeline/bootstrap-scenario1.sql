-- SeedFlow+ scenario1 재실행용 targeted bootstrap 스크립트
-- 전제: MariaDB / MySQL
-- 목적: 시나리오 관련 파이프라인 데이터만 정리하고 admin/sales/client 계정과 거래처를 다시 만든다.
-- 주의: 전체 tbl_client/tbl_employee 삭제를 피해서 락 대기를 줄인다.

START TRANSACTION;

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM tbl_notification_delivery;
DELETE FROM tbl_notification;

DELETE FROM tbl_approval_decision;
DELETE FROM tbl_approval_step;
DELETE FROM tbl_approval_request;

DELETE FROM tbl_sales_deal_log_detail;
DELETE FROM tbl_sales_deal_log;
DELETE FROM tbl_deal_sked;
DELETE FROM tbl_pers_sked;

DELETE FROM tbl_invoice_statement;
DELETE FROM tbl_payment;
DELETE FROM tbl_invoice;
DELETE FROM tbl_statement;

DELETE FROM tbl_order_detail;
DELETE FROM tbl_order_header;

DELETE FROM tbl_contract_detail;
DELETE FROM tbl_contract_header;

DELETE FROM tbl_quotation_detail;
DELETE FROM tbl_quotation_header;

DELETE FROM tbl_request_quotation_detail;
DELETE FROM tbl_request_quotation_header;

DELETE FROM tbl_sales_deal;

DELETE FROM tbl_account_score
WHERE client_id IN (
    SELECT client_id
    FROM tbl_client
    WHERE client_brn = '123-45-67890'
       OR manager_email = 'client@seedflow.com'
       OR client_brn = '223-45-67890'
       OR manager_email = 'client2@seedflow.com'
);

DELETE FROM tbl_client_crops
WHERE client_id IN (
    SELECT client_id
    FROM tbl_client
    WHERE client_brn = '123-45-67890'
       OR manager_email = 'client@seedflow.com'
       OR client_brn = '223-45-67890'
       OR manager_email = 'client2@seedflow.com'
);

DELETE FROM tbl_product_bookmark;
DELETE FROM tbl_product_compare_item;
DELETE FROM tbl_product_compare;
DELETE FROM tbl_product_feedback;
DELETE FROM tbl_product_price_history;
DELETE FROM tbl_product_tag;
DELETE FROM tbl_cultivation_time;
DELETE FROM tbl_tag;
DELETE FROM tbl_product;

SET FOREIGN_KEY_CHECKS = 1;

-- password1234!
SET @bcrypt_pw = '$2y$10$nr12HMi6ppnTT6755FZtReWp/5MVHfyIp5jVE.JrwYEuLtfMaYZim';

INSERT INTO tbl_employee (
    employee_code,
    employee_name,
    employee_email,
    employee_phone,
    address,
    created_at,
    updated_at
)
SELECT
    'TEMP-ADMIN',
    '관리자',
    'admin@seedflow.com',
    '010-0000-0001',
    '서울특별시 강남구 테헤란로 1',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_employee
    WHERE employee_email = 'admin@seedflow.com'
);

SET @admin_employee_id := (
    SELECT employee_id
    FROM tbl_employee
    WHERE employee_email = 'admin@seedflow.com'
    LIMIT 1
);

UPDATE tbl_employee
SET employee_code = CONCAT('EMP-', LPAD(employee_id, 4, '0')),
    employee_name = '관리자',
    employee_phone = '010-0000-0001',
    address = '서울특별시 강남구 테헤란로 1',
    updated_at = NOW()
WHERE employee_id = @admin_employee_id;

INSERT INTO tbl_employee (
    employee_code,
    employee_name,
    employee_email,
    employee_phone,
    address,
    created_at,
    updated_at
)
SELECT
    'TEMP-SALES',
    '영업담당자',
    'sales@seedflow.com',
    '010-0000-0002',
    '서울특별시 강남구 테헤란로 2',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_employee
    WHERE employee_email = 'sales@seedflow.com'
);

SET @sales_employee_id := (
    SELECT employee_id
    FROM tbl_employee
    WHERE employee_email = 'sales@seedflow.com'
    LIMIT 1
);

UPDATE tbl_employee
SET employee_code = CONCAT('EMP-', LPAD(employee_id, 4, '0')),
    employee_name = '영업담당자',
    employee_phone = '010-0000-0002',
    address = '서울특별시 강남구 테헤란로 2',
    updated_at = NOW()
WHERE employee_id = @sales_employee_id;

INSERT INTO tbl_client (
    client_code,
    client_name,
    client_brn,
    ceo_name,
    company_phone,
    address,
    latitude,
    longitude,
    client_type,
    manager_name,
    manager_phone,
    manager_email,
    employee_id,
    total_credit,
    used_credit,
    created_at,
    updated_at
)
SELECT
    'TEMP-CLIENT',
    '시드플로우 거래처',
    '123-45-67890',
    '거래처대표',
    '02-1234-5678',
    '서울특별시 송파구 올림픽로 300',
    NULL,
    NULL,
    'DISTRIBUTOR',
    '거래처담당자',
    '010-0000-0003',
    'client@seedflow.com',
    @sales_employee_id,
    50000000.00,
    0.00,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_client
    WHERE client_brn = '123-45-67890'
       OR manager_email = 'client@seedflow.com'
);

SET @client_id := (
    SELECT client_id
    FROM tbl_client
    WHERE client_brn = '123-45-67890'
       OR manager_email = 'client@seedflow.com'
    LIMIT 1
);

UPDATE tbl_client
SET client_code = CONCAT('CLNT-', LPAD(client_id, 4, '0')),
    client_name = '시드플로우 거래처',
    client_brn = '123-45-67890',
    ceo_name = '거래처대표',
    company_phone = '02-1234-5678',
    address = '서울특별시 송파구 올림픽로 300',
    latitude = NULL,
    longitude = NULL,
    client_type = 'DISTRIBUTOR',
    manager_name = '거래처담당자',
    manager_phone = '010-0000-0003',
    manager_email = 'client@seedflow.com',
    employee_id = @sales_employee_id,
    total_credit = 50000000.00,
    used_credit = 0.00,
    updated_at = NOW()
WHERE client_id = @client_id;

INSERT INTO tbl_client (
    client_code,
    client_name,
    client_brn,
    ceo_name,
    company_phone,
    address,
    latitude,
    longitude,
    client_type,
    manager_name,
    manager_phone,
    manager_email,
    employee_id,
    total_credit,
    used_credit,
    created_at,
    updated_at
)
SELECT
    'TEMP-CLIENT-2',
    '시드플로우 거래처2',
    '223-45-67890',
    '거래처대표2',
    '02-2234-5678',
    '서울특별시 강동구 올림픽로 620',
    NULL,
    NULL,
    'DISTRIBUTOR',
    '거래처담당자2',
    '010-0000-0004',
    'client2@seedflow.com',
    @sales_employee_id,
    50000000.00,
    0.00,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_client
    WHERE client_brn = '223-45-67890'
       OR manager_email = 'client2@seedflow.com'
);

SET @client2_id := (
    SELECT client_id
    FROM tbl_client
    WHERE client_brn = '223-45-67890'
       OR manager_email = 'client2@seedflow.com'
    LIMIT 1
);

UPDATE tbl_client
SET client_code = CONCAT('CLNT-', LPAD(client_id, 4, '0')),
    client_name = '시드플로우 거래처2',
    client_brn = '223-45-67890',
    ceo_name = '거래처대표2',
    company_phone = '02-2234-5678',
    address = '서울특별시 강동구 올림픽로 620',
    latitude = NULL,
    longitude = NULL,
    client_type = 'DISTRIBUTOR',
    manager_name = '거래처담당자2',
    manager_phone = '010-0000-0004',
    manager_email = 'client2@seedflow.com',
    employee_id = @sales_employee_id,
    total_credit = 50000000.00,
    used_credit = 0.00,
    updated_at = NOW()
WHERE client_id = @client2_id;

INSERT INTO tbl_user (
    login_id,
    login_pw,
    status,
    role,
    employee_id,
    client_id,
    last_login_at,
    created_at,
    updated_at
)
SELECT
    'admin@seedflow.com',
    @bcrypt_pw,
    'ACTIVATE',
    'ADMIN',
    @admin_employee_id,
    NULL,
    NULL,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_user
    WHERE login_id = 'admin@seedflow.com'
);

UPDATE tbl_user
SET login_pw = @bcrypt_pw,
    status = 'ACTIVATE',
    role = 'ADMIN',
    employee_id = @admin_employee_id,
    client_id = NULL,
    updated_at = NOW()
WHERE login_id = 'admin@seedflow.com';

INSERT INTO tbl_user (
    login_id,
    login_pw,
    status,
    role,
    employee_id,
    client_id,
    last_login_at,
    created_at,
    updated_at
)
SELECT
    'sales@seedflow.com',
    @bcrypt_pw,
    'ACTIVATE',
    'SALES_REP',
    @sales_employee_id,
    NULL,
    NULL,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_user
    WHERE login_id = 'sales@seedflow.com'
);

UPDATE tbl_user
SET login_pw = @bcrypt_pw,
    status = 'ACTIVATE',
    role = 'SALES_REP',
    employee_id = @sales_employee_id,
    client_id = NULL,
    updated_at = NOW()
WHERE login_id = 'sales@seedflow.com';

INSERT INTO tbl_user (
    login_id,
    login_pw,
    status,
    role,
    employee_id,
    client_id,
    last_login_at,
    created_at,
    updated_at
)
SELECT
    'client@seedflow.com',
    @bcrypt_pw,
    'ACTIVATE',
    'CLIENT',
    NULL,
    @client_id,
    NULL,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_user
    WHERE login_id = 'client@seedflow.com'
);

UPDATE tbl_user
SET login_pw = @bcrypt_pw,
    status = 'ACTIVATE',
    role = 'CLIENT',
    employee_id = NULL,
    client_id = @client_id,
    updated_at = NOW()
WHERE login_id = 'client@seedflow.com';

INSERT INTO tbl_user (
    login_id,
    login_pw,
    status,
    role,
    employee_id,
    client_id,
    last_login_at,
    created_at,
    updated_at
)
SELECT
    'client2@seedflow.com',
    @bcrypt_pw,
    'ACTIVATE',
    'CLIENT',
    NULL,
    @client2_id,
    NULL,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_user
    WHERE login_id = 'client2@seedflow.com'
);

UPDATE tbl_user
SET login_pw = @bcrypt_pw,
    status = 'ACTIVATE',
    role = 'CLIENT',
    employee_id = NULL,
    client_id = @client2_id,
    updated_at = NOW()
WHERE login_id = 'client2@seedflow.com';

COMMIT;
