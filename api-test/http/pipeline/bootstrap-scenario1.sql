-- SeedFlow+ scenario1 재실행용 초기화 + 계정/거래처 seed 스크립트
-- 전제: MariaDB / MySQL
-- 주의: 개발 DB 데이터를 삭제한 뒤 scenario1에 필요한 계정/거래처를 다시 만든다.
-- 비어 있는 테이블에서도 멈추지 않도록 TRUNCATE 대신 DELETE + AUTO_INCREMENT 초기화를 사용한다.

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

DELETE FROM tbl_account_score;
DELETE FROM tbl_client_crops;

DELETE FROM tbl_product_bookmark;
DELETE FROM tbl_product_compare_item;
DELETE FROM tbl_product_compare;
DELETE FROM tbl_product_feedback;
DELETE FROM tbl_product_price_history;
DELETE FROM tbl_product_tag;
DELETE FROM tbl_cultivation_time;
DELETE FROM tbl_tag;
DELETE FROM tbl_product;

DELETE FROM tbl_user;
DELETE FROM tbl_client;
DELETE FROM tbl_employee;

ALTER TABLE tbl_notification_delivery AUTO_INCREMENT = 1;
ALTER TABLE tbl_notification AUTO_INCREMENT = 1;
ALTER TABLE tbl_approval_decision AUTO_INCREMENT = 1;
ALTER TABLE tbl_approval_step AUTO_INCREMENT = 1;
ALTER TABLE tbl_approval_request AUTO_INCREMENT = 1;
ALTER TABLE tbl_sales_deal_log_detail AUTO_INCREMENT = 1;
ALTER TABLE tbl_sales_deal_log AUTO_INCREMENT = 1;
ALTER TABLE tbl_deal_sked AUTO_INCREMENT = 1;
ALTER TABLE tbl_pers_sked AUTO_INCREMENT = 1;
ALTER TABLE tbl_invoice_statement AUTO_INCREMENT = 1;
ALTER TABLE tbl_payment AUTO_INCREMENT = 1;
ALTER TABLE tbl_invoice AUTO_INCREMENT = 1;
ALTER TABLE tbl_statement AUTO_INCREMENT = 1;
ALTER TABLE tbl_order_detail AUTO_INCREMENT = 1;
ALTER TABLE tbl_order_header AUTO_INCREMENT = 1;
ALTER TABLE tbl_contract_detail AUTO_INCREMENT = 1;
ALTER TABLE tbl_contract_header AUTO_INCREMENT = 1;
ALTER TABLE tbl_quotation_detail AUTO_INCREMENT = 1;
ALTER TABLE tbl_quotation_header AUTO_INCREMENT = 1;
ALTER TABLE tbl_request_quotation_detail AUTO_INCREMENT = 1;
ALTER TABLE tbl_request_quotation_header AUTO_INCREMENT = 1;
ALTER TABLE tbl_sales_deal AUTO_INCREMENT = 1;
ALTER TABLE tbl_account_score AUTO_INCREMENT = 1;
ALTER TABLE tbl_client_crops AUTO_INCREMENT = 1;
ALTER TABLE tbl_product_bookmark AUTO_INCREMENT = 1;
ALTER TABLE tbl_product_compare_item AUTO_INCREMENT = 1;
ALTER TABLE tbl_product_compare AUTO_INCREMENT = 1;
ALTER TABLE tbl_product_feedback AUTO_INCREMENT = 1;
ALTER TABLE tbl_product_price_history AUTO_INCREMENT = 1;
ALTER TABLE tbl_product_tag AUTO_INCREMENT = 1;
ALTER TABLE tbl_cultivation_time AUTO_INCREMENT = 1;
ALTER TABLE tbl_tag AUTO_INCREMENT = 1;
ALTER TABLE tbl_product AUTO_INCREMENT = 1;
ALTER TABLE tbl_user AUTO_INCREMENT = 1;
ALTER TABLE tbl_client AUTO_INCREMENT = 1;
ALTER TABLE tbl_employee AUTO_INCREMENT = 1;

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
