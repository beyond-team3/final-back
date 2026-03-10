## [2026-03-10 18:15] 모든 문서 조회 테스트 SQL 작성

### 작업 내용
- 수정 파일: output.sql — `/api/v1/documents`, `/api/v1/deals` 프론트 연동 테스트용 계정/딜/문서 샘플 데이터 SQL 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-10 13:26] 문서 요약 뷰 생성 구문 포함

### 작업 내용
- 수정 파일: output.sql — 단독 실행 시 `v_document_summary` 확인 쿼리가 바로 동작하도록 뷰 생성 SQL 포함

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-10] output.sql 테스트 데이터 네임스페이스 정리

### 변경 대상
- 파일: output.sql
- 클래스/메서드: 테스트 SQL 데이터셋

### 변경 내용
`output.sql`의 테스트 데이터를 전용 네임스페이스(`SC1-20260310`) 기준으로 재정리했다.
기존 저번호 ID/일반 코드 대신 높은 고정 ID와 전용 코드/로그인 ID를 사용하도록 바꿨다.
`v_document_summary` 검증 쿼리에 DealType별 집계 쿼리를 추가해 RFQ~PAY 포함 여부를 바로 확인할 수 있게 했다.

### 변경 이유
기존 데이터와의 충돌 가능성을 낮추고 모든 DealType 포함 여부를 명확히 검증하기 위해.

## [2026-03-10 13:40] output.sql 충돌 방지 및 DealType 점검 반영

### 작업 내용
- 수정 파일: output.sql — 전용 로그인/코드/고정 ID 네임스페이스로 재정리하고 DealType별 검증 쿼리 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-10 13:42] 문서 코드 길이 제한 보정

### 작업 내용
- 수정 파일: output.sql — `order_code`, `statement_code`, `invoice_code`, `payment_code` 20자 제한에 맞춰 테스트 코드값 단축

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-10 13:45] 재실행용 정리 구문 보강

### 작업 내용
- 수정 파일: output.sql — 중간 실패 후 재실행 시에도 충돌하지 않도록 삭제 조건을 코드 기준뿐 아니라 고정 ID 기준까지 확장

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음

## [2026-03-10 13:55] 문서 목록에서 PAY 제외 반영

### 작업 내용
- 수정 파일: src/main/java/com/monsoon/seedflowplus/domain/deal/core/repository/DocumentSummaryQueryRepositoryImpl.java — `/api/v1/documents` 조회에서 PAY 문서를 공통 제외하도록 필터 추가
- 수정 파일: src/test/java/com/monsoon/seedflowplus/domain/deal/core/repository/DocumentSummaryRepositoryTest.java — PAY 제외 정책에 맞춰 repository 테스트 기대값과 검증 케이스 보강

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → <내용>

### 다음 단계
없음
