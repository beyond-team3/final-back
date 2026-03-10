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
