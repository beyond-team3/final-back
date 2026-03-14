# SeedFlow+ v2 Remodeling Progress

기준 문서:
- `AGENTS.md`
- `REMODELING_POLICY.md`

진행 원칙:
- 기존 `/api/v1/**` 는 유지
- 신규 정책은 `/api/v2/**` 에 한정
- 각 단계 완료 후 컴파일 확인, 작업 로그 기록, 커밋

## Checklist

- [ ] 1. 현재 구조 파악
  - [x] 핵심 정책 문서 확인
  - [x] 주요 엔티티/서비스 1차 식별
  - [ ] Deal, RFQ, QUO, CNT, ORD, STMT, INV, PAY 상태/연결 관계 정리
  - [ ] 삭제/반려/재작성/승인 시 상태 변경 경로 정리
  - [ ] 알림/일정/통계 귀속 기준 정리
- [ ] 2. v2 정책 반영용 설계 뼈대 작성
- [ ] 3. 공통 enum / value object / DTO 작성
- [ ] 4. Deal 중심 조회 계층 작성
- [ ] 5. 문서 생성/재작성/승인/취소 흐름 개편
- [ ] 6. snapshot 재계산 로직 작성
- [ ] 7. 알림/일정/통계 연계 수정
- [ ] 8. 컨트롤러 / SecurityConfig / 테스트 작성
- [ ] 9. 문서 업데이트 정리
- [ ] 10. 최종 점검 및 전환 가이드 초안

## Current Focus

현재 진행 단계: `1. 현재 구조 파악`

1차 확인 사항:
- `SalesDeal.currentStatus` 는 문서별 enum 문자열과 직접 결합되어 있음
- `DealPipelineFacade` 는 문서 이벤트 직후 `deal.updateSnapshot(...)` 으로 단건 갱신함
- `QuotationService`, `ContractService` 에는 열린 deal 자동 연결 흐름이 존재함

다음 작업:
- 문서별 상태 enum과 생성/전환 서비스 범위를 전수 확인
- 알림/일정/통계가 deal/document 중 어느 축으로 조회되는지 정리
