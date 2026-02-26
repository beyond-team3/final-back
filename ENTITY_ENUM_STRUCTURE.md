# SeedFlow+ Entity & Enum Structure (Current)

기준 경로: `src/main/java/com/monsoon/seedflowplus/domain`  
기준 일시: 2026-02-25  
스캔 기준: `@Entity`, `public enum`, `implements DocumentStatus`

- `@Entity`: 29개
- `enum`: 23개
- `DocumentStatus` 구현 enum: 8개

## 1) 현재 패키지 구조 요약

- `domain/account/entity`
- `domain/product/{entity,repository,service,dto}`
- `domain/deal/entity`
- `domain/sales/{request,quotation,contract,order,invoice}`
- `domain/billing/{invoice,payment,statement}`
- `domain/notification/entity`
- `domain/note/entity`
- `domain/schedule/entity`
- `domain/document/order/controller` (레거시 컨트롤러 패키지 잔존)

## 2) 엔티티 목록 (도메인별)

### Account

- `Client` (`tbl_client`, `BaseModifyEntity`)
- `ClientCrop` (`tbl_client_crops`, `BaseEntity`)
- `Employee` (`tbl_employee`, `BaseModifyEntity`)
- `User` (`tbl_user`, `BaseModifyEntity`)

### Product

- `Product` (`tbl_product`, `BaseModifyEntity`)
- `Tag` (`tbl_tag`, `BaseCreateEntity`)
- `ProductTag` (`tbl_product_tag`, `BaseCreateEntity`)
- `ProductBookmark` (`tbl_product_bookmark`, `BaseCreateEntity`)
- `ProductFeedback` (`tbl_product_feedback`, `BaseModifyEntity`)
- `CultivationTime` (`tbl_cultivation_time`, `BaseModifyEntity`)

### Sales

- `QuotationRequestHeader` (`tbl_request_quotation_header`, `BaseModifyEntity`)
- `QuotationRequestDetail` (`tbl_request_quotation_detail`, `BaseEntity`)
- `QuotationHeader` (`tbl_quotation_header`, `BaseModifyEntity`)
- `QuotationDetail` (`tbl_quotation_detail`, `BaseEntity`)
- `ContractHeader` (`tbl_contract_header`, `BaseModifyEntity`)
- `ContractDetail` (`tbl_contract_detail`, `BaseEntity`)
- `OrderHeader` (`tbl_order_header`, `BaseCreateEntity`)
- `OrderDetail` (`tbl_order_detail`, `BaseEntity`)
- `Invoice` (`tbl_invoice`, `BaseCreateEntity`)  
  path: `domain/sales/invoice/entity/Invoice`

### Billing

- `Invoice` (`tbl_invoice`, `BaseCreateEntity`)  
  path: `domain/billing/invoice/entity/Invoice`
- `Payment` (`tbl_payment`, `BaseCreateEntity`)
- `Statement` (`tbl_statement`, `BaseCreateEntity`)

### Deal

- `SalesDealLog` (`tbl_sales_deal_log`, `BaseCreateEntity`)

### Notification

- `Notification` (`tbl_notification`, `BaseCreateEntity`)
- `NotificationDelivery` (`tbl_notification_delivery`, `BaseModifyEntity`)

### Note

- `SalesNote` (`tbl_sales_note`, `BaseModifyEntity`)
- `SalesBriefing` (`tbl_sales_briefing`, `BaseModifyEntity`)

### Schedule

- `PersSked` (`tbl_pers_sked`, `BaseModifyEntity`)

## 3) Enum 목록 (도메인별)

### Account

- `ClientType`
- `Role`
- `Status`

### Product

- `ProductCategory`
- `ProductStatus`

### Sales

- `QuotationRequestStatus` (`implements DocumentStatus`)
- `QuotationStatus` (`implements DocumentStatus`)
- `ContractStatus` (`implements DocumentStatus`)
- `BillingCycle`
- `OrderStatus` (`implements DocumentStatus`)
- `InvoiceStatus` (`implements DocumentStatus`)  
  path: `domain/sales/invoice/entity/InvoiceStatus`

### Billing

- `InvoiceStatus` (`implements DocumentStatus`)  
  path: `domain/billing/invoice/entity/InvoiceStatus`
- `PaymentMethod`
- `PaymentStatus` (`implements DocumentStatus`)
- `StatementStatus` (`implements DocumentStatus`)

### Deal

- `DealType`
- `DealStage`
- `ActionType`
- `ActorType`
- `DocumentStatus` (interface)

### Notification

- `NotificationType`
- `NotificationTargetType`
- `DeliveryChannel`
- `DeliveryStatus`

## 4) 현재 충돌/중복 및 불일치 포인트

### A. 동일 엔티티 중복 (중요)

- `Invoice`가 2개 존재
- `domain/sales/invoice/entity/Invoice`
- `domain/billing/invoice/entity/Invoice`
- 두 클래스 모두 `@Table(name = "tbl_invoice")` 사용

### B. 동일 enum 중복 (중요)

- `InvoiceStatus`가 2개 존재
- `domain/sales/invoice/entity/InvoiceStatus`
- `domain/billing/invoice/entity/InvoiceStatus`
- 두 enum 모두 `DocumentStatus` 구현 + 상수 동일

### C. Request 패키지 명칭 불일치

- 현재 RFQ는 `domain/sales/request/entity/*`
- `domain/sales/rfq/entity/*`가 아님

### D. Note 패키지 위치 불일치

- 현재 `SalesNote`, `SalesBriefing`은 `domain/note/entity`
- `domain/sales/common/entity`가 아님

### E. Controller 중복 잔존

- `domain/document/order/controller/OrderController`
- `domain/sales/order/controller/OrderController`

### F. Deal 문서 주석과 실제 enum 명칭 불일치

- `DocumentStatus`/`DealStage` 주석은 `RfqStatus` 등을 참조
- 실제 구현은 `QuotationRequestStatus` 사용
- `QuotationStatus` 주석 예시와 실제 상수(`WAITING_ADMIN`, `FINAL_APPROVED` 등)도 다름

## 5) 참고: `DocumentStatus` 구현 enum (실제)

- `domain/sales/request/entity/QuotationRequestStatus`
- `domain/sales/quotation/entity/QuotationStatus`
- `domain/sales/contract/entity/ContractStatus`
- `domain/sales/order/entity/OrderStatus`
- `domain/sales/invoice/entity/InvoiceStatus`
- `domain/billing/invoice/entity/InvoiceStatus`
- `domain/billing/statement/entity/StatementStatus`
- `domain/billing/payment/entity/PaymentStatus`
