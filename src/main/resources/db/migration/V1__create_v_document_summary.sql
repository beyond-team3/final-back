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
