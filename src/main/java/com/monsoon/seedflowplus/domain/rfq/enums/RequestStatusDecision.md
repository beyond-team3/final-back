# RequestStatus 전환 결정 문서

## 배경

`QuotationRequestStatus` → `RequestStatus` 교체 시,
기존 `COMPLETED` 값에 대한 매핑 방안 결정이 필요하다.

| 기존 (`QuotationRequestStatus`) | 신규 (`RequestStatus`) |
|---|---|
| PENDING   | PENDING   |
| REVIEWING | REVIEWING |
| COMPLETED | **없음**  |

---

## 선택지 비교

### Option A — `RequestStatus`에 `CONVERTED` 추가

RFQ가 QUO로 전환 완료된 상태를 명시적으로 표현한다.

```java
public enum RequestStatus implements DocumentStatus {
    PENDING,    // 견적 대기
    REVIEWING,  // 검토 중
    CONVERTED,  // QUO 전환 완료 (CONVERT 액션과 함께 기록)
    CANCELED    // 취소
}
```

**장점**
- RFQ 상태만 봐도 전환 여부를 즉시 파악 가능
- 기존 `COMPLETED` → `CONVERTED` 1:1 마이그레이션으로 의미가 명확
- 목록 화면에서 "전환됨" 뱃지 등 UI 표현이 쉬움

**단점**
- `RequestStatus` 값이 4개로 늘어남
- SalesHistory `ActionType.CONVERT` 기록과 상태가 이중으로 관리됨
- RFQ 외 다른 문서 타입엔 없는 패턴 (일관성 낮음)

---

### Option B — 상태 유지 (`REVIEWING` 그대로)

전환 시 RFQ status를 변경하지 않고, SalesHistory의 `ActionType=CONVERT` 기록으로만 전환 여부를 판단한다.

```
RFQ 전환 흐름:
  REVIEWING (status 유지)
  + SalesHistory.actionType = CONVERT  ← 전환 근거
```

**장점**
- `RequestStatus` enum을 가장 단순하게 유지 (3개)
- 상태 변경 없이 히스토리만 추가하므로 엔티티 업데이트 불필요
- 파이프라인 아키텍처 원칙에 가장 충실 (상태보다 이벤트로 추적)

**단점**
- RFQ 목록 조회 시 "전환된 건" 필터링에 SalesHistory JOIN 필요
- 기존 `COMPLETED` 레코드를 `REVIEWING`으로 마이그레이션 필요
  (의미가 다른 값으로의 변환이므로 데이터 정합성 주의)

---

### Option C — `CANCELED`로 통일

전환·취소 모두 `CANCELED`로 처리한다.

```
RFQ 전환 → CANCELED
RFQ 취소 → CANCELED
```

**장점**
- enum 값이 가장 적음 (3개)
- 구현이 단순

**단점**
- "전환됨"과 "취소됨"을 상태만으로 구분 불가
- SalesHistory 없이는 전환 여부를 알 수 없어 조회 복잡도 증가
- 의미상 부정확 (취소된 것이 아닌데 CANCELED로 기록)
- **권장하지 않음**

---

## 권장안

| 우선순위 | 선택지 | 이유 |
|---|---|---|
| **1순위** | **Option A (CONVERTED 추가)** | RFQ의 전환 완료가 핵심 비즈니스 이벤트이므로 명시적 상태가 가독성·운영 면에서 유리. 기존 COMPLETED와 1:1 대응으로 마이그레이션도 단순. |
| 2순위 | Option B (REVIEWING 유지) | 이벤트 기반 설계에 충실하나, RFQ 목록 조회 시 추가 JOIN 비용 발생. |

---

## 마이그레이션 영향

| 선택지 | DB 변경 | 기존 `COMPLETED` 레코드 처리 |
|---|---|---|
| A (CONVERTED 추가) | `request_status` 컬럼 허용값에 `CONVERTED` 추가 | `COMPLETED` → `CONVERTED` UPDATE |
| B (REVIEWING 유지) | 없음 | `COMPLETED` → `REVIEWING` UPDATE (의미 손실 주의) |
| C (CANCELED)       | 없음 | `COMPLETED` → `CANCELED` UPDATE (의미 오염) |
