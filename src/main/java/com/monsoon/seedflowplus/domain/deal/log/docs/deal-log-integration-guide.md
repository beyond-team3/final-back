# DealLog 연결 가이드 (실제 구현 기준)

이 문서는 문서 서비스(견적/계약/주문/명세서/청구서/결제)의 상태 변경 시
`SalesDealLog`를 남기는 표준 흐름을 정리합니다.

---

## 1. 공통 호출 순서

1. 현재 상태 조회 (`fromStatus`, `fromStage`)
2. 상태 전이 검증 (`DocStatusTransitionValidator.validateOrThrow(...)`)
3. 문서 상태 변경 및 저장 (`toStatus`, `toStage`)
4. 로그 기록 (`DealLogWriteService.write(...)` 또는 `writeConvertPair(...)`)

핵심:
- 상태 전이 검증/저장 실패 시 예외로 롤백되어 로그도 남지 않습니다.
- `stage` 변화가 없어도 `toStatus`가 바뀌면 로그를 남깁니다.

---

## 2. 서비스 메서드 템플릿 예시

```java
@Transactional
public void changeStatusExample(Long docId, ActionType actionType, ActorType actorType, Long actorId) {
    // 1) 현재 상태 조회
    var doc = repository.findById(docId)
            .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));

    String fromStatus = doc.getStatus().name();
    DealStage fromStage = mapStage(doc.getStatus());

    // 목표 상태 계산
    var nextStatus = decideNextStatus(doc.getStatus(), actionType);
    String toStatus = nextStatus.name();
    DealStage toStage = mapStage(nextStatus);

    // 2) 상태 전이 검증
    docStatusTransitionValidator.validateOrThrow(
            DealType.QUO,   // 실제 문서 타입으로 교체
            fromStatus,
            actionType,
            toStatus
    );

    // 3) 상태 변경 저장
    doc.changeStatus(nextStatus);
    repository.save(doc);

    // 4) 로그 기록
    dealLogWriteService.write(
            doc.getDeal(),
            DealType.QUO,   // 실제 문서 타입으로 교체
            doc.getId(),
            doc.getCode(),
            fromStage,
            toStage,
            fromStatus,
            toStatus,
            actionType,
            null,           // null이면 내부에서 KST now 사용
            actorType,
            actorId
    );
}
```

---

## 3. 문서별 상태 전이 규칙 (현재 `DocStatusTransitionPolicy`)

`RFQ`:
- `PENDING --SUBMIT--> REVIEWING`
- `REVIEWING --APPROVE--> COMPLETED`
- `REVIEWING --REJECT--> PENDING`

`QUO`:
- `WAITING_ADMIN --APPROVE--> WAITING_CLIENT`
- `WAITING_ADMIN --REJECT--> REJECTED_ADMIN`
- `REJECTED_ADMIN --RESUBMIT--> WAITING_ADMIN`
- `WAITING_CLIENT --APPROVE--> FINAL_APPROVED`
- `WAITING_CLIENT --REJECT--> REJECTED_CLIENT`
- `REJECTED_CLIENT --RESUBMIT--> WAITING_CLIENT`
- `FINAL_APPROVED --CONVERT--> COMPLETED`
- `WAITING_CONTRACT --CONVERT--> COMPLETED`
- `WAITING_ADMIN|WAITING_CLIENT --EXPIRE--> EXPIRED`

`CNT`:
- `WAITING_ADMIN --APPROVE--> WAITING_CLIENT`
- `WAITING_ADMIN --REJECT--> REJECTED_ADMIN`
- `REJECTED_ADMIN --RESUBMIT--> WAITING_ADMIN`
- `WAITING_CLIENT --APPROVE--> COMPLETED`
- `WAITING_CLIENT --REJECT--> REJECTED_CLIENT`
- `REJECTED_CLIENT --RESUBMIT--> WAITING_CLIENT`
- `WAITING_ADMIN|WAITING_CLIENT --EXPIRE--> EXPIRED`

`ORD`:
- `PENDING --CONFIRM--> CONFIRMED`
- `PENDING --CANCEL--> CANCELED`

`STMT`:
- `ISSUED --CANCEL--> CANCELED`

`INV`:
- `DRAFT --ISSUE--> PUBLISHED`
- `DRAFT --CANCEL--> CANCELED`
- `PUBLISHED --PAY--> PAID`
- `PUBLISHED --CANCEL--> CANCELED`

`PAY`:
- `PENDING --PAY--> COMPLETED`
- `PENDING --CANCEL--> FAILED`

---

## 4. CONVERT 처리 예시

문서 전환은 같은 트랜잭션에서 `writeConvertPair(...)`로 로그 2건을 남깁니다.

```java
dealLogWriteService.writeConvertPair(
        new DealLogWriteService.ConvertLogRequest(
                originalDeal, originalType, originalRefId, originalCode,
                originalFromStage, originalFromStatus, originalToStatus,
                null, actorType, actorId, "convert original", diffJsonOriginal
        ),
        new DealLogWriteService.ConvertLogRequest(
                newDeal, newType, newRefId, newCode,
                newFromStage, newFromStatus, newToStatus,
                null, actorType, actorId, "create converted doc", diffJsonNew
        )
);
```

규칙:
- 원본 문서 로그: `actionType=CONVERT`, `toStage=APPROVED`
- 신규 문서 로그: `actionType=CREATE`, `toStage=CREATED`

---

## 5. 체크리스트

- `DealLogWriteService.write(...)` 경로에서는 `fromStatus`/`toStatus` 모두 필수(공백/null 불가)
- `toStage`, `docType`, `refId`, `actionType`, `actorType`도 필수
- `actionAt`은 `null`이면 `Asia/Seoul` 기준 현재 시각으로 저장
- `ActorType.SYSTEM`이면 `actorId`는 반드시 `null`
- `ActorType.SYSTEM`이 아니면 `actorId`는 반드시 필요
- 로그 생성은 Controller가 아닌 Service 내부 트랜잭션에서 수행
