# DealLog Refactoring Work Log

이 문서는 SalesDealLog 리팩토링 작업의 히스토리를 기록한다.

목적

- AI 세션 간 컨텍스트 공유
- 리팩토링 진행 상황 추적
- 코드 리뷰 참고
- 장애 발생 시 원인 분석

## 2026-03-05 17:51

### Step

Step15 timeline/order + statement retry determinism hardening

### Purpose

리뷰 지적 사항 기준으로 work-log 시간 정렬 규칙을 일관화하고,
명세서 생성 재시도/연결 invoice 조회/Facade 입력 검증의 비결정성 및 후행 예외 위험을 제거한다.

### Modified Files

- deal-log-work-log.md
- deal-log-architecture.md
- InvoiceStatementRepository.java
- StatementService.java
- DealPipelineFacade.java

### Key Changes

- `deal-log-work-log.md`의 타임라인을 전체 시간 내림차순(최신 우선)으로 재정렬
- `InvoiceStatementRepository`에 `findTopByStatementIdAndIncludedTrueOrderByIdDesc(...)` 추가
- `StatementService.resolveInvoiceId(...)`가 결정적 최신 행을 선택하도록 변경
- `StatementService.createStatement(...)`의 저장 재시도를 `PROPAGATION_REQUIRES_NEW` 트랜잭션 템플릿으로 분리
- `DealPipelineFacade.recordAndSync(...)` 시작부에 핵심 인자 fail-fast null 체크 추가

### Validation

- `./gradlew compileJava -q` 통과
- `./gradlew test --tests "*DealPipelineFacadeTest" --tests "*DealLogPolicyValidatorTest" -q` 통과

## 2026-03-05 17:30

### Step

Step14 Inline findings hardening (null-safe DTO / actorId / error taxonomy)

### Purpose

인라인 지적 사항 기준으로 DTO null 안정성, actorId 인증 강제, convert 경로 fail-fast, 예외 분류 체계를 코드/테스트에서 일치시킨다.

### Modified Files

- StatementResponse.java
- StatementService.java
- DealPipelineFacade.java
- DealErrorType.java
- DealErrorCode.java
- DealLogPolicyValidatorTest.java
- deal-log-architecture.md
- deal-log-work-log.md

### Key Changes

- `StatementResponse.from(...)`의 `recentLogs`를 null-safe 빈 리스트로 정규화
- `StatementService.resolveActorId(...)`에서 non-SYSTEM actorId null 시 `UNAUTHORIZED` throw
- `DealPipelineFacade.recordConvertAndSync(...)` 진입부 null 체크(`Objects.requireNonNull`) 추가
- `DealErrorType`에 `SYSTEM_ERROR` 추가 및 `DIFF_JSON_SERIALIZATION_FAILED` 분류 전환
- `DealLogPolicyValidatorTest`에서 모든 `DealException` 케이스에 기대 `DealErrorCode` 단언 추가

### Validation

- `./gradlew compileJava -q` 통과
- `./gradlew test --tests "*DealLogPolicyValidatorTest" --tests "*DealPipelineFacadeTest" -q` 통과

## 2026-03-05 16:47

### Step

Step13 Unified fix prompt follow-up (recentLogs/read-permission)

### Purpose

recentLogs strict 검증 회귀를 제거하고, invoice 상세 조회에서 principal 기반 권한 검증을 적용해
로그 메타데이터 노출 범위를 사용자 소유 범위로 제한한다.

### Modified Files

- DealLogQueryService.java
- InvoiceController.java
- InvoiceService.java
- StatementService.java
- deal-log-architecture.md
- deal-log-work-log.md

### Key Changes

- `DealLogQueryService.getRecentDocumentLogs(...)`:
  - null 입력(`dealId/docType/refId`) 시 `List.of()` 반환 (null-tolerant)
  - strict 검증은 `getRecentDocumentLogsStrict(...)`로 분리
- `InvoiceController`:
  - `GET /api/v1/invoices/{invoiceId}`
  - `GET /api/v1/invoices/{invoiceId}/detail`
  - 두 엔드포인트에 `@AuthenticationPrincipal` 주입
- `InvoiceService`:
  - `validateInvoiceReadPermission(...)` 추가
  - ADMIN 허용, CLIENT는 본인 `clientId` 소유만 허용
  - SALES_REP/ADMIN 계열은 `invoice.employee` 또는 `deal.ownerEmp` 일치 시 허용
- `StatementService.createStatement(...)`:
  - CREATE 로그 diff에 `isInitialCreate=true` 추가 (from/to 동일 상태 해석 보완)

### Validation

- `./gradlew compileJava -q` 통과
- `./gradlew test --tests "*DealPipelineFacadeTest" --tests "*DealLogPolicyValidatorTest" -q` 통과

## 2026-03-05 16:35

### Step

Step12 Outside diff findings hardening

### Purpose

outside diff 지적 사항을 현재 코드 기준으로 재검증하고,
실제 누락된 매핑/예외 표준화/인덱스/테스트 회귀 범위만 최소 수정으로 반영한다.

### Modified Files

- StatementResponse.java
- StatementService.java
- DealDiffField.java
- DealPipelineFacade.java
- QuotationRequestService.java
- SalesDealLog.java
- DealErrorType.java
- DealErrorCode.java
- DealLogPolicyValidator.java
- DealLogPolicyValidatorTest.java
- DealPipelineFacadeTest.java
- deal-log-architecture.md
- deal-log-work-log.md

### Key Changes

- `StatementResponse`에 `from(statement, invoiceId, recentLogs)` 오버로드를 추가하고 `invoiceId` 빌더 매핑 보강
- `StatementService`가 `InvoiceStatementRepository.findAllByStatementIdAndIncludedTrue(...)`로 invoiceId를 조회해 응답에 주입
- `DealDiffField` 공개 DTO 추가 및 `DealPipelineFacade` 오버로드를 통한 내부 `DiffField` 변환 적용
- `QuotationRequestService`의 `DealLogWriteService.DiffField` 직접 참조를 `DealDiffField`로 교체
- `DealLogPolicyValidator`의 잔여 `IllegalArgumentException` 제거, `DealException(DealErrorCode)`로 통일
- `DealErrorCode`에 `INVALID_ACTOR_ACTION_COMBINATION`, `TARGET_CODE_REQUIRED` 추가
- `SalesDealLog`에 `(deal_id, doc_type, ref_id, action_at, deal_log_id)` 복합 인덱스 추가
- `DealLogPolicyValidatorTest`를 ActorType×ActionType 전수 조합 기반으로 확장
- `DealPipelineFacadeTest`에 `ErrorType.INVALID_DOC_STATUS_TRANSITION` 단언 추가
- `deal-log-work-log.md`의 14:05/14:30/15:10/16:05/16:20 섹션을 일관된 시간 내림차순으로 재정렬

### Validation

- `./gradlew test --tests "*DealLogPolicyValidatorTest" --tests "*DealPipelineFacadeTest" -q`
- `./gradlew compileJava -q`

## 2026-03-05 16:21

### Step

Step10 Deal-log policy/input hardening and docs sync

### Purpose

리뷰 지적 사항 기준으로 문서 스키마와 실제 구현을 일치시키고,
인증 주체 누락/actorId 누락/전이 검증 누락/nullable 연관 NPE 경로를 표준 예외로 조기 차단한다.

### Modified Files

- PRE_RABBIT.md
- docs/refactoring/deal-log-architecture.md
- InvoiceController.java
- InvoiceService.java
- Payment.java
- PaymentService.java
- StatementService.java
- DealErrorCode.java
- DealLogPolicyValidator.java
- DealLogQueryService.java
- DealLogWriteService.java
- DealPipelineFacade.java
- OrderController.java
- OrderService.java
- pre-rabbit.md (deleted)

### Key Changes

- `deal-log-architecture.md` diffJson `fields[].type`에 `MONEY|ENUM|COUNT|BOOLEAN` 추가 및 예시/설명 동기화
- `PRE_RABBIT.md`의 bare code fence를 `text` 지정 fence로 치환(MD040 대응)
- `InvoiceController.publish/toggle`, `OrderController.confirm`에서 principal null/식별자 검증 추가
- `InvoiceService.resolveActorId`, `OrderService.resolveActorId`에서 non-SYSTEM actorId null 시 `UNAUTHORIZED` 강제
- `StatementService.cancelStatement`에 `statement.getDeal()` null 가드 추가(`DEAL_NOT_FOUND`)
- `DealLogPolicyValidator`
  - staff 권한을 `EnumSet.of(...)` 명시 화이트리스트로 전환
  - non-SYSTEM actorId를 `null 또는 0 이하` 금지로 강화
  - `IllegalArgumentException` 대신 `DealException(DealErrorCode)` 사용
- `DealLogQueryService`
  - `getRecentDocumentLogs(...)` null 입력 시 빈 리스트 반환 제거, `INVALID_INPUT_VALUE` 예외로 fail-fast
  - `getLogDetail(...)`에서 ownerEmpId 계산 시 null-safe 가드 추가
- `DealLogWriteService.buildDiffJson(...)` 직렬화 실패를 `DealException(DIFF_JSON_SERIALIZATION_FAILED)`로 표준화
- `DealPipelineFacade.shouldValidateTransition(...)`에서 `UPDATE`도 전이 검증 수행하도록 조정(`CREATE`만 예외)
- `Payment` 생성 상태를 `PENDING`으로 맞추고 `processPayment(...)`에서 `payment.complete()` 후 `PENDING→COMPLETED` 로그 일관화

### Validation

- `./gradlew compileJava -q` 통과

## 2026-03-05 16:20

### Step

Step11 Deal Null Guard Fix Request Re-check

### Purpose

동일 이슈 재요청에 대해 대상 4개 파일의 반영 상태를 재점검하고,
추가 수정 필요 여부를 확정한다.

### Modified Files

- deal-log-work-log.md
- deal-log-architecture.md

### Key Changes

- 코드 재검증 결과 4개 이슈 모두 이미 반영되어 애플리케이션 코드 추가 변경 없음
- 확인 라인:
  - `InvoiceService.createDraftInvoice` (`DEAL_NOT_FOUND` throw)
  - `PaymentService.processPayment` (`invoice.getDeal()` null guard)
  - `StatementService.createStatement` (`orderHeader.getDeal()` null guard)
  - `OrderService.createOrder` (`contract.getDeal()` null guard)

### Validation

- 라인 단위 수동 검증 완료 (재컴파일 불필요 범위)

### Legacy Root Log Migration

- source: removed root `work-log.md`
- migrated summary:
  - Billing/Order 경로 deal null 처리 이슈 4건 수정
  - `InvoiceService.createDraftInvoice`: `warn + return` 제거, `CoreException(ErrorType.DEAL_NOT_FOUND)`로 표준화
  - `PaymentService.processPayment`: `invoice.deal` null 선검증 추가
- `StatementService.createStatement`: `orderHeader.deal` null 선검증 추가
- `OrderService.createOrder`: `contract.deal` null 선검증 추가

## 2026-03-05 16:05

### Step

Step10 Deal Null Guard Re-Verification

### Purpose

요청된 4개 이슈(Invoice Scheduler silent drop, Payment/Statement/Order deal null guard)를
재검증하여 코드 반영 상태를 확인하고 문서 이력을 `docs/refactoring` 기준으로 일원화한다.

### Modified Files

- deal-log-work-log.md
- deal-log-architecture.md

### Key Changes

- 소스 코드 재검증 결과:
  - `InvoiceService.createDraftInvoice`: `contract.getDeal() == null` 시 `CoreException(ErrorType.DEAL_NOT_FOUND, "contractId=...")` throw
  - `PaymentService.processPayment`: `invoice.getDeal()` null 선검증 후 `CoreException(ErrorType.DEAL_NOT_FOUND)` 처리
  - `StatementService.createStatement`: 시작부 `orderHeader.getDeal()` null 가드 처리
  - `OrderService.createOrder`: 계약 기간 검증 직후 `contract.getDeal()` null 가드 처리
- 이번 턴에서는 대상 이슈 관련 애플리케이션 코드 추가 변경 없음(이미 적용 상태 확인)

### Validation

- 라인 단위 재확인:
  - `InvoiceService.java:323`
  - `PaymentService.java:67`
  - `StatementService.java:43`
  - `OrderService.java:69`

## 2026-03-05 15:10

### Step

Step9 Deal Null Guard Hardening

### Purpose

deal 연관이 비정상적으로 null인 레거시/이상 데이터 경로에서 런타임 500(`IllegalArgumentException`) 또는 silent drop이 발생하지 않도록,
서비스 진입부에서 `CoreException(ErrorType.DEAL_NOT_FOUND)`로 실패를 표준화한다.

### Modified Files

- InvoiceService.java
- PaymentService.java
- StatementService.java
- OrderService.java
- deal-log-architecture.md

### Key Changes

- `InvoiceService.createDraftInvoice(...)`
  - `contract.getDeal() == null` 시 `warn + return` 제거
  - `CoreException(ErrorType.DEAL_NOT_FOUND, "contractId=...")` 즉시 throw로 변경
  - 스케줄러(`InvoiceScheduler`)의 기존 `try-catch`에 의해 실패 로그가 운영에서 명시적으로 관찰되도록 조정
- `PaymentService.processPayment(...)`
  - `invoice.getDeal()` null 선검증 추가 후 `ErrorType.DEAL_NOT_FOUND` throw
  - `Payment.create(...)` 내부 `Objects.requireNonNull`에 의한 비표준 예외 노출 차단
- `StatementService.createStatement(...)`
  - 메서드 시작부에 `orderHeader.getDeal()` null 가드 추가
  - `Statement.create(...)` 내부 `Objects.requireNonNull` 런타임 예외를 서비스 계층 표준 예외로 변환
- `OrderService.createOrder(...)`
  - 계약 기간 검증 직후 `contract.getDeal()` null 가드 추가
  - `OrderHeader.create(...)` 내부 `Objects.requireNonNull` 예외를 `CoreException`으로 표준화

### Validation

- `./gradlew compileJava -q` 통과

## 2026-03-05 14:30

### Step

Step8 Mockito 단위 테스트 추가

### Purpose

DB 없이 DealLog 핵심 정책(로그 저장 건수, actor/action 정책, 전이 실패 시 미저장)을
Mockito 기반 유닛 테스트로 회귀 검증한다.

### Modified Files

- DealLogWriteServiceTest.java
- DealPipelineFacadeTest.java
- DealLogPolicyValidatorTest.java
- deal-log-architecture.md

### Key Changes

- `DealLogWriteServiceTest`
  - `writeConvertPair` 호출 시 `SalesDealLogRepository.save` 2회(CONVERT + CREATE) 검증
  - 일반 `write` 호출 시 `save` 1회 검증
  - `ArgumentCaptor`로 저장 로그의 `actionType`, `targetCode` 검증
- `DealLogPolicyValidatorTest`
  - `SYSTEM + APPROVE` 조합이 `IllegalArgumentException`을 던지는 명시 케이스 추가
  - `SYSTEM actorId != null` 예외 케이스에 정책 주석 보강
- `DealPipelineFacadeTest`
  - `DocStatusTransitionValidator.validateOrThrow(...)` 예외 발생 시
    `SalesDealLogRepository.save` 0회 검증

### Validation

- `./gradlew test --tests "*DealLogWriteServiceTest" --tests "*DealPipelineFacadeTest" --tests "*DealLogPolicyValidatorTest" -q` 통과

## 2026-03-05 14:05

### Step

Step7 조회 응답 보강 (targetCode / recentLogs)

### Purpose

타임라인/상세 조회에서 정책 필드 누락을 방지하고,
문서 상세 API 응답에 해당 문서의 최근 Deal 로그를 함께 포함한다.

### Modified Files

- SalesDealLogRepository.java
- DealLogQueryService.java
- DealLogDetailDto.java
- QuotationResponse.java
- ContractResponse.java
- OrderResponse.java
- StatementResponse.java
- InvoiceResponse.java
- InvoiceDetailResponse.java
- PaymentResponse.java
- QuotationService.java
- ContractService.java
- OrderService.java
- StatementService.java
- InvoiceService.java
- PaymentService.java
- deal-log-architecture.md

### Key Changes

- `DealLogDetailDto`에 `targetCode` 필드 추가, 상세 조회에서 `targetCode + diffJson` 제공
- DealLog 상세 조회를 `SalesDealLog` 기준으로 조회하도록 보정해 `DealLogDetail` 미존재 시에도 응답 가능(diff/reason null)
- `SalesDealLogRepository.findRecentByDealIdAndDocTypeAndRefId(...)` 추가 (최근순 정렬)
- `DealLogQueryService.getRecentDocumentLogs(...)` 추가 (기본 20개)
- 문서 상세 응답 DTO에 `recentLogs` 필드 추가:
  - QuotationResponse
  - ContractResponse
  - OrderResponse
  - StatementResponse
  - InvoiceResponse
  - InvoiceDetailResponse
  - PaymentResponse
- 각 문서 상세 서비스에서 `dealId + docType + refId` 기준 최근 로그를 응답에 포함

### Validation

- `./gradlew compileJava -q` 통과

## 2026-03-05 13:25

### Step

Step6 DealPipelineFacade 도입

### Purpose

상태 변경 유스케이스에서 validator + log write + SalesDeal snapshot sync를 하나의 진입점으로 묶어
"로그만 기록" 또는 "스냅샷만 갱신"되는 분리 사고를 방지한다.

### Modified Files

- DealPipelineFacade.java
- DealLogWriteService.java
- QuotationRequestService.java
- QuotationService.java
- ContractService.java
- OrderService.java
- StatementService.java
- InvoiceService.java
- PaymentService.java
- ApprovalDealLogWriter.java
- deal-log-architecture.md

### Key Changes

- `DealPipelineFacade.recordAndSync(...)` 추가: 상태 전이 검증 + 로그 기록 + `SalesDeal.updateSnapshot(...)` 일괄 수행
- `DealPipelineFacade.recordConvertAndSync(...)` 추가: CONVERT/CREATE pair 기록 후 snapshot 1회 동기화
- `DealPipelineFacade.validateTransitionOrThrow(...)` 추가: 상태 변경 전 선검증 진입점 통일
- 도메인 서비스들이 `DealLogWriteService`/`DocStatusTransitionValidator`를 직접 호출하지 않고 Facade를 사용하도록 치환
- `DealLogWriteService`는 로그/상세 기록 전담으로 축소 (snapshot 동기화 책임 제거)

### Validation

- `./gradlew compileJava -q` 통과

## 2026-03-05 12:39

### Step

Step1 Policy Validator  
Step2 DealLogWriteService Refactor  
Step3 Approval Pipeline Fix  
Step4 Document Create Logging  
Step5 Status Change Logging

### Purpose

DealLog 정책 검증을 중앙화하고, 문서 생성/상태 변경 경로 전체에서 로그 + 스냅샷 동기화를 같은 트랜잭션으로 강제한다.

### Modified Files

- DealLogPolicyValidator.java
- DealLogPolicyValidatorTest.java
- ActionType.java
- DealLogWriteService.java
- ApprovalCommandService.java
- ApprovalDealLogWriter.java
- QuotationRequestService.java
- QuotationService.java
- ContractService.java
- OrderService.java
- OrderController.java
- StatementService.java
- StatementController.java
- InvoiceService.java
- InvoiceController.java
- PaymentService.java

### Key Changes

- ActorType + ActionType 조합, targetCode, SYSTEM actorId 규칙을 DealLogPolicyValidator로 중앙 검증
- DealLogWriteService를 write/writeConvertPair 중심으로 정리하고, diff fields 직렬화 및 fields=[] 상세 미생성 정책 적용
- write 내부에서 SalesDeal.updateSnapshot 동기화 수행하도록 통합
- Approval 경로에서 상태 변경 전 전이 검증 호출 및 actorId 매핑(ADMIN/SALES_REP=employeeId, CLIENT=clientId) 보강
- RFQ/QUO/CNT 생성 트랜잭션에 CREATE 로그 연결
- ORDER(confirm/cancel), STATEMENT(create/cancel), INVOICE(publish/toggle), PAYMENT(paid)에 전이 검증 + 로그 기록 추가

### Snapshot Sync

Applied

- DealLogWriteService.write(...) 내부 syncSnapshot
- ApprovalDealLogWriter 경유 write 호출 경로
- QuotationRequestService.createQuotationRequest
- QuotationService.createQuotation
- ContractService.createContract
- OrderService.confirmOrder
- OrderService.cancelOrder
- StatementService.createStatement
- StatementService.cancelStatement
- InvoiceService.publishInvoice
- InvoiceService.toggleStatement
- PaymentService.processPayment

Not Applied

- InvoiceService.createInvoice / createDraftInvoice 생성 로그 연결(이번 단계 범위 외)
- Convert 실사용 도메인 연결(writeConvertPair 호출 실제 흐름)

### Logging Coverage

RFQ  
Quotation  
Contract  
Order  
Statement  
Invoice  
Payment  
Approval

### Remaining Tasks

- Convert 실제 전환 시점(RFQ→QUO, QUO→CNT, CNT→ORD)에서 writeConvertPair 강제 연결
- INVOICE create/createDraft 경로 로그 정책 확정 후 반영
- 도메인별 로그 내용(diff fields) 표준화 및 통합 테스트 보강

### Risk / Notes

- Actor+Action 정책은 현재 서비스 사용 케이스를 수용하도록 확장되어 있으며, 추후 권한 모델 확정 시 재조정 필요
- Statement cancel API를 신규 추가했으므로 API 문서/테스트 케이스 동기화 필요
- DealLogWriteService.validateRequired가 fromStatus/toStatus를 필수로 요구하므로 신규 경로 연결 시 null/blank 주의
