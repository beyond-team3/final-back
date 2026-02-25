# SeedFlow+ Entity & Enum Structure

기준 경로: `src/main/java/com/monsoon/seedflowplus`  
기준 일시: 2026-02-25  
스캔 결과: `@Entity` 30개(도메인 27 + erd 3), `enum` 28개

## 1) 공통 엔티티 베이스 구조

- `BaseEntity`
  - 공통 PK: `id`
- `BaseCreateEntity extends BaseEntity`
  - 생성 시각: `createdAt`
- `BaseModifyEntity extends BaseCreateEntity`
  - 수정 시각: `updatedAt`

상속 트리:

- `BaseEntity`
- `BaseCreateEntity`
- `BaseModifyEntity`

## 2) 도메인 엔티티 구조

### 2.1 Account

- `User` (`tbl_user`, `BaseModifyEntity`)
  - Enum: `Status`, `Role`
  - 연관: `Employee`(1:1), `Client`(1:1)
- `Employee` (`tbl_employee`, `BaseModifyEntity`)
  - 연관: `User`(1:1, mappedBy)
- `Client` (`tbl_client`, `BaseModifyEntity`)
  - Enum: `ClientType`
  - 연관: `Employee`(N:1, 담당자), `User`(1:1), `ClientCrop`(1:N)
- `ClientCrop` (`tbl_client_crops`, `BaseEntity`)
  - 연관: `Client`(N:1)

### 2.2 Product

- `Product` (`tbl_product`, `BaseModifyEntity`)
  - Enum: `ProductCategory`, `ProductStatus`
  - 특이: `tags`를 JSON 컬럼으로 저장
- `Tag` (`tbl_tag`, `BaseCreateEntity`)
- `ProductTag` (`tbl_product_tag`, `BaseCreateEntity`)
  - 연관: `Product`(N:1), `Tag`(N:1)
- `ProductBookmark` (`tbl_product_bookmark`, `BaseCreateEntity`)
  - 연관: `User`(N:1), `Product`(N:1)
  - 제약: `(account_key, product_id)` 유니크
- `ProductFeedback` (`tbl_product_feedback`, `BaseModifyEntity`)
  - 연관: `Product`(N:1), `Employee`(N:1)
- `CultivationTime` (`tbl_cultivation_time`, `BaseModifyEntity`)
  - 연관: `Product`(1:1)

### 2.3 Document (Request / Quotation / Contract / Order / Invoice)

- RFQ
  - `QuotationRequestHeader` (`tbl_request_quotation_header`, `BaseModifyEntity`)
    - Enum: `QuotationRequestStatus`
    - 연관: `Client`(N:1), `QuotationRequestDetail`(1:N)
  - `QuotationRequestDetail` (`tbl_request_quotation_detail`, `BaseEntity`)
    - 연관: `QuotationRequestHeader`(N:1), `Product`(N:1)

- Quotation
  - `QuotationHeader` (`tbl_quotation_header`, `BaseModifyEntity`)
    - Enum: `domain.document.quotation.entity.QuotationStatus`
    - 연관: `QuotationRequestHeader`(N:1), `Client`(N:1), `Employee`(N:1), `QuotationDetail`(1:N)
  - `QuotationDetail` (`tbl_quotation_detail`, `BaseEntity`)
    - 연관: `QuotationHeader`(N:1), `Product`(N:1)

- Contract
  - `ContractHeader` (`tbl_contract_header`, `BaseModifyEntity`)
    - Enum: `domain.document.contract.entity.ContractStatus`, `BillingCycle`
    - 연관: `QuotationHeader`(N:1), `Client`(N:1), `Employee`(N:1), `ContractDetail`(1:N)
    - 특이: `createdAt`를 `issue_date` 컬럼으로 override
  - `ContractDetail` (`tbl_contract_detail`, `BaseEntity`)
    - 연관: `ContractHeader`(N:1), `Product`(N:1)

- Order
  - `OrderHeader` (`tbl_order_header`, `BaseCreateEntity`)
    - Enum: `OrderStatus`
    - 연관: `ContractHeader`(N:1), `Client`(N:1), `Employee`(N:1)
  - `OrderDetail` (`tbl_order_detail`, `BaseEntity`)
    - 연관: `OrderHeader`(N:1), `ContractDetail`(N:1)

- Invoice
  - `Invoice` (`tbl_invoice`, `BaseCreateEntity`)
    - Enum: `InvoiceStatus`
    - 연관: `Client`(N:1), `Employee`(N:1)
    - 특이: `contractId`는 연관 대신 scalar ID로 보관

### 2.4 Statement / Payment

- `Statement` (`tbl_statement`, `BaseCreateEntity`)
  - Enum: `StatementStatus`
  - 연관: `OrderHeader`(N:1)
  - 특이: `invoiceId`는 scalar ID
- `Payment` (`tbl_payment`, `BaseCreateEntity`)
  - Enum: `PaymentMethod`, `PaymentStatus`
  - 연관: `Invoice`(N:1), `Client`(N:1)

### 2.5 Notification

- `Notification` (`tbl_notification`, `BaseCreateEntity`)
  - Enum: `NotificationType`, `NotificationTargetType`
  - 연관: `User`(N:1)
- `NotificationDelivery` (`tbl_notification_delivery`, `BaseModifyEntity`)
  - Enum: `DeliveryChannel`, `DeliveryStatus`
  - 연관: `Notification`(N:1)
  - 제약: `(notification_id, channel)` 유니크

### 2.6 Sales / Schedule / Note

- `SalesHistory` (`tbl_sales_history`, `BaseCreateEntity`)
  - Enum: `PipelineType`, `PipelineStage`, `ActionType`, `ActorType`
  - 연관: `Employee`(N:1), `Client`(N:1)
  - 특이: `fromStatus/toStatus`는 `String` 저장 (`DocumentStatus` 구현 enum의 `name()`)
- `PersSked` (`tbl_pers_sked`, `BaseModifyEntity`)
  - 연관: `User`(N:1)
- `SalesNote` (`tbl_sales_note`, `BaseModifyEntity`)
  - 특이: `clientId`, `authorId`를 scalar로 저장
- `SalesBriefing` (`tbl_sales_briefing`, `BaseModifyEntity`)
  - 특이: `clientId` + JSON 컬럼들로 요약 데이터 저장

## 3) ERD 전용 엔티티(참고)

`erd/sales` 패키지의 아래 3개는 도메인 엔티티와 별도로 ERD 표현용 성격이 강함.

- `SalesNoteErd`
- `SalesBriefingErd`
- `BriefingSourceNoteErd`

## 4) Enum 전체 목록

### 4.1 Account / Product / Payment / Notification

- Account
  - `ClientType`, `Role`, `Status`
- Product
  - `ProductCategory`, `ProductStatus`
- Payment
  - `PaymentMethod`, `PaymentStatus`
- Notification
  - `NotificationType`, `NotificationTargetType`, `DeliveryChannel`, `DeliveryStatus`

### 4.2 Document 관련

- `QuotationRequestStatus`
- `domain.document.quotation.entity.QuotationStatus`
- `domain.document.contract.entity.ContractStatus`
- `OrderStatus`
- `InvoiceStatus`
- `StatementStatus`
- `BillingCycle`

### 4.3 Pipeline / Sales

- `DocumentStatus` (interface)
- `PipelineType`, `PipelineStage`, `ActionType`
- `RequestStatus` (`domain.rfq.entity`)
- `QuotationStatus` (`domain.quotation.entity`)
- `ContractStatus` (`domain.contract.entity`)
- `ActorType`

### 4.4 Core 공통

- `ResultType`
- `ErrorCode`, `ErrorType`

## 5) 겹치는 Enum 정리

### 5.1 이름이 완전히 겹치는 enum (실제 충돌 가능성이 큰 케이스)

1. `ContractStatus`
- `src/main/java/com/monsoon/seedflowplus/domain/contract/entity/ContractStatus.java`
- `src/main/java/com/monsoon/seedflowplus/domain/document/contract/entity/ContractStatus.java`

2. `QuotationStatus`
- `src/main/java/com/monsoon/seedflowplus/domain/quotation/entity/QuotationStatus.java`
- `src/main/java/com/monsoon/seedflowplus/domain/document/quotation/entity/QuotationStatus.java`

영향:
- import 시 혼동 가능
- 서비스/매핑 레이어에서 다른 enum을 참조해도 컴파일은 되는 경우가 있어 런타임 로직 불일치 위험

### 5.2 의미가 많이 겹치는 상태 enum

1. RFQ 상태
- `QuotationRequestStatus`: `PENDING`, `REVIEWING`, `COMPLETED`
- `RequestStatus`: `PENDING`, `REVIEWING`, `CANCELED`
- 겹침: `PENDING`, `REVIEWING`
- 차이: 종결 상태가 `COMPLETED` vs `CANCELED`

2. 계약/견적 승인 플로우
- `domain.contract.entity.ContractStatus`는 `ADMIN_PENDING/CLIENT_PENDING` 스타일
- `domain.document.contract.entity.ContractStatus`는 `WAITING_ADMIN/WAITING_CLIENT` 스타일
- `domain.quotation.entity.QuotationStatus`와 `domain.document.quotation.entity.QuotationStatus`도 동일한 패턴으로 중복

### 5.3 Pipeline 매핑 기준과 실제 엔티티 enum 사용처 불일치

`DocumentStatus` 주석 기준 매핑:
- RFQ -> `RequestStatus`
- QUO -> `domain.quotation.entity.QuotationStatus`
- CNT -> `domain.contract.entity.ContractStatus`
- ORD/STMT/INV/PAY -> 각 상태 enum

하지만 실제 헤더 엔티티는 다음 enum 사용:
- `QuotationHeader.status` -> `domain.document.quotation.entity.QuotationStatus`
- `ContractHeader.status` -> `domain.document.contract.entity.ContractStatus`
- `QuotationRequestHeader.status` -> `QuotationRequestStatus`

즉, Pipeline 설명과 실제 영속 enum이 일부 다름.

## 6) 정리 제안

1. 상태 enum 단일 소스화
- 문서 상태는 패키지 1곳(`domain.document.*` 또는 `domain.pipeline.*`)으로 통합
- 동일 이름 중복(`QuotationStatus`, `ContractStatus`) 제거

2. `DocumentStatus` 구현체 기준 통일
- SalesHistory에서 사용하는 문서 상태 enum을 실제 엔티티 상태 enum과 1:1로 맞춤

3. 전환 기간 운영 방식
- 통합 전에는 FQCN(패키지 포함 타입명) 명시 + mapper를 통해 변환 로직 중앙화

