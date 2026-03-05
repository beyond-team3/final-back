package com.monsoon.seedflowplus.domain.deal.log.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.log.entity.DealLogDetail;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.common.error.DealException;
import com.monsoon.seedflowplus.domain.deal.common.error.DealErrorCode;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.log.entity.SalesDealLog;
import com.monsoon.seedflowplus.domain.deal.log.policy.DealLogPolicyValidator;
import com.monsoon.seedflowplus.domain.deal.log.repository.DealLogDetailRepository;
import com.monsoon.seedflowplus.domain.deal.log.repository.SalesDealLogRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DealLogWriteService {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final SalesDealLogRepository salesDealLogRepository;
    private final DealLogDetailRepository dealLogDetailRepository;
    private final DealLogPolicyValidator dealLogPolicyValidator;
    private final ObjectMapper objectMapper;

    /**
     * DealLog 단일 진입점.
     *
     * 호출 계약:
     * 1) 상태 변경 이벤트는 문서 상태 저장과 같은 트랜잭션 안에서 호출해야 한다.
     * 2) 상태 전이 검증/스냅샷 동기화는 DealPipelineFacade에서 수행한다.
     * 3) targetCode는 null/blank가 아니어야 한다.
     * 4) 성공 시 SalesDealLog/DealLogDetail만 저장한다.
     */
    @Transactional
    public SalesDealLog write(
            SalesDeal deal,
            DealType docType,
            Long refId,
            String targetCode,
            DealStage fromStage,
            DealStage toStage,
            String fromStatus,
            String toStatus,
            ActionType actionType,
            LocalDateTime actionAt,
            ActorType actorType,
            Long actorId
    ) {
        return write(
                deal, docType, refId, targetCode, fromStage, toStage,
                fromStatus, toStatus, actionType, actionAt, actorType, actorId,
                null, (String) null
        );
    }

    @Transactional
    public SalesDealLog write(
            SalesDeal deal,
            DealType docType,
            Long refId,
            String targetCode,
            DealStage fromStage,
            DealStage toStage,
            String fromStatus,
            String toStatus,
            ActionType actionType,
            LocalDateTime actionAt,
            ActorType actorType,
            Long actorId,
            String reason,
            String diffJson
    ) {
        return writeInternal(
                deal,
                docType,
                refId,
                targetCode,
                fromStage,
                toStage,
                fromStatus,
                toStatus,
                actionType,
                actionAt,
                actorType,
                actorId,
                reason,
                diffJson
        );
    }

    @Transactional
    public SalesDealLog write(
            SalesDeal deal,
            DealType docType,
            Long refId,
            String targetCode,
            DealStage fromStage,
            DealStage toStage,
            String fromStatus,
            String toStatus,
            ActionType actionType,
            LocalDateTime actionAt,
            ActorType actorType,
            Long actorId,
            String reason,
            List<DiffField> diffFields
    ) {
        return write(
                deal,
                docType,
                refId,
                targetCode,
                fromStage,
                toStage,
                fromStatus,
                toStatus,
                actionType,
                actionAt,
                actorType,
                actorId,
                reason,
                buildDiffJson(diffFields)
        );
    }

    private SalesDealLog writeInternal(
            SalesDeal deal,
            DealType docType,
            Long refId,
            String targetCode,
            DealStage fromStage,
            DealStage toStage,
            String fromStatus,
            String toStatus,
            ActionType actionType,
            LocalDateTime actionAt,
            ActorType actorType,
            Long actorId,
            String reason,
            String diffJson
    ) {
        validateRequired(deal, docType, refId, toStage, fromStatus, toStatus, actionType, actorType);
        dealLogPolicyValidator.validateTargetCodeOrThrow(targetCode);
        dealLogPolicyValidator.validateActorAndActionOrThrow(actorType, actorId, actionType);

        LocalDateTime resolvedActionAt = actionAt != null ? actionAt : nowKst();

        SalesDealLog savedLog = createLog(
                deal,
                docType,
                refId,
                targetCode,
                fromStage,
                toStage,
                fromStatus,
                toStatus,
                actionType,
                resolvedActionAt,
                actorType,
                actorId
        );

        createDetailIfAny(savedLog, reason, diffJson);

        return savedLog;
    }

    /**
     * CONVERT 예외 규칙:
     * - 원본 로그 1건(actionType=CONVERT, toStage=APPROVED)
     * - 신규 로그 1건(actionType=CREATE, toStage=CREATED)
     * - 스냅샷 동기화는 DealPipelineFacade에서 최종 결과(신규 문서) 기준으로 1회 수행
     */
    @Transactional
    public ConvertLogPair writeConvertPair(ConvertLogRequest original, ConvertLogRequest created) {
        Objects.requireNonNull(original, "original은 null값이 될 수 없습니다.");
        Objects.requireNonNull(created, "created는 null값이 될 수 없습니다.");
        validateConvertPairConsistency(original, created);

        LocalDateTime actionAt = original.actionAt() != null ? original.actionAt()
                : (created.actionAt() != null ? created.actionAt() : nowKst());

        SalesDealLog originalLog = createConvertLog(original, actionAt);
        SalesDealLog createdLog = createCreateLog(created, actionAt);

        return new ConvertLogPair(originalLog, createdLog);
    }

    private SalesDealLog createConvertLog(ConvertLogRequest request, LocalDateTime actionAt) {
        return writeInternal(
                request.deal(),
                request.docType(),
                request.refId(),
                request.targetCode(),
                request.fromStage(),
                DealStage.APPROVED,
                request.fromStatus(),
                request.toStatus(),
                ActionType.CONVERT,
                actionAt,
                request.actorType(),
                request.actorId(),
                request.reason(),
                request.diffJson()
        );
    }

    private SalesDealLog createCreateLog(ConvertLogRequest request, LocalDateTime actionAt) {
        return writeInternal(
                request.deal(),
                request.docType(),
                request.refId(),
                request.targetCode(),
                request.fromStage(),
                DealStage.CREATED,
                request.fromStatus(),
                request.toStatus(),
                ActionType.CREATE,
                actionAt,
                request.actorType(),
                request.actorId(),
                request.reason(),
                request.diffJson()
        );
    }

    private SalesDealLog createLog(
            SalesDeal deal,
            DealType docType,
            Long refId,
            String targetCode,
            DealStage fromStage,
            DealStage toStage,
            String fromStatus,
            String toStatus,
            ActionType actionType,
            LocalDateTime actionAt,
            ActorType actorType,
            Long actorId
    ) {
        return salesDealLogRepository.save(
                SalesDealLog.builder()
                        .deal(deal)
                        .client(null)
                        .docType(docType)
                        .refId(refId)
                        .targetCode(targetCode)
                        .fromStage(fromStage)
                        .toStage(toStage)
                        .fromStatus(fromStatus)
                        .toStatus(toStatus)
                        .actionType(actionType)
                        .actionAt(actionAt)
                        .actorType(actorType)
                        .actorId(actorId)
                        .build()
        );
    }

    private void createDetailIfAny(SalesDealLog log, String reason, String diffJson) {
        if (!StringUtils.hasText(reason) && !StringUtils.hasText(diffJson)) {
            return;
        }
        if (isExplicitEmptyFieldsDiff(diffJson)) {
            return;
        }
        if (StringUtils.hasText(reason) || StringUtils.hasText(diffJson)) {
            dealLogDetailRepository.save(
                    DealLogDetail.builder()
                            .dealLog(log)
                            .reason(reason)
                            .diffJson(diffJson)
                            .build()
            );
        }
    }

    private void validateRequired(
            SalesDeal deal,
            DealType docType,
            Long refId,
            DealStage toStage,
            String fromStatus,
            String toStatus,
            ActionType actionType,
            ActorType actorType
    ) {
        Objects.requireNonNull(deal, "deal은 null값이 될 수 없습니다.");
        Objects.requireNonNull(docType, "docType은 null값이 될 수 없습니다.");
        Objects.requireNonNull(refId, "refId는 null값이 될 수 없습니다.");
        Objects.requireNonNull(toStage, "toStage는 null값이 될 수 없습니다.");
        if (!StringUtils.hasText(fromStatus)) {
            throw new DealException(DealErrorCode.FROM_STATUS_REQUIRED);
        }
        if (!StringUtils.hasText(toStatus)) {
            throw new DealException(DealErrorCode.TO_STATUS_REQUIRED);
        }
        Objects.requireNonNull(actionType, "actionType은 null값이 될 수 없습니다.");
        Objects.requireNonNull(actorType, "actorType은 null값이 될 수 없습니다.");
    }

    private String buildDiffJson(List<DiffField> fields) {
        if (fields == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(Map.of("fields", fields));
        } catch (JsonProcessingException e) {
            // TODO: 프로젝트 표준 예외(ErrorCode/ErrorType)로 치환
            throw new IllegalArgumentException("diffJson 직렬화에 실패했습니다.", e);
        }
    }

    private boolean isExplicitEmptyFieldsDiff(String diffJson) {
        if (!StringUtils.hasText(diffJson)) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(diffJson);
            JsonNode fields = root.path("fields");
            return fields.isArray() && fields.isEmpty();
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private LocalDateTime nowKst() {
        return LocalDateTime.now(KST_ZONE_ID);
    }

    private void validateConvertPairConsistency(ConvertLogRequest original, ConvertLogRequest created) {
        if (original.actionAt() != null && created.actionAt() != null
                && !original.actionAt().equals(created.actionAt())) {
            throw new DealException(DealErrorCode.CONVERT_ACTION_AT_MISMATCH);
        }
        if (original.actorType() != null && created.actorType() != null
                && original.actorType() != created.actorType()) {
            throw new DealException(DealErrorCode.CONVERT_ACTOR_TYPE_MISMATCH);
        }
        if (original.actorId() != null && created.actorId() != null
                && !original.actorId().equals(created.actorId())) {
            throw new DealException(DealErrorCode.CONVERT_ACTOR_ID_MISMATCH);
        }
    }

    public record DiffField(
            String field,
            String label,
            Object before,
            Object after,
            String type
    ) {
    }

    public record ConvertLogRequest(
            SalesDeal deal,
            DealType docType,
            Long refId,
            String targetCode,
            DealStage fromStage,
            String fromStatus,
            String toStatus,
            LocalDateTime actionAt,
            ActorType actorType,
            Long actorId,
            String reason,
            String diffJson
    ) {
    }

    public record ConvertLogPair(SalesDealLog originalLog, SalesDealLog createdLog) {
    }
}
