# DealLog Architecture

이 문서는 SalesDealLog 기반 파이프라인 로그 구조를 설명한다.

--------------------------------------------------

## 1. Core Concepts

### SalesDeal

파이프라인의 현재 상태 스냅샷

주요 필드

- currentStage
- latestDocType
- latestRefId
- lastActivityAt

### SalesDealLog

파이프라인 이벤트 로그

주요 필드

- actionType
- actorType
- actorId
- fromStage
- toStage
- fromStatus
- toStatus
- docType
- refId
- targetCode

### DealLogDetail

필드 변경 사항 저장

diffJson schema

```json
{
  "fields": [
    {
      "field": "fieldName",
      "label": "displayName",
      "before": "value",
      "after": "value",
      "type": "TEXT|NUMBER|CURRENCY|DATE|STATUS|REFERENCE"
    }
  ]
}
```

--------------------------------------------------

## 2. Logging Rules

상태 변경

→ SalesDealLog 1개 생성

필드 변경

→ ActionType.UPDATE

Convert

→ 로그 2개 생성

CONVERT  
CREATE

--------------------------------------------------

## 3. Snapshot Synchronization

문서 상태 변경 시 반드시 실행

실제 구현은 DealPipelineFacade.recordAndSync(...) 내부에서 수행된다.

```java
SalesDeal.updateSnapshot(
    stage,
    currentStatus,
    docType,
    refId,
    targetCode,
    now
)
```

--------------------------------------------------

## 4. Actor Rules

ActorType → actorId

SALES_REP → employeeId  
ADMIN → employeeId  
CLIENT → clientId  
SYSTEM → null

--------------------------------------------------

## 5. Actor + Action Policy

(현재 코드 기준: DealLogPolicyValidator)

SALES_REP

CREATE  
UPDATE  
SUBMIT  
RESUBMIT  
CONVERT  
APPROVE  
REJECT  
CONFIRM  
ISSUE  
PAY  
EXPIRE  
CANCEL

ADMIN

CREATE  
UPDATE  
SUBMIT  
RESUBMIT  
CONVERT  
APPROVE  
REJECT  
CONFIRM  
ISSUE  
PAY  
EXPIRE  
CANCEL

CLIENT

CREATE  
UPDATE  
SUBMIT  
RESUBMIT  
APPROVE  
REJECT  
PAY  
CANCEL

SYSTEM

CREATE  
UPDATE  
SUBMIT  
RESUBMIT  
CONVERT  
CONFIRM  
ISSUE  
PAY  
EXPIRE  
CANCEL

--------------------------------------------------

## 6. Logging Entry Points

현재 로그를 생성하는 서비스 목록(직접 write 호출이 아닌 Facade 경유)

- QuotationRequestService
- QuotationService
- ContractService
- OrderService
- StatementService
- InvoiceService
- PaymentService
- ApprovalCommandService

공통 진입점

- DealPipelineFacade.recordAndSync(...)
- DealPipelineFacade.recordConvertAndSync(...)
- DealPipelineFacade.validateTransitionOrThrow(...)

--------------------------------------------------

## 7. Convert Flows

설계 목표

RFQ → QUOTATION

QUOTATION → CONTRACT

CONTRACT → ORDER

Convert는 반드시 다음 Facade 메서드를 사용한다.

DealPipelineFacade.recordConvertAndSync()

현재 상태

- Facade 내부에서 DealLogWriteService.writeConvertPair()를 호출
- 개별 도메인 서비스의 실사용 전환 연결은 후속 작업 대상

--------------------------------------------------

## 8. Transaction Policy

로그 기록은 상태 변경과 같은 트랜잭션에서 수행된다.

로그 생성 실패 시 전체 트랜잭션은 롤백된다.

즉 다음 상황은 절대 발생하면 안 된다.

State changed but log missing

--------------------------------------------------

## 9. Query / Response Policy

Timeline / Detail 조회 응답에는 `targetCode`가 누락되면 안 된다.

- Timeline DTO(`DealLogSummaryDto`)는 `targetCode` 포함
- DealLog 상세 DTO(`DealLogDetailDto`)도 `targetCode`, `diffJson` 포함

문서 상세 API는 Document Detail 구성 정책에 따라 최근 로그를 함께 제공한다.

- 조회 기준: `dealId + docType + refId`
- 정렬: `actionAt DESC, id DESC`
- 기본 건수: 최근 20건

적용 응답

- QuotationResponse.recentLogs
- ContractResponse.recentLogs
- OrderResponse.recentLogs
- StatementResponse.recentLogs
- InvoiceResponse.recentLogs
- InvoiceDetailResponse.recentLogs
- PaymentResponse.recentLogs

--------------------------------------------------

## 10. Test Policy

핵심 정책은 DB 없이 Mockito 단위 테스트로 검증한다.

- Convert pair: `writeConvertPair` 호출 시 로그 저장 2회(CONVERT + CREATE)
- Normal status change: `write` 호출 시 로그 저장 1회
- Actor/Action 정책: `SYSTEM + APPROVE` 는 `IllegalArgumentException`
- ActorId 정책: `SYSTEM actorId != null` 은 `IllegalArgumentException`
- 전이 실패 정책: `DocStatusTransitionValidator.validateOrThrow(...)` 예외 시 로그 저장 0회

--------------------------------------------------

## 11. Deal Null Guard Policy

문서 생성/상태변경 진입 서비스는 엔티티 정적 팩토리의 `Objects.requireNonNull(deal, ...)`에 의존하지 않고
서비스 계층에서 `deal` null을 먼저 검증한다.

적용 이유

- 런타임 `IllegalArgumentException`(500) 대신 도메인 표준 예외(`CoreException(ErrorType.DEAL_NOT_FOUND)`)를 일관 반환
- 스케줄러 경로에서 silent return을 금지해 운영 실패 감지 가능성 확보

적용 경로

- `InvoiceService.createDraftInvoice` : deal 누락 시 즉시 예외 throw (silent drop 제거)
- `PaymentService.processPayment` : `invoice.getDeal()` null 가드
- `StatementService.createStatement` : `orderHeader.getDeal()` null 가드
- `OrderService.createOrder` : `contract.getDeal()` null 가드

--------------------------------------------------

## 12. Legacy Root Architecture Migration

source: removed root `architecture.md`

- Project Stack
  - Spring Boot 3 / Spring Data JPA (Hibernate 6) / MariaDB / QueryDSL 5
- Package / Layering
  - base package: `com.monsoon.seedflowplus`
  - DDD shape: `domain/{domain}/{controller,service,repository,entity,dto}`
- Entity Base Policy
  - `BaseEntity -> BaseCreateEntity -> BaseModifyEntity`
  - `@AttributeOverride(name="id", column=@Column(name="{domain}_id"))`
  - `@SQLDelete + @SQLRestriction("is_deleted = false")`
  - explicit mutation methods (`updateXxx`) and factory/builder-based creation
- Transaction / Auth
  - class-level `@Transactional(readOnly = true)`, mutating method override
  - JWT + `CustomUserDetails(userId, loginId, role, employeeId, clientId)`

--------------------------------------------------

## 13. Documentation Policy (Refactoring Folder)

작업 이력과 구조 문서는 루트 문서가 아닌 `docs/refactoring/*.md`를 단일 소스로 사용한다.

- 작업 로그: `docs/refactoring/deal-log-work-log.md`
- 구조/정책: `docs/refactoring/deal-log-architecture.md`
- 신규 작업 시 위 2개 문서에 누적 기록하고, 루트 레벨 중복 문서는 생성하지 않는다.

--------------------------------------------------

## 14. Re-Verification Note

deal null guard 관련 4개 필수 경로는 현재 서비스 계층 선검증 정책을 충족한다.

- Invoice scheduler draft path: `CoreException(ErrorType.DEAL_NOT_FOUND)`로 실패 표준화
- Payment create path: `invoice.getDeal()` null guard
- Statement create path: `orderHeader.getDeal()` null guard
- Order create path: `contract.getDeal()` null guard
