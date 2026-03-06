## [2026-03-06] ApprovalCommandService 결과 record 추가

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java
- 클래스/메서드: ApprovalCommandService.DocumentDecisionResult

### 변경 내용
승인 문서 처리 결과를 묶어 다룰 수 있도록 `ApprovalCommandService` 내부에
`DocumentDecisionResult` private record를 추가했다.
아직 사용처는 없고, 이후 문서 상태 전이 계산 결과를 서비스 내부에서 명시적으로 전달하기 위한 준비 구조다.

### 변경 이유
문서 상태 변경 결과를 서비스 내부에서 일관되게 다루기 위한 구조 준비
