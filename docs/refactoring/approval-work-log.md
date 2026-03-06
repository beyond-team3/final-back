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
