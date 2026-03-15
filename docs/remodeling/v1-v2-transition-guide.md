# SeedFlow+ 영업 파이프라인 v1→v2 전환 가이드 초안

## 목적
- `/api/v1/**` 를 유지한 채 `/api/v2/**` 로 리모델링 정책을 병행 적용한다.
- 프론트/QA/운영이 어느 경로를 언제 써야 하는지 기준을 고정한다.

## 적용 범위
- v2 적용 완료:
  - `/api/v2/deals`
  - `/api/v2/deals/{dealId}`
  - `/api/v2/deals/{dealId}/documents`
  - `/api/v2/deals/{dealId}/notifications`
  - `/api/v2/deals/{dealId}/schedules`
  - `/api/v2/deals/kpis`
  - `/api/v2/quotations`
  - `/api/v2/quotations/{quotationId}/revise`
  - `/api/v2/quotations/{quotationId}/cancel`
  - `/api/v2/contracts`
  - `/api/v2/contracts/{contractId}/revise`
  - `/api/v2/contracts/{contractId}/cancel`
  - `/api/v2/statistics/billing/revenue/**`
- 계속 v1 유지:
  - RFQ, ORD, STMT, INV, PAY 생성/상세/변경 API
  - approval 실처리 API
  - 일반 통계 API

## 정책 차이
- deal 연결:
  - v1은 열린 deal 자동 연결이 존재한다.
  - v2는 `상위 문서 -> dealId -> 신규 deal` 순서만 허용한다.
- 재작성:
  - v1은 상태 전환/복구 중심이다.
  - v2는 항상 새 문서 생성과 계보 보존으로 처리한다.
- snapshot:
  - v1은 서비스별 수동 복구가 남아 있다.
  - v2는 전체 문서 기준 재계산 경로를 사용한다.
- 일정:
  - v1 삭제 호출부도 현재는 `DealScheduleStatus.CANCELLED` soft-cancel로 수렴한다.
- 통계:
  - billing revenue v2는 새 집계 로직이 아니라 기존 `PAY COMPLETED` 기준 래퍼다.

## 권장 사용 기준
- 신규 deal 중심 화면:
  - v2 사용
- QUO/CNT 생성, 재작성, 취소:
  - v2 사용
- 기존 주문/청구/결제 운영 화면:
  - v1 유지
- 통계:
  - deal KPI는 v2 사용
  - 일반 매출 추이/랭킹은 v1 유지
  - billing revenue는 필요 시 v2 경로로 점진 전환 가능

## QA 체크포인트
- 같은 client에서 deal 자동 연결이 더 이상 일어나지 않는지 확인
- QUO/CNT 재작성 시 원본 문서가 수정되지 않고 새 문서가 생성되는지 확인
- 취소된 deal schedule 이 조회에서 제외되는지 확인
- `/api/v2/statistics/billing/revenue/**` 응답이 v1과 동일 집계값을 반환하는지 확인
- v1 문서 조회/일정/통계 회귀가 없는지 확인

## 운영 전환 체크리스트
- 프론트에서 deal 메인 목록 호출을 `/api/v2/deals` 로 전환
- QUO/CNT 생성/재작성/취소 액션을 `/api/v2/quotations/**`, `/api/v2/contracts/**` 로 전환
- KPI 화면은 `/api/v2/deals/kpis` 로 전환
- billing revenue 화면은 필요 시 `/api/v2/statistics/billing/revenue/**` 로 전환
- QA에서 `api-test/http/deal/deal-v2.http`, `api-test/http/deal/document-v2.http`, `api-test/http/statistics/billing-revenue-v2.http` 시나리오 실행
- 운영 반영 전 `DocumentSummaryQueryControllerTest`, `ScheduleControllerTest`, `StatisticsControllerTest` 재실행

## 잔여 리스크
- `SalesDeal.currentStatus` 는 여전히 v1 복합 상태 문자열이라 완전한 3축 영속 모델은 아직 아니다.
- RFQ/ORD/STMT/INV/PAY 명령 API는 v2로 아직 분리되지 않았다.
- `dealCode`, `dealTitle` 은 read model 계약에는 반영됐지만 영속 필드는 아직 확정되지 않았다.
- reopen 명시 API는 아직 추가되지 않았다.

## 남은 전환 작업
- RFQ/ORD/STMT/INV/PAY의 v2 명령 API 분리 여부 결정
- reopen 명시 API 추가
- `dealCode`, `dealTitle` 영속 필드 확정
- API 스펙 문서와 프론트 소비 계약 최종 동기화
