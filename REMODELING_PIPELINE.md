# REMODELING_PIPELINE

이 문서는 영업 파이프라인(`Deal`, `RFQ`, `QUO`, `CNT`, `ORD`, `STMT`, `INV`, `PAY`) 리모델링 전에
반드시 확정해야 하는 정책 결정을 정리하기 위한 체크리스트다.

목표는 아래 두 가지다.

1. 현재 구현의 암묵적 가정을 정책으로 드러낸다.
2. 다중 `deal` / 다중 문서 모델 전환 시 필요한 의사결정을 빠짐없이 확정한다.

이 문서의 각 항목은 가능한 한 다음 형식으로 확정한다.

- 결정: `<최종 정책>`
- 이유: `<왜 이렇게 정하는지>`
- 영향 범위: `<영향받는 도메인/서비스/API>`
- 미결 이슈: `<추가 논의가 필요한 점>`

---

## 1. Deal 정책

### 1-1. Deal의 비즈니스 단위

결정:
Deal은 하나의 client에 대한 하나의 영업 기회(opportunity) 단위로 정의합니다.

이유:
RFQ 없이 QUO/CNT에서 시작할 수 있어야 하고, 반려 후 재작성도 같은 영업 문맥 안에서 이어져야 합니다. 문서 체인 전체보다 상위의 비즈니스 컨테이너가 Deal이어야 합니다.

영향 범위:
Deal, RFQ, QUO, CNT, ORD, STMT, INV, PAY, 알림, 일정, 통계, 메인 목록

### 1-2. 같은 Client의 동시 다중 Deal 허용 여부

결정:
허용합니다.

추가 기준:
같은 client라도 다음 중 하나가 다르면 다른 deal로 봅니다.

- 제안 목적이 다름
- 품목군/주요 품종군이 다름
- 계약 체결 시도가 독립적임
- 영업 담당자가 별도 영업 건으로 분리 생성함

이유:
실제 영업에서는 동일 거래처와 동시에 여러 건이 진행될 수 있습니다. 단일 client당 단일 deal은 재작성과 신규 영업 건을 구분하기 어렵게 만듭니다.

영향 범위:
Deal 식별자, UI 목록, 문서 생성 API, 알림 문구, 일정 표시

### 1-3. Deal 생성 주체

결정:
새 deal은 아래 경우에만 생성합니다.

- RFQ 생성 시
- 상위 문서 없이 QUO 생성 시
- 상위 문서 없이 CNT 생성 시
- 별도 새 deal 생성 API 호출 시

금지:
“같은 client의 최근 열린 deal에 자동 연결”은 금지합니다.

이유:
deal 생성 시점과 연결 기준을 명확히 해야 데이터 오염을 막을 수 있습니다.

영향 범위:
문서 생성 서비스, Deal 생성 API, 프론트 생성 플로우

## 2. 문서 연결 정책

### 2-1. deal 연결 규칙

결정:
문서의 dealId는 아래 순서로 결정합니다.

- 상위 문서가 있으면 상위 문서의 dealId를 계승
- 상위 문서가 없고 dealId가 명시되면 해당 deal에 연결
- 둘 다 없으면 새 deal 생성

이유:
문서 체인은 기존 deal 문맥을 따라가야 하고, 새 영업 건은 명시적으로 분리돼야 합니다.

### 2-2. 상위 문서 없는 생성 허용 범위

결정:

- RFQ: 항상 허용
- QUO: 허용
- CNT: 허용하되 예외적 케이스로 취급
- ORD, STMT, INV, PAY: 상위 문서 없이 생성 불가

이유:
앞단 영업 문서는 독립 시작이 가능하지만, 후단 실행 문서는 선행 문맥이 필수입니다.

## 3. 단계별 문서 개수 정책

### 3-1. 같은 deal 내 문서 개수

결정:

- RFQ: 여러 개 허용
- QUO: 여러 개 허용
- CNT: 여러 개 허용
- ORD: 여러 개 허용
- STMT, INV, PAY: 업무상 필요 범위 내 여러 개 허용

단, 대표(active) 문서는 단계별로 1개만 둡니다.

이유:
이력 보존과 재작성/재시도는 여러 개가 필요하지만, 현재 진행 기준은 하나여야 snapshot이 안정적입니다.

### 3-2. 같은 단계의 동시 진행(active) 정책

결정:

- 같은 deal에서 같은 단계의 대표 active 문서는 1개만 허용
- 새 문서를 active로 올리면 기존 대표 문서는 SUPERSEDED, REVISED, REPLACED 중 하나로 비대표 처리

이유:
병렬 active 문서가 많아지면 메인 목록, 승인, 알림, 통계 기준이 모호해집니다.

## 4. 재작성 정책

### 4-1. QUO/CNT 재작성 방식

결정:
재작성은 기존 문서 수정이 아니라 새 문서 생성으로 처리합니다.

필수 필드:

- sourceDocumentId
- revisionGroupKey
- revisionNo

이유:
문서 원본, 반려 이력, 승인 이력, 재작성본을 분리 보존해야 합니다. 문서 체크리스트에서도 재작성 계보 필드 추가가 핵심 결정사항으로 정리돼 있습니다.

영향 범위:
QUO, CNT, approval, log, 알림, 일정, 상세 조회

### 4-2. 재작성 가능 시작점

결정:
재작성 후보는 아래 상태의 문서만 허용합니다.

- REJECTED
- EXPIRED
- CANCELLED_BY_RETURN
- 운영상 재작성 허용 플래그가 켜진 문서

APPROVED, COMPLETED, ACTIVE 문서는 재작성 시작점으로 사용하지 않습니다.

## 5. 상태 정책

### 5-1. 문서 상태 분리 원칙

결정:
모든 문서는 상태를 3축으로 분리합니다.

#### A. 생명주기 상태 (lifecycleStatus)

- DRAFT
- ACTIVE
- COMPLETED
- REJECTED
- CANCELLED
- EXPIRED

DELETED 는 사용하지 않고 CANCELLED 또는 SUPERSEDED로 처리

#### B. 승인 상태 (approvalStatus)

- NOT_REQUESTED
- PENDING_INTERNAL
- PENDING_CLIENT
- APPROVED
- REJECTED_INTERNAL
- REJECTED_CLIENT

#### C. 문서 역할 상태 (documentRole)

- REPRESENTATIVE
- NON_REPRESENTATIVE
- SUPERSEDED
- REVISION_SOURCE

이유:
기존 플로우처럼 합성 상태를 계속 늘리면 유지보수가 어렵습니다.

### 5-2. 삭제 정책

결정:
실제 업무 문서는 물리 삭제하지 않습니다.
사용자 액션 “삭제”는 아래 둘 중 하나로 변환합니다.

- 초안 단계면 CANCELLED
- 대표 문서 교체/재작성 관계면 SUPERSEDED

이유:
삭제가 곧 롤백/취소/무효화를 뜻하는 현재 구조를 분리해야 합니다.

## 6. Deal snapshot 정책

문서에서도 대표 snapshot 선정 규칙이 최우선 확정 대상이라고 되어 있습니다.

### 6-1. snapshot 필드

결정:
Deal은 아래 snapshot을 가집니다.

- currentStage
- currentStatus
- representativeDocumentType
- representativeDocumentId
- lastActivityAt

### 6-2. 대표 문서 선정 규칙

결정:
대표 문서는 아래 우선순위로 계산합니다.

#### 더 뒤 단계 문서 우선

PAY > INV > STMT > ORD > CNT > QUO > RFQ

- 같은 단계면 REPRESENTATIVE 문서 우선
- 같은 조건이면 상태 우선순위 적용

#### 상태 우선순위

- ACTIVE
- PENDING_CLIENT
- PENDING_INTERNAL
- APPROVED
- COMPLETED
- REJECTED
- CANCELLED
- EXPIRED

- 마지막으로 createdAt이 최신인 문서

최신 기준 필드:
createdAt으로 고정합니다.

### 6-3. snapshot 재계산 규칙

결정:
다음 이벤트가 발생하면 Deal snapshot을 전면 재계산합니다.

- 문서 생성
- 승인 요청
- 승인/반려
- 재작성
- 취소
- 만료
- 결제 완료

이전 문서 상태를 수동 복구하지 않고, 전체 문서 집합 기준으로 snapshot을 다시 계산합니다.

## 7. Deal 종료 정책

### 7-1. 종료 조건

결정:
Deal은 아래 둘 중 하나면 종료됩니다.

#### 성공 종료

- PAY 완료 또는 최종 수금 완료
- 진행 중(active/pending) 문서 없음

#### 실패 종료

- 모든 문서가 REJECTED, CANCELLED, EXPIRED, SUPERSEDED 상태이고
- 재개 예정이 없는 경우

### 7-2. reopen 정책

결정:
닫힌 deal은 자동 reopen 하지 않습니다.
POST /api/v2/deals/{dealId}/reopen 으로만 재개합니다.

## 8. 조회 API 정책 (/api/v2)

문서에서도 메인 목록 기준과 deal 상세 정책을 별도 확정해야 한다고 되어 있습니다.

### 8-1. 메인 목록 기준

결정:
메인 목록은 deal 중심입니다.

기본 API

- GET /api/v2/deals
- GET /api/v2/deals/{dealId}

### 8-2. 문서 목록 정책

결정:
문서 목록은 분리합니다.

- 기본 목록은 대표 문서 중심
- 전체 문서 이력은 옵션/탭/별도 API로 제공

예시

- GET /api/v2/deals/{dealId}/documents
- GET /api/v2/quotations?dealId=...

### 8-3. deal 상세 화면 정책

결정:
deal 상세에는 반드시 아래를 포함합니다.

- 단계별 대표 문서
- 전체 문서 이력
- 재작성 계보
- approval 이력
- deal log
- 일정
- 알림 요약

## 9. 일정 정책

문서에서 일정 생성 단위, 같은 deal 내 여러 일정 표시, 재작성/삭제 시 처리 여부를 별도 정책으로 두고 있습니다.

### 9-1. 일정 생성 단위

결정:
일정은 문서 단위로 생성합니다.

예:

- QUO 클라이언트 응답 예정일
- CNT 계약 종료일
- PAY 결제 예정일

### 9-2. 같은 deal 내 여러 일정 표시

결정:
deal 화면에서는 전부 노출하되,

- 기본 정렬은 날짜순
- 필터는 문서유형/진행중/완료포함 제공
- 대표 일정 강조 표시 가능

### 9-3. 재작성/삭제 시 일정 처리

결정:
기존 일정은 삭제하지 않고 상태만 바꿉니다.

- 원본 문서 일정: inactive 또는 cancelled
- 재작성본 일정: 새로 생성

이유:
일정 이력을 보존해야 합니다.

## 10. 알림 정책

문서에서 알림 문맥, 재작성 알림, 수신자 기준을 별도 확정 항목으로 두고 있습니다.

### 10-1. 알림 문맥

결정:
알림은 문서 이벤트 기반 저장 + deal 중심 조회로 갑니다.

즉,

저장 단위: 문서 이벤트

UI 문맥: deal 묶음

### 10-2. 재작성 알림

결정:
반려 후 재작성 시 아래를 모두 발행합니다.

- 원본 반려 알림 유지
- 재작성본 생성 알림 발행
- 재승인 요청 시 새 승인 요청 알림 발행

### 10-3. 수신자 기준

결정:
수신자와 문구는 문서 담당자 + deal 담당자 + 승인 주체 기준으로 결정합니다.

기본 수신 규칙

- 내부 승인 대기: 담당 영업사원, 승인권자
- 클라이언트 승인 대기: 클라이언트 담당자
- 반려: 작성자, 담당 영업사원
- 재작성: 작성자, 담당 영업사원
- 결제 완료: 담당 영업사원, 관련 회계/정산 담당자

알림 본문에는 반드시

- dealCode
- dealTitle
- documentType
- documentNo

를 포함합니다.

## 11. 통계 정책

문서에서도 매출 통계와 deal 관계, deal 기반 보조 지표 필요 여부를 확인 대상으로 두고 있습니다.

### 11-1. 매출 통계 기준

결정:
매출 통계는 기존처럼 Invoice/PAY 기준을 유지합니다.

집계 기준

- PAY = COMPLETED 또는
- INV = PAID

둘 중 하나로 단일화해서 구현하되, 최종 기준은 PAY COMPLETED로 통일합니다.

재작성 문서는 매출 통계 집계 대상이 아닙니다.

### 11-2. Deal 기반 보조 지표

결정:
도입합니다.

추가 KPI

- 전체 deal 수
- 진행 중 deal 수
- 성공 종료율
- 평균 deal 리드타임
- 단계별 전환율
- client 반려율
- 문서 재작성률

## 12. 데이터 모델 추가 정책

### 12-1. Deal 식별 보조 키

결정:
추가합니다.

- dealCode : 사람이 읽는 식별자

dealTitle : 사용자 입력 제목

이 항목은 문서에서도 필요성이 직접 언급돼 있습니다.

### 12-2. Deal 생성 입력값

결정:
새 deal 생성 시 아래를 받습니다.

- clientId (필수)
- dealTitle (필수)
- productCategory (선택)
- memo (선택)
- leadSource (선택)
- ownerEmployeeId (필수)

### 12-3. 재작성 계보 필드

결정:
추가합니다.

- sourceDocumentId
- revisionGroupKey
- revisionNo

### 12-4. 최신 문서 기준

결정:
createdAt

### 12-5. 마이그레이션 정책

결정:
기존 데이터는 우선 유지하고, 신규 /api/v2 데이터부터 새 정책 적용합니다.
구 데이터는 배치 보정 대상으로 분리합니다.

## 13. API 버전 정책

결정:
리모델링 결과는 /api/v2/** 로 제공합니다.

원칙

- /api/v1/** : 기존 구조 유지, 점진적 종료 대상
- /api/v2/** : 새 Deal 정책, 새 상태 모델, 새 조회 기준 적용

권장 v2 자원 예시

- GET /api/v2/deals
- POST /api/v2/deals
- GET /api/v2/deals/{dealId}
- POST /api/v2/deals/{dealId}/reopen
- GET /api/v2/deals/{dealId}/documents
- POST /api/v2/quotations
- POST /api/v2/contracts
- POST /api/v2/quotations/{quotationId}/revise
- POST /api/v2/contracts/{contractId}/revise

## 14. 이 정책으로 바로 바꿔야 하는 구현 포인트

바로 영향 받는 건 이 4개입니다.

### 도메인

- Deal 정의 변경
- 문서 상태 3축 분리
- 대표 문서 개념 도입
- 재작성 계보 도입

### API

- /api/v2/deals 중심 조회
- 문서 API를 deal 문맥 기반으로 재구성

### 부가 서비스

- 알림: 문서 이벤트 저장 + deal 기준 조회
- 일정: 문서 단위 생성, deal 단위 집계
- 통계: PAY 완료 기준 유지 + deal KPI 추가

### 마이그레이션

- v1 데이터는 유지
- 신규는 v2 정책
- 이후 배치 보정

## 15. 마지막으로, 추가로 꼭 고정해야 하는 아주 작은 운영 규칙 3개

### A. 대표 문서 수동 변경 허용 여부

결정: 허용하지 않음
대표 문서는 규칙 기반 계산만 허용

### B. CNT 없이 ORD 생성 예외 허용 여부

결정: 허용하지 않음
