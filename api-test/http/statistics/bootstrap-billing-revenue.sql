-- Billing revenue statistics 전용 bootstrap
-- 목적:
--   1) scenario1 [0단계]와 동일한 admin/sales/client 기본 계정을 보장한다.
--   2) 통계 집계에 필요한 FK 체인
--      client -> sales_deal -> contract_header/detail -> order_header/detail
--      -> statement -> invoice -> invoice_statement
--      을 고정 코드로 재생성한다.
--   3) 재실행 시 기존 scenario/pipeline 데이터가 통계에 섞이지 않도록
--      매출/딜 파이프라인 테이블을 먼저 전체 초기화한다.

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

DELETE FROM tbl_product_bookmark
WHERE product_id IN (
    SELECT product_id
    FROM tbl_product
    WHERE product_code IN ('WM-STAT-260310', 'ML-STAT-260310')
);

DELETE FROM tbl_product_compare_item
WHERE product_id IN (
    SELECT product_id
    FROM tbl_product
    WHERE product_code IN ('WM-STAT-260310', 'ML-STAT-260310')
);

DELETE FROM tbl_product_feedback
WHERE product_id IN (
    SELECT product_id
    FROM tbl_product
    WHERE product_code IN ('WM-STAT-260310', 'ML-STAT-260310')
);

DELETE FROM tbl_product_price_history
WHERE product_id IN (
    SELECT product_id
    FROM tbl_product
    WHERE product_code IN ('WM-STAT-260310', 'ML-STAT-260310')
);

DELETE FROM tbl_product_tag
WHERE product_id IN (
    SELECT product_id
    FROM tbl_product
    WHERE product_code IN ('WM-STAT-260310', 'ML-STAT-260310')
);

DELETE FROM tbl_cultivation_time
WHERE product_id IN (
    SELECT product_id
    FROM tbl_product
    WHERE product_code IN ('WM-STAT-260310', 'ML-STAT-260310')
);

DELETE FROM tbl_product
WHERE product_code IN ('WM-STAT-260310', 'ML-STAT-260310');

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

INSERT INTO tbl_product (
    product_code,
    product_name,
    product_category,
    product_description,
    product_image_url,
    amount,
    unit,
    price,
    status,
    is_deleted,
    tags,
    created_at,
    updated_at
)
SELECT
    'WM-STAT-260310',
    '프리미엄 수박 5kg',
    'WATERMELON',
    '통계 더미데이터용 수박 상품',
    NULL,
    100,
    'BOX',
    35000.00,
    'SALE',
    0,
    '{}',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_product
    WHERE product_code = 'WM-STAT-260310'
);

UPDATE tbl_product
SET product_name = '프리미엄 수박 5kg',
    product_category = 'WATERMELON',
    product_description = '통계 더미데이터용 수박 상품',
    product_image_url = NULL,
    amount = 100,
    unit = 'BOX',
    price = 35000.00,
    status = 'SALE',
    is_deleted = 0,
    tags = '{}',
    updated_at = NOW()
WHERE product_code = 'WM-STAT-260310';

INSERT INTO tbl_product (
    product_code,
    product_name,
    product_category,
    product_description,
    product_image_url,
    amount,
    unit,
    price,
    status,
    is_deleted,
    tags,
    created_at,
    updated_at
)
SELECT
    'ML-STAT-260310',
    '고당도 참외 3kg',
    'MELON',
    '통계 더미데이터용 참외 상품',
    NULL,
    100,
    'BOX',
    22000.00,
    'SALE',
    0,
    '{}',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_product
    WHERE product_code = 'ML-STAT-260310'
);

UPDATE tbl_product
SET product_name = '고당도 참외 3kg',
    product_category = 'MELON',
    product_description = '통계 더미데이터용 참외 상품',
    product_image_url = NULL,
    amount = 100,
    unit = 'BOX',
    price = 22000.00,
    status = 'SALE',
    is_deleted = 0,
    tags = '{}',
    updated_at = NOW()
WHERE product_code = 'ML-STAT-260310';

SET @watermelon_product_id := (
    SELECT product_id
    FROM tbl_product
    WHERE product_code = 'WM-STAT-260310'
    LIMIT 1
);

SET @melon_product_id := (
    SELECT product_id
    FROM tbl_product
    WHERE product_code = 'ML-STAT-260310'
    LIMIT 1
);

INSERT INTO tbl_sales_deal (
    client_id,
    owner_emp_id,
    current_stage,
    current_status,
    latest_doc_type,
    latest_ref_id,
    latest_target_code,
    last_activity_at,
    closed_at,
    summary_memo,
    created_at,
    updated_at
)
SELECT
    @client_id,
    @sales_employee_id,
    'ISSUED',
    'PUBLISHED',
    'INV',
    0,
    'STAT260310-IV01',
    '2026-01-31 10:00:00',
    NULL,
    'STAT-20260310: monthly mixed included deal',
    '2026-01-01 09:00:00',
    '2026-01-31 10:00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_sales_deal
    WHERE summary_memo = 'STAT-20260310: monthly mixed included deal'
);

INSERT INTO tbl_sales_deal (
    client_id,
    owner_emp_id,
    current_stage,
    current_status,
    latest_doc_type,
    latest_ref_id,
    latest_target_code,
    last_activity_at,
    closed_at,
    summary_memo,
    created_at,
    updated_at
)
SELECT
    @client_id,
    @sales_employee_id,
    'PAID',
    'PAID',
    'INV',
    0,
    'STAT260310-IV02',
    '2026-02-28 10:00:00',
    NULL,
    'STAT-20260310: monthly watermelon paid deal',
    '2026-02-01 09:00:00',
    '2026-02-28 10:00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_sales_deal
    WHERE summary_memo = 'STAT-20260310: monthly watermelon paid deal'
);

INSERT INTO tbl_sales_deal (
    client_id,
    owner_emp_id,
    current_stage,
    current_status,
    latest_doc_type,
    latest_ref_id,
    latest_target_code,
    last_activity_at,
    closed_at,
    summary_memo,
    created_at,
    updated_at
)
SELECT
    @client_id,
    @sales_employee_id,
    'ISSUED',
    'PUBLISHED',
    'INV',
    0,
    'STAT260310-IV03',
    '2026-03-31 10:00:00',
    NULL,
    'STAT-20260310: excluded invoice statement deal',
    '2026-03-01 09:00:00',
    '2026-03-31 10:00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_sales_deal
    WHERE summary_memo = 'STAT-20260310: excluded invoice statement deal'
);

SET @deal_id_01 := (
    SELECT deal_id
    FROM tbl_sales_deal
    WHERE summary_memo = 'STAT-20260310: monthly mixed included deal'
    LIMIT 1
);

SET @deal_id_02 := (
    SELECT deal_id
    FROM tbl_sales_deal
    WHERE summary_memo = 'STAT-20260310: monthly watermelon paid deal'
    LIMIT 1
);

SET @deal_id_03 := (
    SELECT deal_id
    FROM tbl_sales_deal
    WHERE summary_memo = 'STAT-20260310: excluded invoice statement deal'
    LIMIT 1
);

INSERT INTO tbl_contract_header (
    contract_code,
    quo_id,
    client_id,
    deal_id,
    author_id,
    status,
    total_amount,
    start_date,
    end_date,
    billing_cycle,
    special_terms,
    memo,
    issue_date,
    updated_at
)
SELECT
    'STAT260310-CN01',
    NULL,
    @client_id,
    @deal_id_01,
    @sales_employee_id,
    'ACTIVE_CONTRACT',
    460000.00,
    '2026-01-01',
    '2026-12-31',
    'MONTHLY',
    '통계 테스트 월 정산',
    '통계 1월 포함 계약',
    '2026-01-01 09:10:00',
    '2026-01-31 10:00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_contract_header
    WHERE contract_code = 'STAT260310-CN01'
);

INSERT INTO tbl_contract_header (
    contract_code,
    quo_id,
    client_id,
    deal_id,
    author_id,
    status,
    total_amount,
    start_date,
    end_date,
    billing_cycle,
    special_terms,
    memo,
    issue_date,
    updated_at
)
SELECT
    'STAT260310-CN02',
    NULL,
    @client_id,
    @deal_id_02,
    @sales_employee_id,
    'ACTIVE_CONTRACT',
    180000.00,
    '2026-02-01',
    '2026-12-31',
    'MONTHLY',
    '통계 테스트 월 정산',
    '통계 2월 포함 계약',
    '2026-02-01 09:10:00',
    '2026-02-28 10:00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_contract_header
    WHERE contract_code = 'STAT260310-CN02'
);

INSERT INTO tbl_contract_header (
    contract_code,
    quo_id,
    client_id,
    deal_id,
    author_id,
    status,
    total_amount,
    start_date,
    end_date,
    billing_cycle,
    special_terms,
    memo,
    issue_date,
    updated_at
)
SELECT
    'STAT260310-CN03',
    NULL,
    @client_id,
    @deal_id_03,
    @sales_employee_id,
    'ACTIVE_CONTRACT',
    147000.00,
    '2026-03-01',
    '2026-12-31',
    'MONTHLY',
    '통계 테스트 월 정산',
    '통계 제외 계약',
    '2026-03-01 09:10:00',
    '2026-03-31 10:00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_contract_header
    WHERE contract_code = 'STAT260310-CN03'
);

SET @cnt_id_01 := (
    SELECT cnt_id
    FROM tbl_contract_header
    WHERE contract_code = 'STAT260310-CN01'
    LIMIT 1
);

SET @cnt_id_02 := (
    SELECT cnt_id
    FROM tbl_contract_header
    WHERE contract_code = 'STAT260310-CN02'
    LIMIT 1
);

SET @cnt_id_03 := (
    SELECT cnt_id
    FROM tbl_contract_header
    WHERE contract_code = 'STAT260310-CN03'
    LIMIT 1
);

INSERT INTO tbl_contract_detail (
    cnt_id,
    product_id,
    product_category,
    product_name,
    total_quantity,
    unit,
    unit_price,
    amount
)
SELECT
    @cnt_id_01,
    @watermelon_product_id,
    '수박',
    '프리미엄 수박 5kg',
    10,
    'BOX',
    35000.00,
    350000.00
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_contract_detail
    WHERE cnt_id = @cnt_id_01
      AND product_name = '프리미엄 수박 5kg'
);

INSERT INTO tbl_contract_detail (
    cnt_id,
    product_id,
    product_category,
    product_name,
    total_quantity,
    unit,
    unit_price,
    amount
)
SELECT
    @cnt_id_01,
    @melon_product_id,
    '참외',
    '고당도 참외 3kg',
    5,
    'BOX',
    22000.00,
    110000.00
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_contract_detail
    WHERE cnt_id = @cnt_id_01
      AND product_name = '고당도 참외 3kg'
);

INSERT INTO tbl_contract_detail (
    cnt_id,
    product_id,
    product_category,
    product_name,
    total_quantity,
    unit,
    unit_price,
    amount
)
SELECT
    @cnt_id_02,
    @watermelon_product_id,
    '수박',
    '프리미엄 수박 5kg',
    6,
    'BOX',
    30000.00,
    180000.00
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_contract_detail
    WHERE cnt_id = @cnt_id_02
      AND product_name = '프리미엄 수박 5kg'
);

INSERT INTO tbl_contract_detail (
    cnt_id,
    product_id,
    product_category,
    product_name,
    total_quantity,
    unit,
    unit_price,
    amount
)
SELECT
    @cnt_id_03,
    @melon_product_id,
    '참외',
    '고당도 참외 3kg',
    7,
    'BOX',
    21000.00,
    147000.00
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_contract_detail
    WHERE cnt_id = @cnt_id_03
      AND product_name = '고당도 참외 3kg'
);

SET @cnt_detail_01_wm := (
    SELECT cnt_detail_id
    FROM tbl_contract_detail
    WHERE cnt_id = @cnt_id_01
      AND product_name = '프리미엄 수박 5kg'
    LIMIT 1
);

SET @cnt_detail_01_ml := (
    SELECT cnt_detail_id
    FROM tbl_contract_detail
    WHERE cnt_id = @cnt_id_01
      AND product_name = '고당도 참외 3kg'
    LIMIT 1
);

SET @cnt_detail_02_wm := (
    SELECT cnt_detail_id
    FROM tbl_contract_detail
    WHERE cnt_id = @cnt_id_02
      AND product_name = '프리미엄 수박 5kg'
    LIMIT 1
);

SET @cnt_detail_03_ml := (
    SELECT cnt_detail_id
    FROM tbl_contract_detail
    WHERE cnt_id = @cnt_id_03
      AND product_name = '고당도 참외 3kg'
    LIMIT 1
);

INSERT INTO tbl_order_header (
    order_code,
    contract_id,
    client_id,
    deal_id,
    employee_id,
    total_amount,
    status,
    created_at
)
SELECT
    'STAT260310-OD01',
    @cnt_id_01,
    @client_id,
    @deal_id_01,
    @sales_employee_id,
    460000.00,
    'CONFIRMED',
    '2026-01-10 10:00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_order_header
    WHERE order_code = 'STAT260310-OD01'
);

INSERT INTO tbl_order_header (
    order_code,
    contract_id,
    client_id,
    deal_id,
    employee_id,
    total_amount,
    status,
    created_at
)
SELECT
    'STAT260310-OD02',
    @cnt_id_02,
    @client_id,
    @deal_id_02,
    @sales_employee_id,
    180000.00,
    'CONFIRMED',
    '2026-02-10 10:00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_order_header
    WHERE order_code = 'STAT260310-OD02'
);

INSERT INTO tbl_order_header (
    order_code,
    contract_id,
    client_id,
    deal_id,
    employee_id,
    total_amount,
    status,
    created_at
)
SELECT
    'STAT260310-OD03',
    @cnt_id_03,
    @client_id,
    @deal_id_03,
    @sales_employee_id,
    147000.00,
    'CONFIRMED',
    '2026-03-10 10:00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_order_header
    WHERE order_code = 'STAT260310-OD03'
);

SET @order_id_01 := (
    SELECT order_id
    FROM tbl_order_header
    WHERE order_code = 'STAT260310-OD01'
    LIMIT 1
);

SET @order_id_02 := (
    SELECT order_id
    FROM tbl_order_header
    WHERE order_code = 'STAT260310-OD02'
    LIMIT 1
);

SET @order_id_03 := (
    SELECT order_id
    FROM tbl_order_header
    WHERE order_code = 'STAT260310-OD03'
    LIMIT 1
);

INSERT INTO tbl_order_detail (
    order_id,
    contract_detail_id,
    quantity,
    shipping_name,
    shipping_phone,
    shipping_address,
    shipping_address_detail,
    delivery_request
)
SELECT
    @order_id_01,
    @cnt_detail_01_wm,
    10,
    '통계 수령인 1',
    '010-3333-0001',
    '서울시 송파구 통계로 1',
    '101호',
    '문 앞'
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_order_detail
    WHERE order_id = @order_id_01
      AND contract_detail_id = @cnt_detail_01_wm
);

INSERT INTO tbl_order_detail (
    order_id,
    contract_detail_id,
    quantity,
    shipping_name,
    shipping_phone,
    shipping_address,
    shipping_address_detail,
    delivery_request
)
SELECT
    @order_id_01,
    @cnt_detail_01_ml,
    5,
    '통계 수령인 1',
    '010-3333-0001',
    '서울시 송파구 통계로 1',
    '101호',
    '문 앞'
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_order_detail
    WHERE order_id = @order_id_01
      AND contract_detail_id = @cnt_detail_01_ml
);

INSERT INTO tbl_order_detail (
    order_id,
    contract_detail_id,
    quantity,
    shipping_name,
    shipping_phone,
    shipping_address,
    shipping_address_detail,
    delivery_request
)
SELECT
    @order_id_02,
    @cnt_detail_02_wm,
    6,
    '통계 수령인 2',
    '010-3333-0002',
    '서울시 송파구 통계로 2',
    '202호',
    '경비실'
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_order_detail
    WHERE order_id = @order_id_02
      AND contract_detail_id = @cnt_detail_02_wm
);

INSERT INTO tbl_order_detail (
    order_id,
    contract_detail_id,
    quantity,
    shipping_name,
    shipping_phone,
    shipping_address,
    shipping_address_detail,
    delivery_request
)
SELECT
    @order_id_03,
    @cnt_detail_03_ml,
    7,
    '통계 수령인 3',
    '010-3333-0003',
    '서울시 송파구 통계로 3',
    '303호',
    '직접 수령'
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_order_detail
    WHERE order_id = @order_id_03
      AND contract_detail_id = @cnt_detail_03_ml
);

INSERT INTO tbl_statement (
    statement_code,
    order_id,
    deal_id,
    supply_amount,
    vat_amount,
    total_amount,
    status,
    created_at
)
SELECT
    'STAT260310-ST01',
    @order_id_01,
    @deal_id_01,
    418181.82,
    41818.18,
    460000.00,
    'ISSUED',
    '2026-01-15 10:00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_statement
    WHERE statement_code = 'STAT260310-ST01'
);

INSERT INTO tbl_statement (
    statement_code,
    order_id,
    deal_id,
    supply_amount,
    vat_amount,
    total_amount,
    status,
    created_at
)
SELECT
    'STAT260310-ST02',
    @order_id_02,
    @deal_id_02,
    163636.36,
    16363.64,
    180000.00,
    'ISSUED',
    '2026-02-15 10:00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_statement
    WHERE statement_code = 'STAT260310-ST02'
);

INSERT INTO tbl_statement (
    statement_code,
    order_id,
    deal_id,
    supply_amount,
    vat_amount,
    total_amount,
    status,
    created_at
)
SELECT
    'STAT260310-ST03',
    @order_id_03,
    @deal_id_03,
    133636.36,
    13363.64,
    147000.00,
    'ISSUED',
    '2026-03-15 10:00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_statement
    WHERE statement_code = 'STAT260310-ST03'
);

SET @statement_id_01 := (
    SELECT statement_id
    FROM tbl_statement
    WHERE statement_code = 'STAT260310-ST01'
    LIMIT 1
);

SET @statement_id_02 := (
    SELECT statement_id
    FROM tbl_statement
    WHERE statement_code = 'STAT260310-ST02'
    LIMIT 1
);

SET @statement_id_03 := (
    SELECT statement_id
    FROM tbl_statement
    WHERE statement_code = 'STAT260310-ST03'
    LIMIT 1
);

INSERT INTO tbl_invoice (
    invoice_code,
    contract_id,
    client_id,
    deal_id,
    employee_id,
    invoice_date,
    start_date,
    end_date,
    supply_amount,
    vat_amount,
    total_amount,
    status,
    memo,
    created_at
)
SELECT
    'STAT260310-IV01',
    @cnt_id_01,
    @client_id,
    @deal_id_01,
    @sales_employee_id,
    '2026-01-31',
    '2026-01-01',
    '2026-01-31',
    418181.82,
    41818.18,
    460000.00,
    'PUBLISHED',
    '통계 1월 포함 청구서',
    '2026-01-31 10:00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_invoice
    WHERE invoice_code = 'STAT260310-IV01'
);

INSERT INTO tbl_invoice (
    invoice_code,
    contract_id,
    client_id,
    deal_id,
    employee_id,
    invoice_date,
    start_date,
    end_date,
    supply_amount,
    vat_amount,
    total_amount,
    status,
    memo,
    created_at
)
SELECT
    'STAT260310-IV02',
    @cnt_id_02,
    @client_id,
    @deal_id_02,
    @sales_employee_id,
    '2026-02-28',
    '2026-02-01',
    '2026-02-28',
    163636.36,
    16363.64,
    180000.00,
    'PAID',
    '통계 2월 포함 청구서',
    '2026-02-28 10:00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_invoice
    WHERE invoice_code = 'STAT260310-IV02'
);

INSERT INTO tbl_invoice (
    invoice_code,
    contract_id,
    client_id,
    deal_id,
    employee_id,
    invoice_date,
    start_date,
    end_date,
    supply_amount,
    vat_amount,
    total_amount,
    status,
    memo,
    created_at
)
SELECT
    'STAT260310-IV03',
    @cnt_id_03,
    @client_id,
    @deal_id_03,
    @sales_employee_id,
    '2026-03-31',
    '2026-03-01',
    '2026-03-31',
    133636.36,
    13363.64,
    147000.00,
    'PUBLISHED',
    '통계 제외 청구서',
    '2026-03-31 10:00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_invoice
    WHERE invoice_code = 'STAT260310-IV03'
);

SET @invoice_id_01 := (
    SELECT invoice_id
    FROM tbl_invoice
    WHERE invoice_code = 'STAT260310-IV01'
    LIMIT 1
);

SET @invoice_id_02 := (
    SELECT invoice_id
    FROM tbl_invoice
    WHERE invoice_code = 'STAT260310-IV02'
    LIMIT 1
);

SET @invoice_id_03 := (
    SELECT invoice_id
    FROM tbl_invoice
    WHERE invoice_code = 'STAT260310-IV03'
    LIMIT 1
);

INSERT INTO tbl_invoice_statement (
    invoice_id,
    statement_id,
    is_included
)
SELECT
    @invoice_id_01,
    @statement_id_01,
    1
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_invoice_statement
    WHERE invoice_id = @invoice_id_01
      AND statement_id = @statement_id_01
);

INSERT INTO tbl_invoice_statement (
    invoice_id,
    statement_id,
    is_included
)
SELECT
    @invoice_id_02,
    @statement_id_02,
    1
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_invoice_statement
    WHERE invoice_id = @invoice_id_02
      AND statement_id = @statement_id_02
);

INSERT INTO tbl_invoice_statement (
    invoice_id,
    statement_id,
    is_included
)
SELECT
    @invoice_id_03,
    @statement_id_03,
    0
WHERE NOT EXISTS (
    SELECT 1
    FROM tbl_invoice_statement
    WHERE invoice_id = @invoice_id_03
      AND statement_id = @statement_id_03
);

UPDATE tbl_sales_deal
SET current_stage = 'ISSUED',
    current_status = 'PUBLISHED',
    latest_doc_type = 'INV',
    latest_ref_id = @invoice_id_01,
    latest_target_code = 'STAT260310-IV01',
    last_activity_at = '2026-01-31 10:00:00',
    updated_at = '2026-01-31 10:00:00'
WHERE deal_id = @deal_id_01;

UPDATE tbl_sales_deal
SET current_stage = 'PAID',
    current_status = 'PAID',
    latest_doc_type = 'INV',
    latest_ref_id = @invoice_id_02,
    latest_target_code = 'STAT260310-IV02',
    last_activity_at = '2026-02-28 10:00:00',
    updated_at = '2026-02-28 10:00:00'
WHERE deal_id = @deal_id_02;

UPDATE tbl_sales_deal
SET current_stage = 'ISSUED',
    current_status = 'PUBLISHED',
    latest_doc_type = 'INV',
    latest_ref_id = @invoice_id_03,
    latest_target_code = 'STAT260310-IV03',
    last_activity_at = '2026-03-31 10:00:00',
    updated_at = '2026-03-31 10:00:00'
WHERE deal_id = @deal_id_03;

COMMIT;

SELECT invoice_code, invoice_date, status, total_amount
FROM tbl_invoice
WHERE invoice_code IN ('STAT260310-IV01', 'STAT260310-IV02', 'STAT260310-IV03')
ORDER BY invoice_code;

SELECT contract_detail.product_category,
       SUM(contract_detail.unit_price * order_detail.quantity) AS billed_amount
FROM tbl_invoice invoice
JOIN tbl_invoice_statement invoice_statement
  ON invoice_statement.invoice_id = invoice.invoice_id
JOIN tbl_statement statement
  ON statement.statement_id = invoice_statement.statement_id
JOIN tbl_order_detail order_detail
  ON order_detail.order_id = statement.order_id
JOIN tbl_contract_detail contract_detail
  ON contract_detail.cnt_detail_id = order_detail.contract_detail_id
WHERE invoice.invoice_code IN ('STAT260310-IV01', 'STAT260310-IV02', 'STAT260310-IV03')
  AND invoice.status IN ('PUBLISHED', 'PAID')
  AND invoice_statement.is_included = 1
  AND statement.status = 'ISSUED'
GROUP BY contract_detail.product_category
ORDER BY billed_amount DESC;
