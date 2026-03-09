-- SeedFlow+ scenario1 재실행용 DB 초기화 스크립트
-- 전제: MariaDB / MySQL
-- 주의: 개발 DB 데이터가 모두 삭제된다.

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
