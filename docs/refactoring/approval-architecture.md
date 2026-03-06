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

## [2026-03-06] validateStepOrder 2단계 공통화

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java
- 클래스/메서드: ApprovalCommandService.validateStepOrder

### 변경 내용
step 1은 항상 허용하고, step 2는 step 1이 `APPROVED`일 때만 처리하도록 검증 로직을 단순화했다.
문서 타입별 분기를 제거하고 승인 요청의 step order 자체를 기준으로 활성 step 여부를 판단하도록 바꿨다.

### 변경 이유
QUO/CNT 모두 ADMIN 이후 CLIENT step을 동일한 규칙으로 처리하기 위한 구조 정리

## [2026-03-06] QUO 승인 요청 2단계 생성 적용

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java
- 클래스/메서드: ApprovalCommandService.createApprovalRequest

### 변경 내용
견적서(`QUO`) 승인 요청 생성 시에도 `CLIENT` 2단계 step을 함께 생성하도록 분기를 확장했다.
기존 계약서(`CNT`) 2단계 생성 로직은 그대로 유지하면서, QUO와 CNT 모두 동일한 2-step 생성 구조를 사용하게 했다.

### 변경 이유
QUO도 ADMIN 승인 후 CLIENT 승인 단계가 필요한 승인 정책 반영

## [2026-03-06] ApprovalCommandService 승인 문서 의존성 추가

### 변경 대상
- 파일: src/main/java/com/monsoon/seedflowplus/domain/approval/service/ApprovalCommandService.java
- 클래스/메서드: ApprovalCommandService

### 변경 내용
견적서/계약서 실문서 상태 변경을 서비스 내부에서 처리할 수 있도록
`QuotationRepository`, `ContractRepository`, `DocStatusTransitionValidator` 의존성을 주입했다.
이후 승인 결정 단계에서 문서 조회, 상태 전이 검증, 상태 변경을 직접 수행할 준비를 맞췄다.

### 변경 이유
승인 처리 공통 순서의 문서 조회와 상태 전이 검증을 ApprovalCommandService 내부에서 수행하기 위한 구조 변경
