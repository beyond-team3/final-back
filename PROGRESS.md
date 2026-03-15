# SeedFlow+ v2 Remodeling Progress

기준 문서:
- `AGENTS.md`
- `REMODELING_POLICY.md`

진행 원칙:
- 기존 `/api/v1/**` 는 유지
- 신규 정책은 `/api/v2/**` 에 한정
- 각 단계 완료 후 컴파일 확인, 작업 로그 기록, 커밋

## Checklist

- [x] 1. 현재 구조 파악
  - [x] 핵심 정책 문서 확인
  - [x] 주요 엔티티/서비스 1차 식별
  - [x] Deal, RFQ, QUO, CNT, ORD, STMT, INV, PAY 상태/연결 관계 정리
  - [x] 삭제/반려/재작성/승인 시 상태 변경 경로 정리
  - [x] 알림/일정/통계 귀속 기준 정리
- [x] 2. v2 정책 반영용 설계 뼈대 작성
- [x] 3. 공통 enum / value object / DTO 작성
- [x] 4. Deal 중심 조회 계층 작성
- [x] 5. 문서 생성/재작성/승인/취소 흐름 개편
- [x] 6. snapshot 재계산 로직 작성
- [x] 7. 알림/일정/통계 연계 수정
- [x] 8. 컨트롤러 / SecurityConfig / 테스트 작성
- [x] 9. 문서 업데이트 정리
- [ ] 10. 최종 점검 및 전환 가이드 초안

## Current Focus

현재 진행 단계: `10. 최종 점검 및 전환 가이드 초안`

1단계 분석 결과:
- `SalesDeal.currentStatus` 는 문서별 enum 문자열과 직접 결합되어 있음
- `DealPipelineFacade` 는 문서 이벤트 직후 `deal.updateSnapshot(...)` 으로 단건 갱신함
- `QuotationService`, `ContractService` 에는 열린 deal 자동 연결 흐름이 존재함
- QUO/CNT 삭제 시 `restoreDealSnapshotAfter...`, `recomputeDealSnapshot(...)` 등 수동 복구/재계산 로직이 서비스별로 분산되어 있음
- 문서 상태 enum은 `DELETED` 를 포함한 복합 상태 구조이며 `DocumentStatus` 마커 인터페이스에 묶여 있음
- 일정 동기화는 `DealScheduleSyncService.deleteByExternalKey(...)` 기반 삭제 모델을 사용 중임
- 알림 조회는 현재 사용자 기준 최신순 조회이며 deal 묶음 조회 계층은 없음
- 통계는 일반 통계에서 `InvoiceStatus.PAID`, 청구 통계에서 `InvoiceStatus.PUBLISHED|PAID` 와 `StatementStatus.ISSUED` 기준을 사용 중임

다음 작업:
- 완료: QUO/CNT `v2` 생성 시 상위 문서 우선, `dealId` 명시, 신규 deal 생성 규칙 초안 반영
- 완료: `revise` API를 새 문서 생성 방식으로 구현
- 완료: `cancel` 명시 API와 approval 취소, snapshot 재동기화 초안 반영
- 완료: 생성/재작성/취소 후 공통 snapshot 재계산 경로 일반화
- 완료: deal 문맥 알림/일정 조회 API 추가
- 완료: v2 견적 생성 시 문서 단위 일정 생성 연계
- 완료: deal KPI v2 조회 계층 추가
- 완료: `/api/v2/**` 보안 매처와 v2 컨트롤러 테스트 추가
- 완료: `DealSchedule` soft-cancel 상태(`CANCELLED`) 도입 및 조회 제외 처리
- 완료: `/api/v2/statistics/billing/revenue/**` 래퍼 엔드포인트 추가
- 완료: `docs/api/domain-api-list.csv` 에 v2 API 인벤토리 반영
- 완료: `api-test/http` v2 요청 컬렉션 추가
- 완료: `v1-v2-transition-guide.md` 초안 추가
- 남음: 10단계 최종 점검과 v1→v2 전환 가이드 초안
