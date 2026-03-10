-- BUG-5 statistics inspection queries
-- Replace the placeholder invoice id before running.

SET @inv_id = 0;

-- 1. Invoice base row
SELECT invoice_id, invoice_code, invoice_date, status, contract_id, deal_id
FROM tbl_invoice
WHERE invoice_id = @inv_id;

-- 2. InvoiceStatement links
SELECT invoice_statement_id, invoice_id, statement_id, is_included
FROM tbl_invoice_statement
WHERE invoice_id = @inv_id;

-- 3. Linked statement status
SELECT s.statement_id, s.statement_code, s.status, s.order_id, s.total_amount
FROM tbl_statement s
JOIN tbl_invoice_statement ist ON ist.statement_id = s.statement_id
WHERE ist.invoice_id = @inv_id;

-- 4. Order details behind the statement
SELECT od.order_detail_id, od.order_id, od.contract_detail_id, od.quantity
FROM tbl_order_detail od
JOIN tbl_statement s ON s.order_id = od.order_id
JOIN tbl_invoice_statement ist ON ist.statement_id = s.statement_id
WHERE ist.invoice_id = @inv_id;

-- 5. Contract detail/category lineage
SELECT
  od.order_detail_id,
  cd.cnt_detail_id,
  cd.product_category,
  cd.product_name,
  cd.unit_price,
  od.quantity
FROM tbl_order_detail od
JOIN tbl_contract_detail cd ON cd.cnt_detail_id = od.contract_detail_id
JOIN tbl_statement s ON s.order_id = od.order_id
JOIN tbl_invoice_statement ist ON ist.statement_id = s.statement_id
WHERE ist.invoice_id = @inv_id;

-- 6. Near-production statistics join reproduction for one invoice
SELECT
  i.invoice_id,
  i.invoice_code,
  i.invoice_date,
  i.status AS invoice_status,
  ist.is_included,
  s.status AS statement_status,
  cd.product_category,
  cd.unit_price,
  od.quantity,
  (COALESCE(cd.unit_price, 0) * COALESCE(od.quantity, 0)) AS billed_line_amount
FROM tbl_invoice i
JOIN tbl_invoice_statement ist ON ist.invoice_id = i.invoice_id
JOIN tbl_statement s ON s.statement_id = ist.statement_id
JOIN tbl_order_detail od ON od.order_id = s.order_id
JOIN tbl_contract_detail cd ON cd.cnt_detail_id = od.contract_detail_id
WHERE i.invoice_id = @inv_id
  AND i.status IN ('PUBLISHED', 'PAID')
  AND ist.is_included = true
  AND s.status = 'ISSUED';

-- 7. Whole-year aggregate candidate count
SELECT
  COUNT(*) AS row_count,
  COALESCE(SUM(cd.unit_price * od.quantity), 0) AS billed_amount
FROM tbl_invoice i
JOIN tbl_invoice_statement ist ON ist.invoice_id = i.invoice_id
JOIN tbl_statement s ON s.statement_id = ist.statement_id
JOIN tbl_order_detail od ON od.order_id = s.order_id
JOIN tbl_contract_detail cd ON cd.cnt_detail_id = od.contract_detail_id
WHERE i.invoice_date BETWEEN '2026-01-01' AND '2026-12-31'
  AND i.status IN ('PUBLISHED', 'PAID')
  AND ist.is_included = true
  AND s.status = 'ISSUED';
