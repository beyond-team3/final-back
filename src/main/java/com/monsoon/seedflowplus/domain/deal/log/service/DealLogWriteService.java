package com.monsoon.seedflowplus.domain.deal.log.service;

import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.log.entity.DealLogDetail;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.common.error.DealException;
import com.monsoon.seedflowplus.domain.deal.common.error.DealErrorCode;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.log.entity.SalesDealLog;
import com.monsoon.seedflowplus.domain.deal.log.repository.DealLogDetailRepository;
import com.monsoon.seedflowplus.domain.deal.log.repository.SalesDealLogRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
                null, null
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
        validateRequired(deal, docType, refId, toStage, fromStatus, toStatus, actionType, actorType);
        validateActor(actorType, actorId);

        SalesDealLog savedLog = salesDealLogRepository.save(
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
                        .actionAt(actionAt != null ? actionAt : nowKst())
                        .actorType(actorType)
                        .actorId(actorId)
                        .build()
        );

        if (StringUtils.hasText(reason) || StringUtils.hasText(diffJson)) {
            dealLogDetailRepository.save(
                    DealLogDetail.builder()
                            .dealLog(savedLog)
                            .reason(reason)
                            .diffJson(diffJson)
                            .build()
            );
        }

        return savedLog;
    }

    @Transactional
    public SalesDealLog convertOriginal(
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
        return write(
                deal, docType, refId, targetCode, fromStage, DealStage.APPROVED,
                fromStatus, toStatus, ActionType.CONVERT, actionAt, actorType, actorId,
                reason, diffJson
        );
    }

    @Transactional
    public SalesDealLog createNew(
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
        return write(
                deal, docType, refId, targetCode, fromStage, DealStage.CREATED,
                fromStatus, toStatus, ActionType.CREATE, actionAt, actorType, actorId,
                reason, diffJson
        );
    }

    @Transactional
    public ConvertLogPair writeConvertPair(ConvertLogRequest original, ConvertLogRequest created) {
        Objects.requireNonNull(original, "original은 null값이 될 수 없습니다.");
        Objects.requireNonNull(created, "created는 null값이 될 수 없습니다.");
        validateConvertPairConsistency(original, created);

        LocalDateTime actionAt = original.actionAt() != null ? original.actionAt()
                : (created.actionAt() != null ? created.actionAt() : nowKst());

        SalesDealLog originalLog = convertOriginal(
                original.deal(),
                original.docType(),
                original.refId(),
                original.targetCode(),
                original.fromStage(),
                original.fromStatus(),
                original.toStatus(),
                actionAt,
                original.actorType(),
                original.actorId(),
                original.reason(),
                original.diffJson()
        );

        SalesDealLog createdLog = createNew(
                created.deal(),
                created.docType(),
                created.refId(),
                created.targetCode(),
                created.fromStage(),
                created.fromStatus(),
                created.toStatus(),
                actionAt,
                created.actorType(),
                created.actorId(),
                created.reason(),
                created.diffJson()
        );

        return new ConvertLogPair(originalLog, createdLog);
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

    private void validateActor(ActorType actorType, Long actorId) {
        if (actorType == ActorType.SYSTEM) {
            if (actorId != null) {
                throw new DealException(DealErrorCode.SYSTEM_ACTOR_ID_MUST_BE_NULL);
            }
            return;
        }
        if (actorId == null) {
            throw new DealException(DealErrorCode.NON_SYSTEM_ACTOR_ID_REQUIRED);
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
