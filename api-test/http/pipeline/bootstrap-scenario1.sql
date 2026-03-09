-- SeedFlow+ scenario1 재실행용 초기화 + 계정/거래처 seed 스크립트
-- 전제: MariaDB / MySQL
-- 주의: 개발 DB 데이터를 삭제한 뒤 scenario1에 필요한 계정/거래처를 다시 만든다.

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE tbl_notification_delivery;
TRUNCATE TABLE tbl_notification;

TRUNCATE TABLE tbl_approval_decision;
TRUNCATE TABLE tbl_approval_step;
TRUNCATE TABLE tbl_approval_request;

TRUNCATE TABLE tbl_sales_deal_log_detail;
TRUNCATE TABLE tbl_sales_deal_log;
TRUNCATE TABLE tbl_deal_sked;
TRUNCATE TABLE tbl_pers_sked;

TRUNCATE TABLE tbl_invoice_statement;
TRUNCATE TABLE tbl_payment;
TRUNCATE TABLE tbl_invoice;
TRUNCATE TABLE tbl_statement;

TRUNCATE TABLE tbl_order_detail;
TRUNCATE TABLE tbl_order_header;

TRUNCATE TABLE tbl_contract_detail;
TRUNCATE TABLE tbl_contract_header;

TRUNCATE TABLE tbl_quotation_detail;
TRUNCATE TABLE tbl_quotation_header;

TRUNCATE TABLE tbl_request_quotation_detail;
TRUNCATE TABLE tbl_request_quotation_header;

TRUNCATE TABLE tbl_sales_deal;

TRUNCATE TABLE tbl_account_score;
TRUNCATE TABLE tbl_client_crops;

TRUNCATE TABLE tbl_product_bookmark;
TRUNCATE TABLE tbl_product_compare_item;
TRUNCATE TABLE tbl_product_compare;
TRUNCATE TABLE tbl_product_feedback;
TRUNCATE TABLE tbl_product_price_history;
TRUNCATE TABLE tbl_product_tag;
TRUNCATE TABLE tbl_cultivation_time;
TRUNCATE TABLE tbl_tag;
TRUNCATE TABLE tbl_product;

TRUNCATE TABLE tbl_user;
TRUNCATE TABLE tbl_client;
TRUNCATE TABLE tbl_employee;

SET FOREIGN_KEY_CHECKS = 1;

START TRANSACTION;

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
) VALUES (
    'TEMP-ADMIN',
    '관리자',
    'admin@seedflow.com',
    '010-0000-0001',
    '서울특별시 강남구 테헤란로 1',
    NOW(),
    NOW()
);

SET @admin_employee_id = LAST_INSERT_ID();

UPDATE tbl_employee
SET employee_code = CONCAT('EMP-', LPAD(@admin_employee_id, 4, '0'))
WHERE employee_id = @admin_employee_id;


INSERT INTO tbl_employee (
    employee_code,
    employee_name,
    employee_email,
    employee_phone,
    address,
    created_at,
    updated_at
) VALUES (
    'TEMP-SALES',
    '영업담당자',
    'sales@seedflow.com',
    '010-0000-0002',
    '서울특별시 강남구 테헤란로 2',
    NOW(),
    NOW()
);

SET @sales_employee_id = LAST_INSERT_ID();

UPDATE tbl_employee
SET employee_code = CONCAT('EMP-', LPAD(@sales_employee_id, 4, '0'))
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
) VALUES (
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
);

SET @client_id = LAST_INSERT_ID();

UPDATE tbl_client
SET client_code = CONCAT('CLNT-', LPAD(@client_id, 4, '0'))
WHERE client_id = @client_id;


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
) VALUES (
    'admin@seedflow.com',
    @bcrypt_pw,
    'ACTIVATE',
    'ADMIN',
    @admin_employee_id,
    NULL,
    NULL,
    NOW(),
    NOW()
);

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
) VALUES (
    'sales@seedflow.com',
    @bcrypt_pw,
    'ACTIVATE',
    'SALES_REP',
    @sales_employee_id,
    NULL,
    NULL,
    NOW(),
    NOW()
);

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
) VALUES (
    'client@seedflow.com',
    @bcrypt_pw,
    'ACTIVATE',
    'CLIENT',
    NULL,
    @client_id,
    NULL,
    NOW(),
    NOW()
);

COMMIT;
