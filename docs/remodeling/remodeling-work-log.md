# Remodeling Work Log

## [2026-03-15 03:40] AGENTS v2 버전 계층 계획 추가 및 문구 보정

### 작업 내용
- 수정 파일: `AGENTS.md` — `/api/v2/**` 신규 버전 계층 추가 계획, 권장 패키지 구조, 1차/보류 대상, 구현 단계, 주의사항을 추가
- 수정 파일: `AGENTS.md` — 범위 외 항목의 `AGENTS.md 자체 수정` 문구를 사용자 명시 요청 없는 임의 수정 금지로 보정
- 수정 파일: `docs/remodeling/remodeling-work-log.md` — 리모델링 전용 작업 로그 파일 신규 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 없음

### 다음 단계
`/api/v2/**` 1차 대상 도메인별 컨트롤러/서비스 패키지 초안 정리

## [2026-03-15 03:48] AGENTS 엔티티/Enum 보존 규칙 추가

### 작업 내용
- 수정 파일: `AGENTS.md` — 엔티티/enum 변경 시 기존 내용을 바로 삭제하지 말고 우선 주석 또는 대체 보존 방식으로 남기도록 지시사항 추가

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 없음

### 다음 단계
사용자 요청 시 AGENTS 문서 변경분 stage 및 커밋

## [2026-03-15 04:05] 리모델링 진행 체크리스트 문서 추가

### 작업 내용
- 수정 파일: `PROGRESS.md` — 권장 구현 순서를 기준으로 단계별 체크리스트와 현재 분석 진행 상태를 추가
- 수정 파일: `docs/remodeling/remodeling-work-log.md` — 진행 체크리스트 추가 작업 로그 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 없음

### 다음 단계
1단계 현재 구조 파악 세부 분석 계속 진행

## [2026-03-15 04:17] 현재 구조 파악 1단계 분석 완료

### 작업 내용
- 수정 파일: `PROGRESS.md` — 문서 상태 결합, deal 자동 연결, snapshot 갱신, 일정/알림/통계 귀속 기준에 대한 1단계 분석 결과를 반영하고 다음 단계 포커스를 갱신
- 수정 파일: `docs/remodeling/remodeling-work-log.md` — 1단계 분석 완료 작업 로그 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 없음

### 다음 단계
2단계 v2 정책 반영용 설계 뼈대 작성

## [2026-03-15 04:26] v2 공통 모델 초안 추가

### 작업 내용
- 수정 파일: `docs/remodeling/remodeling-architecture.md` — `v2` 공통 상태 enum과 deal 중심 DTO 추가 구조를 기록
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/common/DocumentLifecycleStatus.java` — 문서 생명주기 상태 enum 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/common/DocumentApprovalStatus.java` — 문서 승인 상태 enum 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/common/DocumentRole.java` — 문서 대표성 역할 enum 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/dto/RevisionInfoDto.java` — 재작성 계보 DTO 초안 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/dto/DealSnapshotDto.java` — deal snapshot DTO 초안 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/dto/DealSummaryDto.java` — deal 목록 DTO 초안 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/dto/DealDetailDto.java` — deal 상세 DTO 초안 추가
- 수정 파일: `PROGRESS.md` — 2단계, 3단계 완료 처리 및 4단계 포커스 반영

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 컴파일 확인 전

### 다음 단계
4단계 deal 중심 조회 계층 작성

## [2026-03-15 04:39] v2 deal 조회 계층 초안 추가

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/controller/DealV2QueryController.java` — `/api/v2/deals` 목록/상세/문서목록 조회 엔드포인트 초안 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/service/DealV2QueryService.java` — 기존 deal/document 리포지토리 재사용 기반의 `v2` 조회 서비스 초안 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/dto/DealDocumentSummaryDto.java` — deal 문서 목록 응답 DTO 추가
- 수정 파일: `docs/remodeling/remodeling-architecture.md` — `v2` deal 조회 계층 구조를 기록
- 수정 파일: `PROGRESS.md` — 4단계 완료 처리 및 5단계 포커스 반영
- 수정 파일: `docs/remodeling/remodeling-work-log.md` — `v2` deal 조회 계층 추가 작업 로그 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 최초 compileJava 실패(`Iterable.stream()`, `updatedAt` getter) 후 수정하여 재컴파일 성공

### 다음 단계
5단계 문서 생성/재작성/승인/취소 흐름 개편

## [2026-03-15 05:06] v2 QUO/CNT 생성 및 재작성 초안 추가

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/entity/QuotationHeader.java` — 견적서 재작성 계보 필드와 설정 메서드 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/entity/ContractHeader.java` — 계약서 재작성 계보 필드와 설정 메서드 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/repository/QuotationRepository.java` — revision group 기준 최신 revision 조회 메서드 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/repository/ContractRepository.java` — revision group 기준 최신 revision 조회 메서드 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/v2/**` — 견적서 v2 생성/재작성 DTO, 서비스, 컨트롤러 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/v2/**` — 계약서 v2 생성/재작성 DTO, 서비스, 컨트롤러 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/dto/DealDocumentCommandResultDto.java` — 생성/재작성 결과 DTO 추가
- 수정 파일: `docs/remodeling/remodeling-architecture.md` — 5단계 명령 계층 초안 구조 기록
- 수정 파일: `docs/remodeling/remodeling-work-log.md` — 5단계 작업 로그 기록

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → `ContractV2CommandService` 에 `ClientRepository`, `EmployeeRepository` import 누락으로 최초 실패 후 수정하여 재컴파일 성공

### 다음 단계
5단계 컴파일 확인 및 cancel/supersede 보완

## [2026-03-15 05:22] v2 cancel 및 snapshot 재동기화 초안 추가

### 작업 내용
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/deal/v2/service/DealV2SnapshotSyncService.java` — document summary 기반 deal snapshot 재동기화 서비스 초안 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/v2/service/QuotationV2CommandService.java` — v2 견적서 취소, approval 취소, snapshot 재동기화 후처리 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/v2/service/ContractV2CommandService.java` — v2 계약서 취소, approval 취소, snapshot 재동기화 후처리 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/quotation/v2/controller/QuotationV2Controller.java` — 견적서 cancel API 추가
- 수정 파일: `src/main/java/com/monsoon/seedflowplus/domain/sales/contract/v2/controller/ContractV2Controller.java` — 계약서 cancel API 추가
- 수정 파일: `docs/remodeling/remodeling-architecture.md` — cancel/snapshot 후처리 구조 기록
- 수정 파일: `PROGRESS.md` — 5단계 완료 처리 및 6단계 포커스 반영

### 컴파일 결과
- [x] 오류 없음
- [ ] 오류 있음 → 없음

### 다음 단계
6단계 snapshot 재계산 로직 일반화
