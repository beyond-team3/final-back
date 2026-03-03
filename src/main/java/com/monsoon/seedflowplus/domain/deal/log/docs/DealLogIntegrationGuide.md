# DealLog 연결 가이드 (조회/기록 규칙 준수)

이 문서는 `quotation`, `contract`, `invoice`, `payment` 등 문서 서비스의 **상태 변경 메서드**에서  
`SalesDealLog`를 어떻게 호출해야 하는지 설명한다.

실제 연결 코드는 이 문서에서 수행하지 않는다.  
아래 순서를 서비스 메서드에 그대로 적용한다.

---

## 1. 공통 호출 순서

1. 현재 상태 조회 (`fromStatus`, `fromStage`)
2. 상태 전이 검증 (`DocStatusTransitionValidator`)
3. 문서 상태 변경 및 저장 (`toStatus`, `toStage`)
4. 로그 기록 (`dealLogWriteService.recordStatusChange(...)` 역할 메서드 호출)

핵심:
- 상태 변경 실패 시 예외를 던지고 롤백한다.
- 예외가 발생하면 DealLog를 남기지 않는다.
- stage 변화가 없어도 `toStatus`가 바뀌면 로그를 남긴다.

---

## 2. 서비스 메서드 템플릿 예시

```java
@Transactional
public void changeStatusExample(Long docId, ActionType actionType, ActorType actorType, Long actorId) {
    // 1) 현재 상태 조회
    QuotationHeader doc = quotationRepository.findById(docId)
            .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));

    String fromStatus = doc.getStatus().name();
    DealStage fromStage = mapStage(doc.getStatus()); // 기존 매핑 함수 사용

    // 요청 목표 상태 결정
    QuotationStatus nextStatus = decideNextStatus(doc.getStatus(), actionType);
    String toStatus = nextStatus.name();
    DealStage toStage = mapStage(nextStatus);

    // 2) 상태 전이 검증
    docStatusTransitionValidator.validateOrThrow(
            DealType.QUO,
            fromStatus,
            actionType,
            toStatus
    );

    // 3) 상태 변경 저장
    doc.changeStatus(nextStatus);
    quotationRepository.save(doc);

    // 4) 로그 기록 (recordStatusChange 역할)
    // - 현재 구현체가 write(...) 라면 write(...)를 래핑한 recordStatusChange(...)를 서비스에 추가해 사용 권장
    dealLogWriteService.write(
            doc.getDeal(),                  // SalesDeal
            DealType.QUO,                   // docType
            doc.getId(),                    // refId
            doc.getQuotationCode(),         // targetCode
            fromStage,
            toStage,
            fromStatus,
            toStatus,
            actionType,
            LocalDateTime.now(ZoneId.of("Asia/Seoul")), // KST
            actorType,
            actorId
    );
}
```

---

## 3. 문서별 적용 포인트

`QuotationService`:
- 승인/반려/재제출/만료 전환 전에 검증 호출
- `fromStatus=doc.getStatus().name()` 기반

`ContractService`:
- 관리자 승인, 거래처 승인, 반려, 재제출, 만료에 동일 패턴 적용

`InvoiceService`:
- 발행(`ISSUE`), 결제완료(`PAY`), 취소(`CANCEL`) 전환 전에 검증

`PaymentService`:
- 결제완료(`PAY`) 또는 실패/취소(`CANCEL`) 전환 전에 검증

---

## 4. CONVERT 처리 예시

전환은 같은 트랜잭션에서 로그 2건을 기록한다.

```java
@Transactional
public void convertExample(...) {
    // 상태 전이 검증 + 원본/신규 문서 저장이 선행됨

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
}
```

규칙:
- 원본 문서 로그: `actionType=CONVERT`, `toStage=APPROVED`
- 신규 문서 로그: `actionType=CREATE`, `toStage=CREATED`

---

## 5. 체크리스트

- `fromStatus`는 상태 변경 로그(예: `UPDATE`, `TRANSITION`)에서만 `null` 금지이며, 최초 생성 로그(`CREATE`)에서는 `fromStatus=null` 허용
- `toStatus`는 모든 로그에서 필수(`null` 금지)
- `ActorType.SYSTEM`이면 `actorId=null` 유지 (`SYSTEM`이 아니면 `actorId` 필수)
- 로그 생성은 Controller가 아닌 Service 내부에서만 수행
- 상태 변경이 실제로 반영된 트랜잭션 안에서 로그를 기록
