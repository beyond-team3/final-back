# API_SPEC_V2.md

## 1. 목적

본 문서는 Sales Deal Pipeline 리모델링 결과에 대한 `/api/v2/**` 기준 API 명세 초안이다.

v2의 핵심 원칙은 다음과 같다.

- Deal 중심 조회를 기본으로 한다.
- 문서는 Deal 아래에서 다건으로 보존한다.
- 재작성(revise)은 기존 문서 수정이 아니라 새 문서 생성으로 처리한다.
- Deal snapshot은 문서 집합 기준으로 재계산한다.
- 알림/일정/통계는 v2 기준으로만 집계 및 조회한다.

---

## 2. 최종 확정 정책

### 2.1 Deal 정책
- Deal은 하나의 client에 대한 하나의 영업 기회(opportunity) 단위이다.
- 같은 client에 대해 동시 다중 deal을 허용한다.
- 새 deal은 아래 경우에만 생성한다.
  - RFQ 생성 시
  - 상위 문서 없이 QUO 생성 시
  - 상위 문서 없이 CNT 생성 시
  - 명시적 Deal 생성 API 호출 시
- 같은 client의 최근 열린 deal에 자동 연결하는 정책은 금지한다.

### 2.2 문서 연결 정책
- 상위 문서가 있으면 상위 문서의 `dealId`를 계승한다.
- 상위 문서가 없고 `dealId`가 명시되면 해당 deal에 연결한다.
- 둘 다 없으면 새 deal을 생성한다.

### 2.3 상위 문서 없는 생성 허용 여부
- RFQ: 가능
- QUO: 가능
- CNT: 가능
- ORD: 불가
- STMT: 불가
- INV: 불가
- PAY: 불가

### 2.4 단계별 제약
- RFQ는 승인 절차가 없다.
- CNT 없이 ORD를 생성할 수 없다.
- Payment 완료 상태의 source of truth는 `PaymentStatus.COMPLETED` 하나만 사용한다.
- 통계 집계는 v2 기준으로만 수행한다.

### 2.5 재작성 정책
- QUO/CNT 재작성은 새 문서 생성이다.
- 원본 문서는 유지한다.
- 재작성본은 아래 필드를 가진다.
  - `sourceDocumentId`
  - `revisionGroupKey`
  - `revisionNo`

### 2.6 삭제 정책
- 업무 문서는 물리 삭제하지 않는다.
- 사용자 액션 “삭제”는 상태 전이로만 처리한다.
  - 초안/진행 취소: `CANCELLED`
  - 재작성으로 대체: `SUPERSEDED`

### 2.7 상태 축 분리
모든 문서는 상태를 분리한다.
- 생명주기 상태: `LifecycleStatus`
- 승인 상태: `ApprovalStatus` (승인 대상 문서만)
- 문서 역할 상태: `DocumentRole`

### 2.8 Deal 종료 정책
성공 종료:
- `PaymentStatus.COMPLETED`
- 진행 중 문서 없음

실패 종료:
- 모든 문서가 종료 계열 상태
- 재개 예정 없음

### 2.9 Reopen 정책
- 닫힌 deal은 자동 reopen 하지 않는다.
- `POST /api/v2/deals/{dealId}/reopen` 으로만 재개한다.

### 2.10 알림/일정 테이블 정책
- v2는 전용 테이블 사용을 허용한다.
- 권장안: 기존 v1과 분리된 v2 전용 테이블 사용
  - 이유: 기존 의미 혼선 방지, 마이그레이션 단순화, v2 집계 기준 고정

### 2.11 유니크 제약 권장안
대표(active) 문서가 단계별 1개만 유지돼야 하므로 DB 차원의 일부 제약을 권장한다.

권장안:
1. 문서 번호 unique
   - `rfq_no`, `quotation_no`, `contract_no`, `order_no`, `statement_no`, `invoice_no`, `payment_no`
2. `deal_code` unique
3. 대표 문서 제약은 완전한 DB unique보다 애플리케이션 + 부분 unique 인덱스 병행 권장

추천 이유:
- 같은 deal 안에 과거 문서/재작성본/취소본은 여러 건 존재해야 한다.
- 따라서 `deal_id + stage` 전체 unique는 부적절하다.
- 대신 아래 제약이 적절하다.

가능한 DB가 partial unique index를 지원하면:
- `deal_id + is_representative + lifecycle_active_group` 에 대해 대표 문서 1건만 허용

MariaDB 계열에서 partial unique가 까다로우면 권장 방식:
- DB는 일반 인덱스만 두고
- 서비스 계층에서 “같은 deal, 같은 단계의 active representative 1건만 허용” 검증
- 트랜잭션 내에서 기존 대표 문서를 `NON_REPRESENTATIVE/SUPERSEDED` 처리 후 신규 대표 지정

최종 추천:
- **문서 번호 unique + deal_code unique는 DB 강제**
- **대표 문서 1건 정책은 서비스 계층 강제**
- 필요 시 추후 DB generated column/functional index 검토

---

## 3. 리소스 모델

### 3.1 Deal
- Deal은 영업 기회의 최상위 집계 루트이다.
- 문서, 로그, 알림, 일정, 통계의 조회 문맥이 된다.

### 3.2 문서 종류
- RFQ
- Quotation
- Contract
- Order
- Statement
- Invoice
- Payment

### 3.3 Approval 대상
- Quotation
- Contract

RFQ에는 approval이 없다.

---

## 4. 상태 Enum

## 4.1 공통 Enum

```java
public enum DocumentType {
    RFQ, QUOTATION, CONTRACT, ORDER, STATEMENT, INVOICE, PAYMENT
}
```

```java
public enum DealStage {
    RFQ, QUOTATION, CONTRACT, ORDER, STATEMENT, INVOICE, PAYMENT
}
```

```java
public enum DealStatus {
    OPEN,
    IN_PROGRESS,
    WAITING_INTERNAL_APPROVAL,
    WAITING_CLIENT_APPROVAL,
    EXECUTING,
    CLOSED_WON,
    CLOSED_LOST,
    CLOSED_CANCELLED
}
```

## 4.2 문서 생명주기

```java
public enum LifecycleStatus {
    DRAFT,
    ACTIVE,
    COMPLETED,
    REJECTED,
    CANCELLED,
    EXPIRED,
    SUPERSEDED
}
```

## 4.3 문서 역할

```java
public enum DocumentRole {
    REPRESENTATIVE,
    NON_REPRESENTATIVE,
    REVISION_SOURCE,
    SUPERSEDED
}
```

## 4.4 승인 상태

```java
public enum ApprovalStatus {
    NOT_REQUESTED,
    PENDING_INTERNAL,
    PENDING_CLIENT,
    APPROVED,
    REJECTED_INTERNAL,
    REJECTED_CLIENT
}
```

## 4.5 후행 문서 상태

```java
public enum FulfillmentStatus {
    PENDING,
    CONFIRMED,
    DELIVERED,
    FAILED
}
```

```java
public enum StatementStatus {
    ISSUED,
    CANCELLED
}
```

```java
public enum InvoiceStatus {
    DRAFT,
    PUBLISHED,
    CANCELLED
}
```

```java
public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    CANCELLED
}
```

## 4.6 종료 사유

```java
public enum DealCloseReason {
    PAYMENT_COMPLETED,
    ALL_DOCUMENTS_TERMINATED,
    USER_CANCELLED,
    EXPIRED,
    REJECTED
}
```

---

## 5. ERD 초안

## 5.1 핵심 테이블

### `tbl_deal`
- `deal_id` PK
- `deal_code` UK
- `deal_title`
- `client_id`
- `owner_employee_id`
- `product_category_code`
- `lead_source`
- `memo`
- `current_stage`
- `current_status`
- `representative_document_type`
- `representative_document_id`
- `last_activity_at`
- `is_closed`
- `closed_at`
- `closed_reason`
- `created_at`
- `created_by`
- `modified_at`
- `modified_by`

### `tbl_rfq`
- `rfq_id` PK
- `deal_id` FK
- `client_id`
- `owner_employee_id`
- `rfq_no` UK
- `title`
- `description`
- `lifecycle_status`
- `document_role`
- `is_representative`
- `requested_at`
- `expired_at`
- audit fields

### `tbl_quotation`
- `quotation_id` PK
- `deal_id` FK
- `base_rfq_id` FK nullable
- `quotation_no` UK
- `title`
- `description`
- `lifecycle_status`
- `approval_status`
- `document_role`
- `is_representative`
- `source_quotation_id` nullable
- `revision_group_key`
- `revision_no`
- `requested_at`
- `respond_due_date`
- `approved_at`
- `rejected_at`
- `expired_at`
- audit fields

### `tbl_contract`
- `contract_id` PK
- `deal_id` FK
- `base_quotation_id` FK nullable
- `contract_no` UK
- `title`
- `description`
- `lifecycle_status`
- `approval_status`
- `document_role`
- `is_representative`
- `source_contract_id` nullable
- `revision_group_key`
- `revision_no`
- `contract_start_date`
- `contract_end_date`
- `approved_at`
- `rejected_at`
- `expired_at`
- audit fields

### `tbl_order`
- `order_id` PK
- `deal_id` FK
- `contract_id` FK not null
- `order_no` UK
- `title`
- `lifecycle_status`
- `fulfillment_status`
- `document_role`
- `is_representative`
- `ordered_at`
- `confirmed_at`
- `delivered_at`
- audit fields

### `tbl_statement`
- `statement_id` PK
- `deal_id` FK
- `order_id` FK not null
- `statement_no` UK
- `title`
- `lifecycle_status`
- `statement_status`
- `document_role`
- `is_representative`
- `issued_at`
- audit fields

### `tbl_invoice`
- `invoice_id` PK
- `deal_id` FK
- `statement_id` FK not null
- `invoice_no` UK
- `title`
- `lifecycle_status`
- `invoice_status`
- `document_role`
- `is_representative`
- `issued_at`
- `published_at`
- audit fields

### `tbl_payment`
- `payment_id` PK
- `deal_id` FK
- `invoice_id` FK not null
- `payment_no` UK
- `lifecycle_status`
- `payment_status`
- `document_role`
- `is_representative`
- `requested_at`
- `completed_at`
- `amount`
- `currency_code`
- audit fields

### `tbl_approval_request_v2`
- `approval_request_id` PK
- `deal_id` FK
- `document_type`
- `document_id`
- `approval_line_type`
- `approval_status`
- `current_step_order`
- `requested_by_employee_id`
- `requested_at`
- `completed_at`
- audit fields

### `tbl_approval_step_v2`
- `approval_step_id` PK
- `approval_request_id` FK
- `step_order`
- `approver_type`
- `approver_employee_id`
- `approver_client_contact_id`
- `decision_status`
- `decided_at`
- `comment`
- audit fields

### `tbl_deal_log_v2`
- `deal_log_id` PK
- `deal_id` FK
- `document_type`
- `document_id`
- `event_type`
- `event_message`
- `actor_type`
- `actor_id`
- `occurred_at`
- audit fields

### `tbl_notification_delivery_v2`
- `notification_id` PK
- `deal_id` FK
- `document_type`
- `document_id`
- `recipient_type`
- `recipient_id`
- `notification_type`
- `title`
- `body`
- `is_read`
- `read_at`
- audit fields

### `tbl_deal_schedule_v2`
- `schedule_id` PK
- `deal_id` FK
- `document_type`
- `document_id`
- `schedule_type`
- `title`
- `description`
- `schedule_date`
- `schedule_status`
- audit fields

---

## 6. 엔티티 초안

## 6.1 Deal

```java
class Deal extends BaseModifyEntity {
    Long dealId;
    String dealCode;
    String dealTitle;

    Long clientId;
    Long ownerEmployeeId;
    String productCategoryCode;
    String leadSource;
    String memo;

    DealStage currentStage;
    DealStatus currentStatus;
    DocumentType representativeDocumentType;
    Long representativeDocumentId;
    LocalDateTime lastActivityAt;

    boolean closed;
    LocalDateTime closedAt;
    DealCloseReason closedReason;

    void recalculateSnapshot(DealSnapshotAggregate aggregate);
    void close(DealCloseReason reason);
    void reopen();
}
```

## 6.2 공통 문서 추상화

```java
abstract class DealDocument extends BaseModifyEntity {
    Long id;
    Long dealId;

    String documentNo;
    String title;
    String description;

    LifecycleStatus lifecycleStatus;
    DocumentRole documentRole;
    boolean representative;
}
```

## 6.3 승인형 문서 추상화

```java
abstract class ApprovableDealDocument extends DealDocument {
    ApprovalStatus approvalStatus;
    LocalDateTime approvedAt;
    LocalDateTime rejectedAt;
    LocalDateTime expiredAt;
}
```

## 6.4 Quotation

```java
class Quotation extends ApprovableDealDocument {
    Long baseRfqId;

    Long sourceQuotationId;
    String revisionGroupKey;
    Integer revisionNo;

    LocalDate respondDueDate;
}
```

## 6.5 Contract

```java
class Contract extends ApprovableDealDocument {
    Long baseQuotationId;

    Long sourceContractId;
    String revisionGroupKey;
    Integer revisionNo;

    LocalDate contractStartDate;
    LocalDate contractEndDate;
}
```

## 6.6 Order / Statement / Invoice / Payment

```java
class Order extends DealDocument {
    Long contractId;
    FulfillmentStatus fulfillmentStatus;
}

class Statement extends DealDocument {
    Long orderId;
    StatementStatus statementStatus;
}

class Invoice extends DealDocument {
    Long statementId;
    InvoiceStatus invoiceStatus;
}

class Payment extends DealDocument {
    Long invoiceId;
    PaymentStatus paymentStatus;
    BigDecimal amount;
}
```

---

## 7. Snapshot 계산 규칙

### 7.1 목적
Deal snapshot은 수동 롤백이 아니라 문서 집합 기준 재계산으로 정한다.

### 7.2 단계 우선순위
- PAYMENT
- INVOICE
- STATEMENT
- ORDER
- CONTRACT
- QUOTATION
- RFQ

### 7.3 같은 단계 내 우선순위
1. `REPRESENTATIVE`
2. `LifecycleStatus.ACTIVE`
3. 승인 상태 우선순위
   - `PENDING_CLIENT`
   - `PENDING_INTERNAL`
   - `APPROVED`
   - `REJECTED_*`
4. `createdAt` 최신

### 7.4 재계산 트리거
아래 이벤트 발생 시 snapshot 전면 재계산:
- 문서 생성
- 승인 요청
- 승인/반려
- 재작성
- 취소
- 만료
- 결제 완료

---

## 8. API 공통 규칙

- Base path: `/api/v2`
- 응답 형식은 기존 프로젝트 공통 응답 래퍼 사용
- 메인 목록은 Deal 중심
- 문서 API는 Deal 문맥을 명시
- v2 통계는 v2 데이터만 대상으로 한다

---

## 9. API 목록

이 섹션은 2026-03-15 기준 **실제 구현된 v2 엔드포인트만** 기록한다.
정책상 필요하지만 아직 구현되지 않은 항목은 9.6 절에 별도 정리한다.

## 9.1 Deal API

### `GET /api/v2/deals`
Deal 목록 조회

#### Query Parameters
- `ownerEmpId`
- `clientId`
- `currentStage`
- `latestDocType`
- `isClosed`
- `keyword`
- `fromAt` (`ISO_LOCAL_DATE_TIME`)
- `toAt` (`ISO_LOCAL_DATE_TIME`)
- `page`
- `size`
- `sort`

#### Response item example
```json
{
  "dealId": 101,
  "dealCode": null,
  "dealTitle": null,
  "clientId": 5,
  "clientName": "OO농협",
  "ownerEmpId": 7,
  "ownerEmpName": "이하경",
  "snapshot": {
    "currentStage": "PENDING_CLIENT",
    "currentStatus": "FINAL_APPROVED",
    "representativeDocumentType": "QUO",
    "representativeDocumentId": 301,
    "lastActivityAt": "2026-03-15T10:00:00"
  },
  "closedAt": null
}
```

### `GET /api/v2/deals/kpis`
deal KPI 조회

### `GET /api/v2/deals/{dealId}`
deal 상세 조회

### `GET /api/v2/deals/{dealId}/documents`
deal 하위 전체 문서 조회

### `GET /api/v2/deals/{dealId}/notifications`
deal 알림 조회

#### Query Parameters
- `page`
- `size`

### `GET /api/v2/deals/{dealId}/schedules`
deal 일정 조회

#### Query Parameters
- `from` (`ISO_LOCAL_DATE_TIME`)
- `to` (`ISO_LOCAL_DATE_TIME`)

---

## 9.2 Quotation API

### `POST /api/v2/quotations`
신규 quotation 생성

#### Request
```json
{
  "requestId": 11,
  "dealId": 101,
  "clientId": 5,
  "memo": "봄 작기 제안",
  "items": [
    {
      "productId": 9001,
      "productName": "수박 모종",
      "productCategory": "WATERMELON",
      "quantity": 100,
      "unit": "BOX",
      "unitPrice": 12000
    }
  ]
}
```

### `POST /api/v2/quotations/{quotationId}/revise`
재작성 quotation 생성

#### Request
```json
{
  "memo": "클라이언트 피드백 반영",
  "items": [
    {
      "productId": 9001,
      "productName": "수박 모종",
      "productCategory": "WATERMELON",
      "quantity": 120,
      "unit": "BOX",
      "unitPrice": 11500
    }
  ]
}
```

### `PATCH /api/v2/quotations/{quotationId}/cancel`
quotation 취소

---

## 9.3 Contract API

### `POST /api/v2/contracts`
신규 contract 생성

#### Request
```json
{
  "quotationId": 301,
  "dealId": 101,
  "clientId": 5,
  "startDate": "2026-04-01",
  "endDate": "2026-08-31",
  "billingCycle": "MONTHLY",
  "specialTerms": "특약",
  "memo": "봄 작기 계약",
  "items": [
    {
      "productId": 9001,
      "productName": "수박 모종",
      "productCategory": "WATERMELON",
      "totalQuantity": 100,
      "unit": "BOX",
      "unitPrice": 12000
    }
  ]
}
```

### `POST /api/v2/contracts/{contractId}/revise`
재작성 contract 생성

#### Request
```json
{
  "startDate": "2026-04-01",
  "endDate": "2026-08-31",
  "billingCycle": "MONTHLY",
  "specialTerms": "특약",
  "memo": "반려 반영 재작성",
  "items": [
    {
      "productId": 9001,
      "productName": "수박 모종",
      "productCategory": "WATERMELON",
      "totalQuantity": 100,
      "unit": "BOX",
      "unitPrice": 12000
    }
  ]
}
```

### `PATCH /api/v2/contracts/{contractId}/cancel`
contract 취소

---

## 9.4 Billing Statistics API

### `GET /api/v2/statistics/billing/revenue/monthly`
v2 월별 청구 매출 조회

#### Query Parameters
- `from` (`ISO_DATE`)
- `to` (`ISO_DATE`)
- `category` (optional)

### `GET /api/v2/statistics/billing/revenue/by-category`
v2 품종별 청구 매출 조회

#### Query Parameters
- `from` (`ISO_DATE`)
- `to` (`ISO_DATE`)
- `category` (optional)

### `GET /api/v2/statistics/billing/revenue/monthly-by-category`
v2 월별/품종별 청구 매출 조회

#### Query Parameters
- `from` (`ISO_DATE`)
- `to` (`ISO_DATE`)
- `category` (optional)

---

## 9.5 공통 응답 DTO 초안

### `DealDocumentCommandResultDto`

```java
public class DealDocumentCommandResultDto {
    Long dealId;
    DealType documentType;
    Long documentId;
    String documentCode;
    RevisionInfoDto revisionInfo;
}
```

### `DealSummaryDto`

```java
public class DealSummaryDto {
    Long dealId;
    String dealCode;
    String dealTitle;
    Long clientId;
    String clientName;
    Long ownerEmpId;
    String ownerEmpName;
    DealSnapshotDto snapshot;
    LocalDateTime closedAt;
}
```

### `DealDetailDto`

```java
public class DealDetailDto {
    Long dealId;
    String dealCode;
    String dealTitle;
    Long clientId;
    String clientName;
    Long ownerEmpId;
    String ownerEmpName;
    String summaryMemo;
    DealSnapshotDto snapshot;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime closedAt;
}
```

---

## 9.6 미구현 계획 항목

아래 항목은 정책 문서에는 존재하지만, 현재 코드베이스에는 아직 `/api/v2/**` 구현이 없다.

- `POST /api/v2/deals`
- `POST /api/v2/deals/{dealId}/recalculate-snapshot`
- `POST /api/v2/deals/{dealId}/reopen`
- RFQ v2 전 구간
- quotation 상세/목록/승인 요청 v2
- contract 상세/목록/승인 요청 v2
- order v2 전 구간
- statement v2 전 구간
- invoice v2 전 구간
- payment v2 전 구간
- approval request v2 전 구간
- `GET /api/v2/statistics/revenue`

---

## 10. 주요 DTO 초안

이 절은 정책 설계 초안이다. 실제 구현 DTO와 이름/필드가 다를 수 있다.

## 10.1 DealSummaryResponse

```java
public record DealSummaryResponse(
    Long dealId,
    String dealCode,
    String dealTitle,
    Long clientId,
    String clientName,
    Long ownerEmployeeId,
    String ownerEmployeeName,
    DealStage currentStage,
    DealStatus currentStatus,
    DocumentType representativeDocumentType,
    Long representativeDocumentId,
    LocalDateTime lastActivityAt,
    boolean closed
) {}
```

## 10.2 DealDetailResponse

```java
public record DealDetailResponse(
    Long dealId,
    String dealCode,
    String dealTitle,
    SnapshotDto snapshot,
    RepresentativeDocumentsDto representativeDocuments
) {}
```

## 10.3 CreateQuotationRequest

```java
public record CreateQuotationRequest(
    Long dealId,
    Long baseRfqId,
    String title,
    String description,
    LocalDate respondDueDate
) {}
```

## 10.4 ReviseQuotationRequest

```java
public record ReviseQuotationRequest(
    String title,
    String description,
    LocalDate respondDueDate
) {}
```

## 10.5 CreateContractRequest

```java
public record CreateContractRequest(
    Long dealId,
    Long baseQuotationId,
    String title,
    String description,
    LocalDate contractStartDate,
    LocalDate contractEndDate
) {}
```

---

## 11. 권장 구현 순서

1. Deal / Quotation / Contract 도메인부터 v2 확정
2. snapshot 재계산 서비스 구현
3. approval 분리
4. `/api/v2/deals`, `/api/v2/quotations`, `/api/v2/contracts` 구현
5. order~payment 연결
6. 알림/일정 v2 전용 테이블 적용
7. 통계 v2 집계 적용

---

## 12. 보완 메모

- RFQ는 approval 없음
- Order는 Contract 없이는 생성 불가
- Payment 완료 기준은 `PaymentStatus.COMPLETED` 단일화
- 대표 문서 제약은 서비스 계층 강제를 우선 추천
- noti/sked는 v2 전용 테이블 분리 권장
- 통계는 v2 데이터만 사용
