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
