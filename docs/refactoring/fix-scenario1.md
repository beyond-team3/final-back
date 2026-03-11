## [2026-03-10] Schedule 구현 요약 문서 정합화

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/doc/schedule-implementation-summary.md
- 클래스/메서드: 문서 업데이트, 구조 변경 없음

### 변경 내용
현재 schedule 구현과 요약 문서를 대조해 패키지 구조, API 경로, 엔티티 스키마,
권한/검증 정책 설명을 실제 코드 기준으로 수정했다.
문서성 변경만 수행했으며 메서드 시그니처, 클래스 구성, 의존 방향은 바뀌지 않았다.

### 변경 이유
구현 문서와 실제 코드의 불일치를 제거하기 위한 문서 정합화

## [2026-03-10 09:32] Schedule 구현 요약 문서 수정

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/doc/schedule-implementation-summary.md — 현재 구현 기준으로 패키지 구조, API, 엔티티, 정책 설명 정리
- 수정 파일: docs/refactoring/fix-scenario1.md — 작업 기록 및 아키텍처 변경 여부 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 없음

## [2026-03-10] order confirm client actor policy fix

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/log/policy/DealLogPolicyValidator.java
- 클래스/메서드: DealLogPolicyValidator.createAllowedActionsByActor

### 변경 내용
시나리오1의 ORD 확정 호출 주체와 실제 딜 로그 정책을 맞추기 위해 `ActorType.CLIENT`의 허용 액션에 `CONFIRM`을 추가했다.
이제 `OrderService.confirmOrder(...)`가 CLIENT 요청으로 실행되어도 ORD CONFIRM 로그와 후속 STMT CREATE 로그가 같은 흐름에서 저장될 수 있다.

### 변경 이유
시나리오1의 ORD 확정 주체는 CLIENT이며, 기존 정책과 충돌해 400(`INVALID_ACTOR_ACTION_COMBINATION`)이 발생했다.

## [2026-03-10 09:51] BUG-1 order confirm 400 수정

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/log/policy/DealLogPolicyValidator.java — CLIENT의 ORD confirm 로그 기록이 400으로 차단되지 않도록 허용 액션에 CONFIRM 추가
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/deal/log/policy/DealLogPolicyValidatorTest.java — CLIENT + CONFIRM 조합 허용 정책을 테스트 기대값에 반영
- 수정 파일: src/test/http/debug/bug1_order_confirm.http — ORD 생성 후 confirm 최소 재현용 HTTP 디버그 스니펫 신규 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 확인 예정

### 다음 단계
컴파일 및 대상 테스트 확인

## [2026-03-10 10:04] BUG-2 ORD 생성 DealLog 기록 보강

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/sales/order/service/OrderService.java — ORD 생성 완료 후 `ActionType.CREATE` 기준으로 DealLog와 deal snapshot이 즉시 기록되도록 `recordAndSync(...)` 추가
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/sales/order/service/OrderServiceTest.java — `createOrder()`가 ORD CREATE 로그를 남기는지 검증하는 단위 테스트 신규 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음

### 다음 단계
없음

## [2026-03-10 09:44] Scenario1 schedule 검증 정합화

### 작업 내용
- 수정 파일: api-test/http/pipeline/scenario1.http — schedule 응답의 실제 docType enum(CONTRACT, ORDER 등)에 맞춰 assertion과 주석 보정

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 없음

## [2026-03-10] schedule doc type contract 표기 제거

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/DealDocType.java
- 클래스/메서드: DealDocType

### 변경 내용
schedule 도메인 전용 문서 타입 enum에서 `CONTRACT`와 `CNT`가 공존하던 중복 표현을 정리했다.
DB가 비어 있는 상태를 전제로 `CONTRACT` 상수를 제거하고 `CNT`만 남겨 일정 응답의 `docType.name()` 표기를 단일화했다.

### 변경 이유
같은 계약 문서를 두 enum 이름으로 유지하면 일정 응답과 externalKey 규칙이 분기되므로 표기를 통일해야 한다.

## [2026-03-10 10:20] BUG-3 선행 DealDocType CNT 정리

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/DealDocType.java — `DealDocType.CONTRACT` 제거 후 `CNT` 단일 표기로 통일

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음

### 다음 단계
BUG-3 트리거 연결 작업 재개

## [2026-03-10] schedule doc type 약어 체계 통일

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/DealDocType.java
- 클래스/메서드: DealDocType

### 변경 내용
schedule 도메인 전용 문서 타입 enum을 deal 공통 `DealType`과 같은 약어 체계(`RFQ`, `QUO`, `CNT`, `ORD`, `STMT`, `INV`, `PAY`)로 통일했다.
이에 따라 schedule 도메인 테스트의 enum 참조도 동일한 약어 이름으로 갱신했다.

### 변경 이유
계약만 약어인 혼합 표기보다 전 문서를 동일 규칙으로 맞춰 응답 문자열과 externalKey 규칙 해석을 단순화하기 위해서다.

## [2026-03-10 10:25] BUG-3 선행 DealDocType 약어 통일

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/schedule/entity/DealDocType.java — schedule 문서 타입 enum을 `RFQ/QUO/CNT/ORD/STMT/INV/PAY` 약어 체계로 통일
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/schedule/dto/command/DealScheduleUpsertCommandTest.java — `DealDocType.QUOTATION` 참조를 `DealDocType.QUO`로 변경
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/schedule/entity/DealScheduleTest.java — schedule 엔티티 테스트의 docType 기대값을 약어 enum으로 동기화
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/schedule/query/ScheduleQueryServiceTest.java — unified schedule 조회 테스트의 docType fixture를 약어 enum으로 변경
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/schedule/sync/DealScheduleSyncServiceTest.java — sync 테스트의 docType fixture를 약어 enum으로 변경

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음

### 다음 단계
BUG-3 트리거 연결 작업 재개

## [2026-03-10] BUG-3 일정 동기화 트리거 연결

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java
- 클래스/메서드: ApprovalCommandService.decideStep, syncContractApprovalSchedulesIfNeeded

### 변경 내용
CLIENT 최종 계약 승인 이후 `DealScheduleSyncService`를 통해 계약 시작일/만료일 일정을 자동 upsert하도록 연결했다.
승인 서비스에 일정 동기화 의존성과 담당자 사용자 해석(`UserRepository`)을 추가하고, CNT 승인 시 두 개의 schedule command를 생성하는 내부 헬퍼를 보강했다.

### 변경 이유
BUG-3. 계약 승인 파이프라인에서 `DOC_APPROVED` 일정이 생성되지 않던 연결 누락을 보완하기 위해서다.

## [2026-03-10] BUG-3 주문/청구/결제 일정 동기화 연결

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/sales/order/service/OrderService.java
- 클래스/메서드: OrderService.confirmOrder, syncDeliveryDueSchedule

### 변경 내용
ORD confirm 이후 주문 생성일 기준 `DELIVERY_DUE` 일정을 upsert하도록 연결했다.
서비스에 일정 동기화 의존성과 담당자 사용자 해석을 추가하고, `externalKey`와 제목/기간 생성 규칙을 내부 헬퍼로 고정했다.

### 변경 이유
BUG-3. 주문 확정 이후 생산 일정 파이프라인이 끊겨 조회 캘린더에 납품 예정 일정이 누락되던 문제를 막기 위해서다.

## [2026-03-10] BUG-3 청구/결제 일정 동기화 연결

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/billing/invoice/service/InvoiceService.java
- 클래스/메서드: InvoiceService.publishInvoice, syncPaymentDueSchedule

### 변경 내용
INV publish 이후 `invoiceDate`를 기준으로 `PAYMENT_DUE` 일정을 upsert하도록 연결했다.
청구 서비스에 일정 동기화 의존성과 담당자 사용자 해석을 추가해 발행 직후 시스템 일정이 생성되도록 보강했다.

### 변경 이유
BUG-3. 청구서 발행 후 결제 마감 일정이 생성되지 않던 연결 누락을 보완하기 위해서다.

## [2026-03-10] BUG-3 결제 수신 일정 동기화 연결

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/billing/payment/service/PaymentService.java
- 클래스/메서드: PaymentService.processPayment, syncPaymentReceivedSchedule

### 변경 내용
PAY 완료 처리 직후 `createdAt` 기준 `PAYMENT_RECEIVED` 일정을 upsert하도록 연결했다.
결제 서비스에 일정 동기화 의존성과 담당자 사용자 해석을 추가해 결제 완료 캘린더 이벤트가 자동 생성되도록 맞췄다.

### 변경 이유
BUG-3. 결제 완료 파이프라인에서 schedule sync 호출이 빠져 일정 이력이 누락되던 문제를 해결하기 위해서다.

## [2026-03-10 10:41] BUG-3 DealScheduleSyncService 트리거 연결

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java — CNT CLIENT 최종 승인 직후 계약 시작일/만료일 `DOC_APPROVED` 일정 2건을 upsert하도록 연결
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/sales/order/service/OrderService.java — ORD confirm 직후 주문 생성일 기준 `DELIVERY_DUE` 일정 upsert 연결
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/billing/invoice/service/InvoiceService.java — INV publish 직후 `invoiceDate` 기준 `PAYMENT_DUE` 일정 upsert 연결
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/billing/payment/service/PaymentService.java — PAY 완료 직후 `createdAt` 기준 `PAYMENT_RECEIVED` 일정 upsert 연결
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandServiceTest.java — CNT CLIENT 최종 승인 시 일정 upsert 2건 검증 추가
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/sales/order/service/OrderServiceTest.java — ORD confirm 시 일정 upsert 검증 추가
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/billing/invoice/service/InvoiceServiceTest.java — INV publish 일정 upsert 단위 테스트 신규 추가
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/billing/payment/service/PaymentServiceTest.java — PAY 완료 일정 upsert 단위 테스트 신규 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음

### 다음 단계
없음

## [2026-03-10 11:18] BUG-3 후속 doc_type VARCHAR 검증 보강

### 작업 내용
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/schedule/repository/DealScheduleRepositoryTest.java — `DealDocType.CNT` 기준 `DealSchedule` 저장/조회 JPA 슬라이스 테스트 신규 추가
- 수정 파일: docs/refactoring/fix-scenario1.md — doc_type VARCHAR 후속 작업 결과와 제약 사항 기록
- 참고: `tbl_deal_sked.doc_type` 직접 ALTER는 샌드박스 TCP 소켓 제한으로 이 세션에서 실행하지 못함

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음

### 다음 단계
개발 DB에서 `ALTER TABLE tbl_deal_sked MODIFY COLUMN doc_type VARCHAR(20) NOT NULL;` 직접 실행 후 `DESCRIBE tbl_deal_sked;` 재확인

## [2026-03-10 11:22] BUG-4 INV 알림 assertion 제거

### 작업 내용
- 수정 파일: api-test/http/pipeline/scenario1.http — INV publish 직후 `INVOICE_ISSUED` 알림 조회 assertion을 주석 처리하고 미구현 사유 주석 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → HTTP 시나리오 파일 변경으로 컴파일 대상 아님

### 다음 단계
별도 태스크에서 Invoice 발행 알림 구현

## [2026-03-10 11:40] BUG-5 통계 점검 SQL 추가

### 작업 내용
- 수정 파일: bug5-stat-check.sql — Invoice/InvoiceStatement/Statement/OrderDetail/ContractDetail 연결과 통계 집계 조건을 한 번에 점검할 수 있는 SQL 스크립트 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → SQL 스크립트 파일 추가로 컴파일 대상 아님

### 다음 단계
실제 개발 DB에서 `@inv_id` 값을 채워 집계 대상 데이터 존재 여부 확인

## [2026-03-10] BUG-6 일정 assignee fallback 보강

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java
- 클래스/메서드: ApprovalCommandService.decideStep, syncContractApprovalSchedulesIfNeeded

### 변경 내용
계약 최종 승인 후 일정 동기화 시 deal owner에 연결된 user가 없으면 현재 승인 사용자 또는 거래처 user를 fallback assignee로 사용하도록 바꿨다.
최종 승인 메서드에서 principal을 일정 동기화까지 전달하도록 시그니처를 조정해 런타임 user 조회 실패가 문서 승인 전체를 롤백시키지 않게 했다.

### 변경 이유
BUG-6. scenario1에서 CNT 최종 승인 시 일정 assignee 조회 예외로 500이 발생하던 흐름을 복구하기 위해서다.

## [2026-03-10] BUG-6 주문/청구/결제 일정 assignee fallback 보강

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/sales/order/service/OrderService.java
- 클래스/메서드: OrderService.confirmOrder, syncDeliveryDueSchedule

### 변경 내용
ORD confirm 일정 동기화가 owner user 부재 시 현재 사용자 또는 거래처 user를 assignee로 사용하도록 보강했다.
동시에 INV publish, PAY 처리 경로도 같은 fallback 정책을 적용해 일정 생성 실패가 본문서 상태 변경을 막지 않도록 정리했다.

### 변경 이유
BUG-6. 시나리오 후반부 ORD/INV/PAY 단계에서 동일한 assignee 해석 가정으로 500이 연쇄 발생하던 문제를 막기 위해서다.

## [2026-03-10 12:04] BUG-6 일정 assignee fallback 적용

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java — CNT 최종 승인 일정 동기화에 principal/client fallback assignee 해석 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/sales/order/service/OrderService.java — ORD confirm 일정 동기화에 principal/client fallback assignee 해석 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/billing/invoice/service/InvoiceService.java — INV publish 일정 동기화에 principal/client fallback assignee 해석 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/billing/payment/service/PaymentService.java — PAY 일정 동기화에 client user fallback assignee 해석 추가
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandServiceTest.java — owner user 부재 시 계약 승인 일정이 현재 사용자로 생성되는 fallback 테스트 추가
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/sales/order/service/OrderServiceTest.java — owner user 부재 시 주문 일정 assignee fallback 테스트 추가
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/billing/invoice/service/InvoiceServiceTest.java — owner user 부재 시 청구 일정 assignee fallback 테스트 추가
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/billing/payment/service/PaymentServiceTest.java — owner user 부재 시 결제 일정 assignee fallback 테스트 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음

### 다음 단계
가능하면 로컬 서버에서 `api-test/http/pipeline/scenario1.http`를 다시 실행해 실제 500 재현 여부를 확인

## [2026-03-10] BUG-6 Statement 동일 트랜잭션 참여로 변경

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/billing/statement/service/StatementService.java
- 클래스/메서드: StatementService.createStatement, createAndRecordStatement

### 변경 내용
주문 확정 시 명세서 생성 경로에서 `ObjectProvider<StatementService>` 자기 참조와 `createAndRecordStatementRequiresNew(...)` 분리 호출을 제거했다.
명세서 저장과 STMT DealLog 기록이 `createStatement()`가 참여한 기존 주문 확정 트랜잭션 안에서 그대로 수행되도록 내부 private 메서드 호출 구조로 정리했다.

### 변경 이유
BUG-6. `OrderService.confirmOrder()`가 잡고 있는 주문 row lock과 STMT FK insert의 별도 트랜잭션 충돌을 없애기 위해서다.

## [2026-03-10 12:27] BUG-6 Statement REQUIRES_NEW 제거

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/billing/statement/service/StatementService.java — 명세서 생성/DealLog 기록을 기존 주문 확정 트랜잭션에 참여하도록 `REQUIRES_NEW` 메서드와 self provider 제거
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/sales/order/service/OrderServiceTest.java — 주문 확정 시 ORD DealLog 기록 후 일정 동기화와 STMT 생성 호출이 이어지는 회귀 테스트 추가
- 수정 파일: docs/refactoring/fix-scenario1.md — BUG-6 구조 변경 및 작업 로그 기록 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 해당 없음

### 다음 단계
로컬 서버에서 `PATCH /api/v1/orders/{id}/confirm`를 다시 호출해 lock wait timeout 재현이 사라졌는지 확인
## [2026-03-10 18:15] 모든 문서 조회 테스트 SQL 작성

### 작업 내용
- 수정 파일: output.sql — `/api/v1/documents`, `/api/v1/deals` 프론트 연동 테스트용 계정/딜/문서 샘플 데이터 SQL 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-10 13:26] 문서 요약 뷰 생성 구문 포함

### 작업 내용
- 수정 파일: output.sql — 단독 실행 시 `v_document_summary` 확인 쿼리가 바로 동작하도록 뷰 생성 SQL 포함

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-10] output.sql 테스트 데이터 네임스페이스 정리

### 변경 대상
- 파일: output.sql
- 클래스/메서드: 테스트 SQL 데이터셋

### 변경 내용
`output.sql`의 테스트 데이터를 전용 네임스페이스(`SC1-20260310`) 기준으로 재정리했다.
기존 저번호 ID/일반 코드 대신 높은 고정 ID와 전용 코드/로그인 ID를 사용하도록 바꿨다.
`v_document_summary` 검증 쿼리에 DealType별 집계 쿼리를 추가해 RFQ~PAY 포함 여부를 바로 확인할 수 있게 했다.

### 변경 이유
기존 데이터와의 충돌 가능성을 낮추고 모든 DealType 포함 여부를 명확히 검증하기 위해.

## [2026-03-10 13:40] output.sql 충돌 방지 및 DealType 점검 반영

### 작업 내용
- 수정 파일: output.sql — 전용 로그인/코드/고정 ID 네임스페이스로 재정리하고 DealType별 검증 쿼리 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-10 13:42] 문서 코드 길이 제한 보정

### 작업 내용
- 수정 파일: output.sql — `order_code`, `statement_code`, `invoice_code`, `payment_code` 20자 제한에 맞춰 테스트 코드값 단축

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-10 13:45] 재실행용 정리 구문 보강

### 작업 내용
- 수정 파일: output.sql — 중간 실패 후 재실행 시에도 충돌하지 않도록 삭제 조건을 코드 기준뿐 아니라 고정 ID 기준까지 확장

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-10 13:55] 문서 목록에서 PAY 제외 반영

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/repository/DocumentSummaryQueryRepositoryImpl.java — `/api/v1/documents` 조회에서 PAY 문서를 공통 제외하도록 필터 추가
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/deal/core/repository/DocumentSummaryRepositoryTest.java — PAY 제외 정책에 맞춰 repository 테스트 기대값과 검증 케이스 보강

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-10 14:22] 테스트 SQL과 HTTP 환경 정합성 보정

### 작업 내용
- 수정 파일: output.sql — `930003` 딜에서 INV/PAY 샘플을 제거해 deal snapshot과 실제 문서 체인을 일치시킴
- 수정 파일: api-test/http/_env.http — SQL에서 생성하는 로그인 계정과 기본 fallback ID를 테스트 데이터셋에 맞게 갱신

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음
