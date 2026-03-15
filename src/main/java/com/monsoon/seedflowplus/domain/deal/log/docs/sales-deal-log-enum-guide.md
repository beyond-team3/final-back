# SalesDeal / SalesDealLog Enum 가이드

`SalesDeal`은 거래 1건의 최신 상태 스냅샷을 보관하고,
`SalesDealLog`는 영업 Deal(RFQ→QUO→CNT→ORD→STMT→INV)의 상태 변경 이벤트를
append-only로 기록합니다.

---

## DealType — 어떤 문서인가 (docType / latestDocType)

| 값 | 문서 |
|---|---|
| `RFQ`  | 견적요청서 |
| `QUO`  | 견적서 |
| `CNT`  | 계약서 |
| `ORD`  | 주문서 |
| `STMT` | 명세서 |
| `INV`  | 청구서 |
| `PAY`  | 결제 (결제를 독립 Deal으로 추적할 때만 사용) |

---

## ActionType — 무슨 행위가 발생했는가 (actionType)

| 값 | 설명 |
|---|---|
| `CREATE`    | 문서 생성 |
| `SUBMIT`    | 승인 요청 제출 |
| `RESUBMIT`  | 반려 후 재제출 |
| `CONVERT`   | 다음 문서로 전환 (RFQ→QUO, QUO→CNT 등) |
| `APPROVE`   | 승인 |
| `REJECT`    | 반려 |
| `CONFIRM`   | 주문 확정 |
| `ISSUE`     | 명세서·청구서 발행 |
| `PAY`       | 결제 완료 처리 |
| `EXPIRE`    | 만료 |
| `CANCEL`    | 취소 |

> **APPROVE / REJECT의 행위자 구분**
> `APPROVE_ADMIN` / `APPROVE_CLIENT`처럼 나누지 않고,
> `actionType=APPROVE` + `actorType=ADMIN|CLIENT` 조합으로 구분합니다.

---

## DealStage — 현재 어느 단계인가 (currentStage / fromStage / toStage)

문서마다 상태값 이름이 달라도 공통 단계로 통일해 대시보드·집계에 사용합니다.
`SalesDeal`에는 `currentStage`로 최신값이,
`SalesDealLog`에는 `fromStage`(변경 전) → `toStage`(변경 후) 형태로 기록됩니다.

| 값 | 설명 |
|---|---|
| `CREATED`         | 문서 생성됨 (초안) |
| `IN_PROGRESS`     | 작성·검토 진행 중 |
| `PENDING_ADMIN`   | 관리자 승인 대기 |
| `REJECTED_ADMIN`  | 관리자 반려 |
| `PENDING_CLIENT`  | 거래처 승인 대기 |
| `REJECTED_CLIENT` | 거래처 반려 |
| `CONFIRMED`       | 주문 확정 (ORD) |
| `ISSUED`          | 발행 완료 (STMT·INV) |
| `PAID`            | 결제 완료 |
| `APPROVED`        | 최종 승인·계약 완료 또는 전환 전 원본 문서 종결 |
| `EXPIRED`         | 만료 |
| `CANCELED`        | 취소 |

> **집계 시 주의**: "처리 완료" 건수를 집계할 때 `CONFIRMED` + `APPROVED`를 모두 포함해야 합니다.
> (`CONFIRMED`은 주문 확정, `APPROVED`는 견적·계약 완료에 사용됩니다.)

---

## ActorType — 누가 수행했는가 (actorType)

| 값 | 설명 |
|---|---|
| `SALES_REP` | 일반 영업사원 |
| `ADMIN`  | 관리자 |
| `CLIENT` | 거래처 |
| `SYSTEM` | 시스템 자동 처리 |

---

## currentStatus / fromStatus / toStatus — 문서별 세부 상태

`String` 타입으로 저장되며, 각 문서의 상태 Enum `name()` 값을 담습니다.
`docType`(또는 `latestDocType`)에 따라 허용되는 값이 결정됩니다.

| dealType | 사용 Enum | 허용값 |
|---|---|---|
| `RFQ`  | `QuotationRequestStatus` | `PENDING` `REVIEWING` `COMPLETED` `DELETED` |
| `QUO`  | `QuotationStatus` | `WAITING_ADMIN` `REJECTED_ADMIN` `WAITING_CLIENT` `REJECTED_CLIENT` `FINAL_APPROVED` `WAITING_CONTRACT` `COMPLETED` `EXPIRED` `DELETED` |
| `CNT`  | `ContractStatus`  | `WAITING_ADMIN` `REJECTED_ADMIN` `WAITING_CLIENT` `REJECTED_CLIENT` `COMPLETED` `ACTIVE_CONTRACT` `EXPIRED` `DELETED` |
| `ORD`  | `OrderStatus`     | `PENDING` `CONFIRMED` `CANCELED` |
| `STMT` | `StatementStatus` | `ISSUED` `CANCELED` |
| `INV`  | `InvoiceStatus`   | `DRAFT` `PUBLISHED` `PAID` `CANCELED` |
| `PAY`  | `PaymentStatus`   | `PENDING` `COMPLETED` `FAILED` |

엔티티(`SalesDealLog`) 레벨에서는 `fromStatus`가 `null`일 수 있습니다.
다만 현재 `DealLogWriteService.write(...)` 경로는 `fromStatus` 공백/null을 허용하지 않습니다.
문서 타입별 허용값 검증은 서비스 레이어에서 수행합니다.

---

## 기록 예시

**견적서(QUO) 관리자 승인 시 (`SalesDealLog`)**
```
docType      : QUO
actionType   : APPROVE
actorType    : ADMIN
fromStage    : PENDING_ADMIN   /  fromStatus : WAITING_ADMIN
toStage      : PENDING_CLIENT  /  toStatus   : WAITING_CLIENT
```

**문서 전환 시** (로그 레코드 2개 생성)
```
# 1. 원본 문서 종결
docType      : (원본 타입)  /  actionType : CONVERT
toStage      : APPROVED  /  toStatus : (전환 후 원본 문서 상태, 예: QUO는 COMPLETED)

# 2. 신규 문서 생성
docType      : (신규 타입)  /  actionType : CREATE
toStage      : CREATED  /  toStatus : (신규 문서 최초 상태)
```
