-- 모든 문서 조회 페이지 프론트 API 연결 테스트용 데이터
-- 대상 API:
--   POST /api/v1/auth/login
--   GET  /api/v1/documents
--   GET  /api/v1/deals
--
-- 이 SQL은 기존 운영/개발 데이터와 충돌을 줄이기 위해
-- 전용 네임스페이스(SC1-20260310) 코드와 높은 고정 ID를 사용한다.
--
-- 테스트 로그인 계정
--   ADMIN     : sc1_admin_20260310  / password123
--   SALES_REP : sc1_sales_20260310  / 12345678
--   CLIENT    : sc1_client_20260310 / password123
--
-- 비밀번호 해시:
--   password123 -> $2y$10$hYJlUfYhgMa299g3.fVa4O/7XHLddvydZcN.izd2xjl.zkR.hGQnq
--   12345678   -> $2y$10$.XmgbrbX8p.X49iKsThg2OBkRFYlAt27IffsG09cljCDx8bpkCc9y
--
-- 포함 DealType 점검 결과
--   RFQ / QUO / CNT / ORD / STMT / INV / PAY 전부 포함
--   PAY는 "입금확인서"가 아니라 billing/payment 도메인의 결제 문서 타입이다.

CREATE OR REPLACE VIEW v_document_summary AS
SELECT CONCAT('RFQ-', rfq_id) AS surrogate_id,
       'RFQ' AS doc_type, rfq_id AS doc_id, deal_id, client_id,
       request_code AS doc_code, NULL AS amount, NULL AS expired_date,
       CONCAT('', request_status) AS status, created_at
FROM tbl_request_quotation_header
WHERE request_status != 'DELETED'
UNION ALL
SELECT CONCAT('QUO-', quo_id),
       'QUO', quo_id, deal_id, client_id,
       quotation_code, total_amount, expired_date,
       CONCAT('', status) AS status, created_at
FROM tbl_quotation_header
WHERE status != 'DELETED'
UNION ALL
SELECT CONCAT('CNT-', cnt_id),
       'CNT', cnt_id, deal_id, client_id,
       contract_code, total_amount, end_date,
       CONCAT('', status) AS status, issue_date
FROM tbl_contract_header
WHERE status != 'DELETED'
UNION ALL
SELECT CONCAT('ORD-', o.order_id),
       'ORD', o.order_id, o.deal_id, d.client_id,
       o.order_code, o.total_amount, NULL,
       CONCAT('', o.status) AS status, o.created_at
FROM tbl_order_header o
JOIN tbl_sales_deal d ON d.deal_id = o.deal_id
UNION ALL
SELECT CONCAT('STMT-', s.statement_id),
       'STMT', s.statement_id, s.deal_id, d.client_id,
       s.statement_code, s.total_amount, NULL,
       CONCAT('', s.status) AS status, s.created_at
FROM tbl_statement s
JOIN tbl_sales_deal d ON d.deal_id = s.deal_id
UNION ALL
SELECT CONCAT('INV-', i.invoice_id),
       'INV', i.invoice_id, i.deal_id, d.client_id,
       i.invoice_code, i.total_amount, i.end_date,
       CONCAT('', i.status) AS status, i.created_at
FROM tbl_invoice i
JOIN tbl_sales_deal d ON d.deal_id = i.deal_id
UNION ALL
SELECT CONCAT('PAY-', p.payment_id),
       'PAY', p.payment_id, p.deal_id, d.client_id,
       p.payment_code, p.payment_amount, NULL,
       CONCAT('', p.status) AS status, p.created_at
FROM tbl_payment p
JOIN tbl_sales_deal d ON d.deal_id = p.deal_id;

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM tbl_payment
WHERE payment_code IN ('SC1-20260310-PAY-OPEN', 'SC1-20260310-PAY-CLOSED');

DELETE FROM tbl_invoice
WHERE invoice_code IN ('SC1-20260310-INV-OPEN', 'SC1-20260310-INV-CLOSED');

DELETE FROM tbl_statement
WHERE statement_code IN ('SC1-20260310-STMT-OPEN');

DELETE FROM tbl_order_header
WHERE order_code IN ('SC1-20260310-ORD-OPEN', 'SC1-20260310-ORD-CLOSED');

DELETE FROM tbl_contract_header
WHERE contract_code IN ('SC1-20260310-CNT-OPEN', 'SC1-20260310-CNT-CLOSED');

DELETE FROM tbl_quotation_header
WHERE quotation_code IN ('SC1-20260310-QUO-OPEN', 'SC1-20260310-QUO-PENDING');

DELETE FROM tbl_request_quotation_header
WHERE request_code IN ('SC1-20260310-RFQ-OPEN', 'SC1-20260310-RFQ-PENDING', 'SC1-20260310-RFQ-CLOSED');

DELETE FROM tbl_sales_deal
WHERE latest_target_code IN (
    'SC1-20260310-PAY-OPEN',
    'SC1-20260310-QUO-PENDING',
    'SC1-20260310-ORD-CLOSED'
)
OR summary_memo LIKE 'SC1-20260310:%';

DELETE FROM tbl_user
WHERE login_id IN ('sc1_admin_20260310', 'sc1_sales_20260310', 'sc1_client_20260310');

DELETE FROM tbl_client
WHERE client_code IN ('SC1-20260310-CL-01', 'SC1-20260310-CL-02');

DELETE FROM tbl_employee
WHERE employee_code IN ('SC1-20260310-EMP-ADMIN', 'SC1-20260310-EMP-SALES-01', 'SC1-20260310-EMP-SALES-02');

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO tbl_employee (
    employee_id,
    employee_code,
    employee_name,
    employee_email,
    employee_phone,
    address,
    created_at,
    updated_at
) VALUES
    (910001, 'SC1-20260310-EMP-ADMIN', '시나리오 관리자', 'sc1.admin.20260310@seedflow.test', '010-9100-0001', '서울시 종로구 테스트로 1', '2026-03-10 09:00:00', '2026-03-10 09:00:00'),
    (910002, 'SC1-20260310-EMP-SALES-01', '시나리오 영업1', 'sc1.sales1.20260310@seedflow.test', '010-9100-0002', '서울시 강남구 테스트로 2', '2026-03-10 09:01:00', '2026-03-10 09:01:00'),
    (910003, 'SC1-20260310-EMP-SALES-02', '시나리오 영업2', 'sc1.sales2.20260310@seedflow.test', '010-9100-0003', '부산시 해운대구 테스트로 3', '2026-03-10 09:02:00', '2026-03-10 09:02:00');

INSERT INTO tbl_client (
    client_id,
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
) VALUES
    (920001, 'SC1-20260310-CL-01', '시나리오 거래처A', '9100012026031', '대표A', '02-9100-0001', '경기도 성남시 테스트로 101', 37.3940, 127.1112, 'DISTRIBUTOR', '담당A', '010-9200-0001', 'sc1.client1.20260310@seedflow.test', 910002, 15000000.00, 3500000.00, '2026-03-10 09:10:00', '2026-03-10 09:10:00'),
    (920002, 'SC1-20260310-CL-02', '시나리오 거래처B', '9100022026031', '대표B', '051-9100-0002', '부산시 사상구 테스트로 202', 35.1521, 128.9918, 'NURSERY', '담당B', '010-9200-0002', 'sc1.client2.20260310@seedflow.test', 910003, 9000000.00, 1000000.00, '2026-03-10 09:11:00', '2026-03-10 09:11:00');

INSERT INTO tbl_user (
    user_id,
    login_id,
    login_pw,
    status,
    role,
    employee_id,
    client_id,
    last_login_at,
    created_at,
    updated_at
) VALUES
    (990001, 'sc1_admin_20260310', '$2y$10$hYJlUfYhgMa299g3.fVa4O/7XHLddvydZcN.izd2xjl.zkR.hGQnq', 'ACTIVATE', 'ADMIN', 910001, NULL, '2026-03-10 10:00:00', '2026-03-10 10:00:00', '2026-03-10 10:00:00'),
    (990002, 'sc1_sales_20260310', '$2y$10$.XmgbrbX8p.X49iKsThg2OBkRFYlAt27IffsG09cljCDx8bpkCc9y', 'ACTIVATE', 'SALES_REP', 910002, NULL, '2026-03-10 10:01:00', '2026-03-10 10:01:00', '2026-03-10 10:01:00'),
    (990003, 'sc1_client_20260310', '$2y$10$hYJlUfYhgMa299g3.fVa4O/7XHLddvydZcN.izd2xjl.zkR.hGQnq', 'ACTIVATE', 'CLIENT', NULL, 920001, '2026-03-10 10:02:00', '2026-03-10 10:02:00', '2026-03-10 10:02:00');

INSERT INTO tbl_sales_deal (
    deal_id,
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
) VALUES
    (930001, 920001, 910002, 'PAID', 'COMPLETED', 'PAY', 970001, 'SC1-20260310-PAY-OPEN', '2026-03-10 16:00:00', NULL, 'SC1-20260310: open full-chain deal for all document list', '2026-03-10 11:00:00', '2026-03-10 16:00:00'),
    (930002, 920001, 910002, 'PENDING_ADMIN', 'WAITING_ADMIN', 'QUO', 950002, 'SC1-20260310-QUO-PENDING', '2026-03-10 14:00:00', NULL, 'SC1-20260310: open pending quotation deal for filter test', '2026-03-10 11:10:00', '2026-03-10 14:00:00'),
    (930003, 920002, 910003, 'CANCELED', 'CANCELED', 'ORD', 960002, 'SC1-20260310-ORD-CLOSED', '2026-03-10 15:00:00', '2026-03-10 15:10:00', 'SC1-20260310: closed foreign-scope order deal for access filter test', '2026-03-10 11:20:00', '2026-03-10 15:10:00');

INSERT INTO tbl_request_quotation_header (
    rfq_id,
    request_code,
    client_id,
    deal_id,
    requirements,
    request_status,
    created_at,
    updated_at
) VALUES
    (940001, 'SC1-20260310-RFQ-OPEN', 920001, 930001, '통합 문서 조회용 완료 체인 RFQ', 'COMPLETED', '2026-03-10 11:30:00', '2026-03-10 11:40:00'),
    (940002, 'SC1-20260310-RFQ-PENDING', 920001, 930002, '승인 대기 문서 조회용 RFQ', 'COMPLETED', '2026-03-10 11:35:00', '2026-03-10 11:45:00'),
    (940003, 'SC1-20260310-RFQ-CLOSED', 920002, 930003, '다른 거래처/다른 영업사원 범위 확인용 RFQ', 'REVIEWING', '2026-03-10 11:40:00', '2026-03-10 11:50:00');

INSERT INTO tbl_quotation_header (
    quo_id,
    rfq_id,
    quotation_code,
    client_id,
    deal_id,
    author_id,
    status,
    total_amount,
    expired_date,
    memo,
    created_at,
    updated_at
) VALUES
    (950001, 940001, 'SC1-20260310-QUO-OPEN', 920001, 930001, 910002, 'COMPLETED', 1650000.00, '2026-04-09', 'SC1-20260310 open quotation', '2026-03-10 12:00:00', '2026-03-10 12:05:00'),
    (950002, 940002, 'SC1-20260310-QUO-PENDING', 920001, 930002, 910002, 'WAITING_ADMIN', 770000.00, '2026-04-10', 'SC1-20260310 pending admin quotation', '2026-03-10 12:10:00', '2026-03-10 12:10:00');

INSERT INTO tbl_contract_header (
    cnt_id,
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
) VALUES
    (960001,  'SC1-20260310-CNT-OPEN',   950001, 920001, 930001, 910002, 'ACTIVE_CONTRACT', 1650000.00, '2026-03-10', '2026-12-31', 'MONTHLY',   '월 1회 납품', 'SC1-20260310 open contract',   '2026-03-10 12:30:00', '2026-03-10 12:30:00'),
    (9600021, 'SC1-20260310-CNT-CLOSED', NULL,   920002, 930003, 910003, 'COMPLETED',       350000.00,  '2026-03-10', '2026-06-30', 'QUARTERLY', '분기 납품',   'SC1-20260310 closed contract', '2026-03-10 12:40:00', '2026-03-10 12:40:00');

INSERT INTO tbl_order_header (
    order_id,
    order_code,
    contract_id,
    client_id,
    deal_id,
    employee_id,
    total_amount,
    status,
    created_at
) VALUES
    (960001, 'SC1-20260310-ORD-OPEN', 960001, 920001, 930001, 910002, 1650000.00, 'CONFIRMED', '2026-03-10 13:00:00'),
    (960002, 'SC1-20260310-ORD-CLOSED', 9600021, 920002, 930003, 910003, 350000.00, 'CANCELED', '2026-03-10 13:10:00');

INSERT INTO tbl_statement (
    statement_id,
    statement_code,
    order_id,
    deal_id,
    supply_amount,
    vat_amount,
    total_amount,
    status,
    created_at
) VALUES
    (965001, 'SC1-20260310-STMT-OPEN', 960001, 930001, 1500000.00, 150000.00, 1650000.00, 'ISSUED', '2026-03-10 13:20:00');

INSERT INTO tbl_invoice (
    invoice_id,
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
) VALUES
    (966001, 'SC1-20260310-INV-OPEN', 960001, 920001, 930001, 910002, '2026-03-10', '2026-03-01', '2026-03-31', 1500000.00, 150000.00, 1650000.00, 'PAID', 'SC1-20260310 open invoice', '2026-03-10 13:40:00'),
    (966002, 'SC1-20260310-INV-CLOSED', 9600021, 920002, 930003, 910003, '2026-03-10', '2026-03-01', '2026-03-31', 318181.82, 31818.18, 350000.00, 'CANCELED', 'SC1-20260310 closed invoice', '2026-03-10 13:50:00');

INSERT INTO tbl_payment (
    payment_id,
    payment_code,
    invoice_id,
    client_id,
    deal_id,
    payment_amount,
    payment_method,
    status,
    created_at
) VALUES
    (970001, 'SC1-20260310-PAY-OPEN', 966001, 920001, 930001, 1650000.00, 'TRANSFER', 'COMPLETED', '2026-03-10 16:00:00'),
    (970002, 'SC1-20260310-PAY-CLOSED', 966002, 920002, 930003, 350000.00, 'CASH', 'FAILED', '2026-03-10 14:00:00');

-- 확인용 샘플 쿼리 1: 계정/권한
SELECT user_id, login_id, role, employee_id, client_id
FROM tbl_user
WHERE login_id IN ('sc1_admin_20260310', 'sc1_sales_20260310', 'sc1_client_20260310')
ORDER BY user_id;

-- 확인용 샘플 쿼리 2: deal snapshot
SELECT deal_id, client_id, owner_emp_id, current_stage, current_status, latest_doc_type, latest_ref_id, latest_target_code, closed_at
FROM tbl_sales_deal
WHERE summary_memo LIKE 'SC1-20260310:%'
ORDER BY deal_id;

-- 확인용 샘플 쿼리 3: 모든 DealType 포함 여부
SELECT doc_type, COUNT(*) AS doc_count
FROM v_document_summary
WHERE doc_code LIKE 'SC1-20260310-%'
GROUP BY doc_type
ORDER BY doc_type;

-- 확인용 샘플 쿼리 4: 문서 목록 조회 확인
SELECT surrogate_id, doc_type, doc_code, amount, expired_date, status, created_at
FROM v_document_summary
WHERE doc_code LIKE 'SC1-20260310-%'
ORDER BY created_at DESC, surrogate_id DESC;
