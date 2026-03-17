CREATE OR REPLACE VIEW v_document_summary AS

SELECT CONCAT('RFQ-', rfq_id) AS surrogate_id,
       'RFQ' AS doc_type, rfq.rfq_id AS doc_id, rfq.deal_id, rfq.client_id,
       rfq.request_code AS doc_code, NULL AS amount, NULL AS expired_date,
       CONCAT('', rfq.request_status) AS status, rfq.created_at,
       c.client_name AS client_name, e.employee_name AS owner_employee_name
FROM tbl_request_quotation_header rfq
         LEFT JOIN tbl_client c ON c.client_id = rfq.client_id
         LEFT JOIN tbl_sales_deal d ON d.deal_id = rfq.deal_id
         LEFT JOIN tbl_employee e ON e.employee_id = d.owner_emp_id
WHERE rfq.request_status != 'DELETED'

UNION ALL

SELECT CONCAT('QUO-', quo_id),
       'QUO', quo.quo_id, quo.deal_id, quo.client_id,
       quo.quotation_code, quo.total_amount, quo.expired_date,
       CONCAT('', quo.status) AS status, quo.created_at,
       c.client_name AS client_name, e.employee_name AS owner_employee_name
FROM tbl_quotation_header quo
         LEFT JOIN tbl_client c ON c.client_id = quo.client_id
         LEFT JOIN tbl_sales_deal d ON d.deal_id = quo.deal_id
         LEFT JOIN tbl_employee e ON e.employee_id = d.owner_emp_id
WHERE quo.status != 'DELETED'

UNION ALL

SELECT CONCAT('CNT-', cnt_id),
       'CNT', cnt.cnt_id, cnt.deal_id, cnt.client_id,
       cnt.contract_code, cnt.total_amount, cnt.end_date,
       CONCAT('', cnt.status) AS status, cnt.issue_date,
       c.client_name AS client_name, e.employee_name AS owner_employee_name
FROM tbl_contract_header cnt
         LEFT JOIN tbl_client c ON c.client_id = cnt.client_id
         LEFT JOIN tbl_sales_deal d ON d.deal_id = cnt.deal_id
         LEFT JOIN tbl_employee e ON e.employee_id = d.owner_emp_id
WHERE cnt.status != 'DELETED'

UNION ALL

SELECT CONCAT('ORD-', o.order_id),
       'ORD', o.order_id, o.deal_id, d.client_id,
       o.order_code, o.total_amount, NULL,
       CONCAT('', o.status) AS status, o.created_at,
       c.client_name AS client_name, e.employee_name AS owner_employee_name
FROM tbl_order_header o
         JOIN tbl_sales_deal d ON d.deal_id = o.deal_id
         LEFT JOIN tbl_client c ON c.client_id = d.client_id
         LEFT JOIN tbl_employee e ON e.employee_id = d.owner_emp_id

UNION ALL

SELECT CONCAT('STMT-', statement_id),
       'STMT', s.statement_id, s.deal_id, d.client_id,
       s.statement_code, s.total_amount, NULL,
       CONCAT('', s.status) AS status, s.created_at,
       c.client_name AS client_name, e.employee_name AS owner_employee_name
FROM tbl_statement s
         JOIN tbl_sales_deal d ON d.deal_id = s.deal_id
         LEFT JOIN tbl_client c ON c.client_id = d.client_id
         LEFT JOIN tbl_employee e ON e.employee_id = d.owner_emp_id

UNION ALL

SELECT CONCAT('INV-', i.invoice_id),
       'INV', i.invoice_id, i.deal_id, d.client_id,
       i.invoice_code, i.total_amount, i.end_date,
       CONCAT('', i.status) AS status, i.created_at,
       c.client_name AS client_name, e.employee_name AS owner_employee_name
FROM tbl_invoice i
         JOIN tbl_sales_deal d ON d.deal_id = i.deal_id
         LEFT JOIN tbl_client c ON c.client_id = d.client_id
         LEFT JOIN tbl_employee e ON e.employee_id = d.owner_emp_id

UNION ALL

SELECT CONCAT('PAY-', p.payment_id),
       'PAY', p.payment_id, p.deal_id, d.client_id,
       p.payment_code, p.payment_amount, NULL,
       CONCAT('', p.status) AS status, p.created_at,
       c.client_name AS client_name, e.employee_name AS owner_employee_name
FROM tbl_payment p
         JOIN tbl_sales_deal d ON d.deal_id = p.deal_id
         LEFT JOIN tbl_client c ON c.client_id = d.client_id
         LEFT JOIN tbl_employee e ON e.employee_id = d.owner_emp_id;
