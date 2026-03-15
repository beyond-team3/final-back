# Approval 기능 구현 요약 (Current)

## 1. 목적
Approval 도메인은 QUO/CNT 문서의 승인 요청 생성, 단계별 승인/반려 처리, 권한 기반 조회를 담당한다.

주요 시나리오:
- 승인 요청 생성
- 승인 단계 결정(승인/반려)
- 승인 요청 상세 조회
- 승인 요청 검색(조건 + 권한 범위)
- 승인 결과에 따른 문서 상태 전이, 딜 로그 기록, 알림 이벤트 발행

## 2. 패키지 구조
- `domain.approval.controller`
  - `ApprovalController`
- `domain.approval.service`
  - `ApprovalCommandService`
  - `ApprovalDealLogWriter`
- `domain.approval.repository`
  - `ApprovalRequestRepository`
  - `ApprovalStepRepository`
  - `ApprovalDecisionRepository`
- `domain.approval.entity`
  - `ApprovalRequest`, `ApprovalStep`, `ApprovalDecision`
  - enum: `ApprovalStatus`, `ApprovalStepStatus`, `DecisionType`
- `domain.approval.dto`
  - request: `CreateApprovalRequestRequest`, `DecideApprovalRequest`
  - response: `CreateApprovalRequestResponse`, `ApprovalDetailResponse`, `ApprovalStepResponse`

## 3. 핵심 도메인 정책

### 3.1 지원 문서 타입/중복 정책
- 지원 `DealType`: `QUO`, `CNT`만 허용
- `PENDING` 상태로 동일 문서(`dealType + targetId`)의 요청이 이미 있으면 생성 차단
- 동시성 충돌로 DB unique 충돌 발생 시 `APPROVAL_REQUEST_DUPLICATED`로 정규화

### 3.2 승인 단계 모델
- 요청 생성 시 기본 단계:
  - 1단계: `ADMIN`, `WAITING`
  - 2단계: `CLIENT`, `WAITING` (QUO/CNT에서 추가)
- 단계별 단일 결정 보장:
  - `ApprovalDecision`은 `approval_step_id` unique
  - 의사결정 시 `ApprovalStepRepository.findByIdAndApprovalRequestIdForUpdate(...)`로 비관적 락 사용

### 3.3 권한/소유 검증
- 생성(create):
  - `SALES_REP`, `ADMIN`, `CLIENT` principal을 `ActorType`으로 매핑
  - `SALES_REP`는 대상 문서의 딜 담당자(ownerEmp)와 본인 employeeId 일치 필수
  - `CLIENT`는 `clientIdSnapshot` 정합성(스냅샷 누락/불일치) 검증
- 결정(decide):
  - 단계 actor와 principal role 일치 필요
  - `CLIENT` 단계는 `request.clientIdSnapshot == principal.clientId` 필수
- 조회(get/search):
  - `ADMIN`: 전체 접근
  - `CLIENT`: 본인 client 범위
  - `SALES_REP`: 본인 담당 딜 범위

### 3.4 상태 전이/문서 반영
- 결정 시 공통 순서:
  1. 요청/단계 조회 및 상태 확인
  2. 권한/순서/입력 검증
  3. 대상 문서 상태 전이 검증(`DocStatusTransitionValidator.validateOrThrow`)
  4. 문서 상태 업데이트 + 결정 저장 + 단계/요청 상태 업데이트
  5. 딜 로그 기록 및 알림 이벤트 발행
- QUO 상태 전이:
  - ADMIN 승인: `WAITING_ADMIN -> WAITING_CLIENT`
  - ADMIN 반려: `WAITING_ADMIN -> REJECTED_ADMIN`
  - CLIENT 승인: `WAITING_CLIENT -> FINAL_APPROVED`
  - CLIENT 반려: `WAITING_CLIENT -> REJECTED_CLIENT`
- CNT 상태 전이:
  - ADMIN 승인: `WAITING_ADMIN -> WAITING_CLIENT`
  - ADMIN 반려: `WAITING_ADMIN -> REJECTED_ADMIN`
  - CLIENT 승인: `WAITING_CLIENT -> COMPLETED`
  - CLIENT 반려: `WAITING_CLIENT -> REJECTED_CLIENT`

### 3.5 요청/단계 최종 상태
- 반려 결정 즉시 `ApprovalRequest.REJECTED`
- 2단계 승인 완료 시 `ApprovalRequest.APPROVED`
- 반려 사유: `REJECT` 결정에서 필수

### 3.6 알림 이벤트 연계
- 이벤트 발행은 `NotificationEventPublisher.publishAfterCommit(...)` 사용
- 생성 시: 1차 승인자(모든 ADMIN)에게 `ApprovalRequestedEvent`
- 단계 승인 후 다음 단계가 WAITING이면 다음 승인자에게 `ApprovalRequestedEvent`
- 반려 시: 요청자에게 `ApprovalRejectedEvent`
- 최종 승인 시: 요청자에게 `ApprovalCompletedEvent`

## 4. API
기본 경로: `/api/v1/approvals`

- `POST /api/v1/approvals`
  - 승인 요청 생성
  - body: `dealType(QUO|CNT)`, `targetId`, `clientIdSnapshot`, `targetCodeSnapshot`
  - 응답: `CreateApprovalRequestResponse`
- `POST /api/v1/approvals/{approvalId}/steps/{stepId}/decision`
  - 승인/반려 결정
  - body: `decision(APPROVE|REJECT)`, `reason`
  - 응답: `ApprovalDetailResponse`
- `GET /api/v1/approvals/{approvalId}`
  - 승인 요청 상세 조회
- `GET /api/v1/approvals?status=&dealType=&targetId=`
  - 승인 요청 검색(Pageable)

응답 포맷은 공통 `ApiResult` 사용.

## 5. 데이터/영속성 포인트
- `tbl_approval_request`
  - 핵심 컬럼: `deal_type`, `target_id`, `status`, `client_id_snapshot`, `target_code_snapshot`
  - 인덱스: `(deal_type, target_id)`, `(status)`
- `tbl_approval_step`
  - unique: `(approval_request_id, step_order)`
- `tbl_approval_decision`
  - unique: `(approval_step_id)`
- 검색 쿼리(`ApprovalRequestRepository`):
  - 관리자 전체 검색
  - client/영업 담당자 범위 검색은 최신 `SalesDealLog` 기준 접근 범위 제한

## 6. 트랜잭션/시간 정책
- `ApprovalCommandService` 클래스 레벨 `@Transactional`
- 조회 메서드(`getApproval`, `search`)는 `@Transactional(readOnly = true)`
- 결정 시 step 조회는 PESSIMISTIC_WRITE로 동시 결정 충돌 완화
- 시간 생성은 `Clock` 주입 + `LocalDateTime.now(clock)` 사용

## 7. 예외/검증
- 인증/주체 정보 누락: `UNAUTHORIZED`
- 승인 요청/단계 미존재: `APPROVAL_NOT_FOUND`, `APPROVAL_STEP_NOT_FOUND`
- 이미 결정/종료된 요청: `APPROVAL_ALREADY_DECIDED`
- 권한 불일치: `APPROVAL_ROLE_MISMATCH`, `APPROVAL_CLIENT_MISMATCH`, `ACCESS_DENIED`
- 반려 사유 누락: `APPROVAL_REASON_REQUIRED`
- 문서 타입/상태 전이 오류: `APPROVAL_UNSUPPORTED_DEAL_TYPE`, `INVALID_DOC_STATUS_TRANSITION`
- 문서 미존재: `QUOTATION_NOT_FOUND`, `CONTRACT_NOT_FOUND`, `DEAL_NOT_FOUND`

## 8. 연계 컴포넌트
- 딜 로그: `ApprovalDealLogWriter` + `DealPipelineFacade`
- 문서 상태 전이 검증: `DocStatusTransitionValidator`
- 알림 연계: approval 이벤트 3종 + notification 이벤트 핸들러
