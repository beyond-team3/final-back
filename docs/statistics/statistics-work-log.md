## [2026-03-06 10:33] 월별 청구 매출 category 필터 반영

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/statistics/billing/repository/BillingRevenueStatisticsRepository.java` — 월별 청구 매출 조회에서도 category 파라미터가 존재할 때만 `EXISTS` 서브쿼리에 품종 조건이 적용되도록 수정

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-06 10:41] 서비스 검증 정책 테스트 고정

### 작업 내용
- 수정 파일: `src/test/java/com/monsoon/seedflowplus/domain/statistics/billing/service/BillingRevenueStatisticsQueryServiceTest.java` — 24개월 허용/초과, 역전 날짜, fromDate/toDate null 검증 케이스를 추가해 `BillingRevenueStatisticsQueryService` 검증 정책을 고정

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음
