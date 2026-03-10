-- 모든 문서 조회 페이지 프론트 API 연결 테스트용 데이터
-- 대상 API:
--   POST /api/v1/auth/login
--   GET  /api/v1/documents
--   GET  /api/v1/deals
--
-- 테스트 로그인 계정
--   ADMIN     : sales_admin / password123
--   SALES_REP : idid / 12345678
--   CLIENT    : client_id_01 / password123
--
-- 비밀번호 해시:
--   password123 -> $2y$10$hYJlUfYhgMa299g3.fVa4O/7XHLddvydZcN.izd2xjl.zkR.hGQnq
--   12345678   -> $2y$10$.XmgbrbX8p.X49iKsThg2OBkRFYlAt27IffsG09cljCDx8bpkCc9y

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
SELECT CONCAT('STMT-', statement_id),
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

DELETE FROM tbl_payment WHERE payment_id IN (4601);
DELETE FROM tbl_invoice WHERE invoice_id IN (4501);
DELETE FROM tbl_statement WHERE statement_id IN (4401);
DELETE FROM tbl_order_header WHERE order_id IN (4301, 4302);
DELETE FROM tbl_contract_header WHERE cnt_id IN (4201, 4202);
DELETE FROM tbl_quotation_header WHERE quo_id IN (4101, 4102);
DELETE FROM tbl_request_quotation_header WHERE rfq_id IN (4001, 4002, 4003);
DELETE FROM tbl_sales_deal WHERE deal_id IN (3001, 3002, 3003);
DELETE FROM tbl_user WHERE user_id IN (9001, 9002, 9003);
DELETE FROM tbl_client WHERE client_id IN (2001, 2002);
DELETE FROM tbl_employee WHERE employee_id IN (1001, 1002, 1003);

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
    (1001, 'EMP-ADMIN-01', '문서 관리자', 'admin@seedflow.test', '010-1000-1000', '서울시 중구 세종대로 1', '2026-03-01 09:00:00', '2026-03-01 09:00:00'),
    (1002, 'EMP-SALES-01', '김영업', 'sales1@seedflow.test', '010-2000-2000', '서울시 강남구 테헤란로 10', '2026-03-01 09:10:00', '2026-03-01 09:10:00'),
    (1003, 'EMP-SALES-02', '박영업', 'sales2@seedflow.test', '010-3000-3000', '부산시 해운대구 센텀중앙로 20', '2026-03-01 09:20:00', '2026-03-01 09:20:00');

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
    (2001, 'CL-TEST-01', '테스트농자재상회', '1101112222333', '홍대표', '02-111-2222', '경기도 성남시 분당구 판교로 100', 37.3940, 127.1112, 'DISTRIBUTOR', '이담당', '010-1111-2222', 'client1@seedflow.test', 1002, 10000000.00, 2100000.00, '2026-03-01 10:00:00', '2026-03-01 10:00:00'),
    (2002, 'CL-TEST-02', '남부원예유통', '2202223333444', '최대표', '051-333-4444', '부산시 사상구 광장로 55', 35.1521, 128.9918, 'NURSERY', '오담당', '010-3333-4444', 'client2@seedflow.test', 1003, 8000000.00, 1200000.00, '2026-03-01 10:10:00', '2026-03-01 10:10:00');

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
    (9001, 'sales_admin', '$2y$10$hYJlUfYhgMa299g3.fVa4O/7XHLddvydZcN.izd2xjl.zkR.hGQnq', 'ACTIVATE', 'ADMIN', 1001, NULL, '2026-03-10 08:50:00', '2026-03-01 11:00:00', '2026-03-10 08:50:00'),
    (9002, 'idid', '$2y$10$.XmgbrbX8p.X49iKsThg2OBkRFYlAt27IffsG09cljCDx8bpkCc9y', 'ACTIVATE', 'SALES_REP', 1002, NULL, '2026-03-10 08:55:00', '2026-03-01 11:10:00', '2026-03-10 08:55:00'),
    (9003, 'client_id_01', '$2y$10$hYJlUfYhgMa299g3.fVa4O/7XHLddvydZcN.izd2xjl.zkR.hGQnq', 'ACTIVATE', 'CLIENT', NULL, 2001, '2026-03-10 09:00:00', '2026-03-01 11:20:00', '2026-03-10 09:00:00');

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
    (3001, 2001, 1002, 'PAID', 'COMPLETED', 'PAY', 4601, 'PAY-260310-001', '2026-03-09 16:00:00', NULL, '완료된 전체 문서 체인 테스트용 딜', '2026-03-02 09:00:00', '2026-03-09 16:00:00'),
    (3002, 2001, 1002, 'PENDING_ADMIN', 'WAITING_ADMIN', 'QUO', 4102, 'QUO-260310-002', '2026-03-07 14:00:00', NULL, '관리자 승인 대기 문서 필터 테스트용 딜', '2026-03-03 10:00:00', '2026-03-07 14:00:00'),
    (3003, 2002, 1003, 'CANCELED', 'CANCELED', 'ORD', 4302, 'ORD-260310-002', '2026-03-05 17:00:00', '2026-03-05 17:10:00', '타 영업사원 및 종료 딜 필터 테스트용 딜', '2026-03-04 11:00:00', '2026-03-05 17:10:00');

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
    (4001, 'RFQ-260310-001', 2001, 3001, '수박 종자 3종 견적 요청', 'COMPLETED', '2026-03-02 09:30:00', '2026-03-02 10:00:00'),
    (4002, 'RFQ-260310-002', 2001, 3002, '고추 종자 긴급 견적 요청', 'COMPLETED', '2026-03-03 10:30:00', '2026-03-03 11:00:00'),
    (4003, 'RFQ-260310-003', 2002, 3003, '남부지역 딸기 모종 견적 요청', 'REVIEWING', '2026-03-04 11:30:00', '2026-03-04 12:00:00');

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
    (4101, 4001, 'QUO-260310-001', 2001, 3001, 1002, 'COMPLETED', 1650000.00, '2026-04-08', '완료 체인용 견적서', '2026-03-03 09:00:00', '2026-03-03 09:30:00'),
    (4102, 4002, 'QUO-260310-002', 2001, 3002, 1002, 'WAITING_ADMIN', 770000.00, '2026-04-06', '관리자 승인 대기 견적서', '2026-03-07 14:00:00', '2026-03-07 14:00:00');

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
    (4201, 'CNT-260310-001', 4101, 2001, 3001, 1002, 'ACTIVE_CONTRACT', 1650000.00, '2026-03-04', '2026-12-31', 'MONTHLY', '월 1회 납품', '정상 진행 계약', '2026-03-04 09:00:00', '2026-03-04 09:00:00'),
    (4202, 'CNT-260310-002', NULL, 2002, 3003, 1003, 'COMPLETED', 350000.00, '2026-03-05', '2026-06-30', 'QUARTERLY', '샘플 계약', '종결 직전 계약', '2026-03-05 09:00:00', '2026-03-05 09:00:00');

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
    (4301, 'ORD-260310-001', 4201, 2001, 3001, 1002, 1650000.00, 'CONFIRMED', '2026-03-05 09:00:00'),
    (4302, 'ORD-260310-002', 4202, 2002, 3003, 1003, 350000.00, 'CANCELED', '2026-03-05 17:00:00');

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
    (4401, 'STMT-260310-001', 4301, 3001, 1500000.00, 150000.00, 1650000.00, 'ISSUED', '2026-03-06 09:00:00');

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
    (4501, 'INV-260310-001', 4201, 2001, 3001, 1002, '2026-03-07', '2026-03-01', '2026-03-31', 1500000.00, 150000.00, 1650000.00, 'PAID', '결제 완료 청구서', '2026-03-07 09:00:00');

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
    (4601, 'PAY-260310-001', 4501, 2001, 3001, 1650000.00, 'TRANSFER', 'COMPLETED', '2026-03-09 16:00:00');

-- 확인용 샘플 쿼리
SELECT user_id, login_id, role, employee_id, client_id
FROM tbl_user
WHERE user_id IN (9001, 9002, 9003)
ORDER BY user_id;

SELECT deal_id, client_id, owner_emp_id, current_stage, current_status, latest_doc_type, latest_target_code, closed_at
FROM tbl_sales_deal
WHERE deal_id IN (3001, 3002, 3003)
ORDER BY deal_id;

SELECT surrogate_id, doc_type, doc_code, amount, expired_date, status, created_at
FROM v_document_summary
WHERE deal_id IN (3001, 3002, 3003)
ORDER BY created_at DESC, surrogate_id DESC;
