ALTER TABLE tbl_invoice
    ADD COLUMN IF NOT EXISTS paid_at DATE NULL;

CREATE INDEX IF NOT EXISTS idx_invoice_status_paid_at
    ON tbl_invoice (status, paid_at);

UPDATE tbl_invoice
SET paid_at = invoice_date
WHERE status = 'PAID'
  AND paid_at IS NULL;
