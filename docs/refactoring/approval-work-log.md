## [2026-03-06 11:37] DocumentDecisionResult 추가

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java — 내부 DocumentDecisionResult record 추가
- 수정 파일: docs/refactoring/approval-architecture.md — 구조 변경 기록 추가
- 수정 파일: docs/refactoring/approval-work-log.md — 작업 및 컴파일 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
validateStepOrder() 수정

## [2026-03-06 11:40] validateStepOrder 수정

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java — step 1/2 공통 순서 검증으로 교체
- 수정 파일: docs/refactoring/approval-architecture.md — 구조 변경 기록 추가
- 수정 파일: docs/refactoring/approval-work-log.md — 작업 및 컴파일 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
createApprovalRequest()의 QUO 2-step 생성 수정

## [2026-03-06 11:42] QUO createApprovalRequest 2-step 적용

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java — QUO 승인 요청에도 CLIENT step 2 생성 추가
- 수정 파일: docs/refactoring/approval-architecture.md — 구조 변경 기록 추가
- 수정 파일: docs/refactoring/approval-work-log.md — 작업 및 컴파일 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 15:00] approval submit 소유권 검증 및 로그 분리

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java — SALES_REP submit 시 QUO/CNT 연결 deal 담당자 소유권 검증 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalDealLogWriter.java — submit 로그를 snapshot 동기화 없는 DealLogWriteService 경로로 분리
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandServiceTest.java — SALES_REP/ADMIN submit 소유권 정상·예외 케이스 추가
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalDealLogWriterTest.java — submit 로그가 sync 없이 기록되는 경로 테스트 추가
- 수정 파일: docs/refactoring/approval-architecture.md — 구조 변경 기록 추가
- 수정 파일: docs/refactoring/approval-work-log.md — 작업 및 테스트 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 14:03] 승인 접근 제어 및 stage mismatch 이슈 수정

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/repository/ApprovalRequestRepository.java — CLIENT/SALES_REP 승인 요청 검색을 최신 SalesDealLog 1건 기준으로 제한
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java — 미지원 역할을 ActorType.SYSTEM으로 매핑하지 않고 UNAUTHORIZED 예외 처리
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalDealLogWriter.java — 승인 로그 fromStage와 deal currentStage 불일치 시 예외 발생
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/approval/repository/ApprovalRequestRepositoryTest.java — 최신 로그 기준 CLIENT/SALES_REP 접근 제어 및 snapshot 우선 회귀 테스트 추가
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandServiceTest.java — SALES_REP scoped search 경로 테스트 추가
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalDealLogWriterTest.java — stage mismatch 예외 테스트로 보정

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 12:04] 승인 에러 코드 추가

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/core/common/support/error/ErrorCode.java — AP010 승인 에러 코드 추가
- 수정 파일: docs/refactoring/approval-architecture.md — 구조 변경 기록 추가
- 수정 파일: docs/refactoring/approval-work-log.md — 작업 및 컴파일 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
ApprovalCommandServiceTest 재실행

## [2026-03-06 12:06] ApprovalDealLogWriter 정리 및 승인 테스트 추가

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalDealLogWriter.java — writer 내부 상태 계산 제거 및 전달값 기반 기록으로 정리
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java — writer 호출 시 표준 인자 순서로 정리
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandServiceTest.java — QUO/CNT 승인 흐름 9개 Mockito 단위테스트 추가
- 수정 파일: docs/refactoring/approval-architecture.md — 구조 변경 기록 추가
- 수정 파일: docs/refactoring/approval-work-log.md — 작업 및 테스트 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 11:47] ApprovalCommandService 의존성 추가

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java — QuotationRepository, ContractRepository, DocStatusTransitionValidator 주입 추가
- 수정 파일: docs/refactoring/approval-architecture.md — 구조 변경 기록 추가
- 수정 파일: docs/refactoring/approval-work-log.md — 작업 및 컴파일 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
applyQuotationDecision() 구현

## [2026-03-06 11:48] applyQuotationDecision 구현

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java — QUO 문서 조회, 상태 전이 검증, 상태 변경 helper 추가
- 수정 파일: docs/refactoring/approval-architecture.md — 구조 변경 기록 추가
- 수정 파일: docs/refactoring/approval-work-log.md — 작업 및 컴파일 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
applyContractDecision() 구현

## [2026-03-06 11:49] applyContractDecision 구현

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java — CNT 문서 조회, 상태 전이 검증, 상태 변경 helper 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/sales/contract/entity/ContractHeader.java — 계약서 상태 변경 메서드 추가
- 수정 파일: docs/refactoring/approval-architecture.md — 구조 변경 기록 추가
- 수정 파일: docs/refactoring/approval-work-log.md — 작업 및 컴파일 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
resolveAndApplyDocumentDecision() 구현

## [2026-03-06 11:50] resolveAndApplyDocumentDecision 구현

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java — dealType 기반 문서 상태 적용 분기 helper 추가
- 수정 파일: src/main/java/com/monsoon/seedflowplus/core/common/support/error/ErrorType.java — 미지원 승인 문서 타입 예외 추가
- 수정 파일: docs/refactoring/approval-architecture.md — 구조 변경 기록 추가
- 수정 파일: docs/refactoring/approval-work-log.md — 작업 및 컴파일 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
decideStep() 재구성

## [2026-03-06 11:55] decideStep 재구성

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java — 문서 상태 선변경 기준으로 decideStep 순서 재배치
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalDealLogWriter.java — 서비스에서 계산한 상태/단계를 그대로 기록하도록 writeDecision 변경
- 수정 파일: docs/refactoring/approval-architecture.md — 구조 변경 기록 추가
- 수정 파일: docs/refactoring/approval-work-log.md — 작업 및 컴파일 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 12:28] ApprovalCommandService 이슈 수정

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java — QUO clientIdSnapshot 필수화, submit actorType/actorId 매핑 보정, search totalElements 안전화
- 수정 파일: src/main/java/com/monsoon/seedflowplus/core/common/support/error/ErrorType.java — clientIdSnapshot 필수 메시지를 승인 공통 문구로 정리
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandServiceTest.java — snapshot 필수, submit log actor 매핑, 접근 제어 후 totalElements 검증 테스트 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 12:48] 승인 요청 이슈 4건 수정

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java — QUO/CNT 승인 요청 생성 시 문서 기준 client snapshot 강제, 역할별 승인 요청 검색 쿼리 사용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/repository/ApprovalRequestRepository.java — CLIENT/SALES_REP 접근 제어를 저장소 페이징 쿼리로 분리
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalDealLogWriter.java — 승인 로그 fromStage를 실제 deal snapshot 기준으로 기록
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/controller/ApprovalController.java — 승인 요청 생성 API에 201 Created 응답 추가
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandServiceTest.java — snapshot 검증 및 저장소 기반 검색 테스트 보강
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalDealLogWriterTest.java — 실제 deal stage 기록 검증 테스트 추가
- 수정 파일: docs/refactoring/approval-architecture.md — 구조 변경 기록 추가
- 수정 파일: docs/refactoring/approval-work-log.md — 작업 및 테스트 결과 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음
