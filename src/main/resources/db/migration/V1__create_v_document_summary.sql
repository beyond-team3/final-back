CREATE OR REPLACE VIEW v_document_summary AS

SELECT CONCAT('RFQ-', rfq_id) AS surrogate_id,
       'RFQ' AS doc_type, rfq_id AS doc_id, deal_id, client_id,
       request_code AS doc_code, NULL AS amount, NULL AS expired_date,
       request_status AS status, created_at
FROM tbl_request_quotation_header
WHERE request_status != 'DELETED'

UNION ALL

SELECT CONCAT('QUO-', quo_id),
       'QUO', quo_id, deal_id, client_id,
       quotation_code, total_amount, expired_date,
       status, created_at
FROM tbl_quotation_header
WHERE status != 'DELETED'

UNION ALL

SELECT CONCAT('CNT-', cnt_id),
       'CNT', cnt_id, deal_id, client_id,
       contract_code, total_amount, end_date,
       status, issue_date
FROM tbl_contract_header
WHERE status != 'DELETED'

UNION ALL

SELECT CONCAT('ORD-', order_id),
       'ORD', order_id, deal_id, client_id,
       order_code, total_amount, NULL,
       status, created_at
FROM tbl_order_header

UNION ALL

SELECT CONCAT('STMT-', statement_id),
       'STMT', statement_id, deal_id, NULL,
       statement_code, total_amount, NULL,
       status, created_at
FROM tbl_statement

UNION ALL

SELECT CONCAT('INV-', invoice_id),
       'INV', invoice_id, deal_id, client_id,
       invoice_code, total_amount, end_date,
       status, created_at
FROM tbl_invoice

UNION ALL

SELECT CONCAT('PAY-', payment_id),
       'PAY', payment_id, deal_id, client_id,
       payment_code, payment_amount, NULL,
       status, created_at
FROM tbl_payment;
