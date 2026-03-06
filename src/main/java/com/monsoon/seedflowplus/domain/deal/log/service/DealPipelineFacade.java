package com.monsoon.seedflowplus.domain.deal.log.service;

import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.log.dto.DealDiffField;
import com.monsoon.seedflowplus.domain.deal.log.entity.SalesDealLog;
import com.monsoon.seedflowplus.domain.notification.event.DealStatusChangedEvent;
import com.monsoon.seedflowplus.domain.notification.event.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DealPipelineFacade {

    private final DealLogWriteService dealLogWriteService;
    private final DocStatusTransitionValidator docStatusTransitionValidator;
    private final NotificationEventPublisher notificationEventPublisher;
    private final UserRepository userRepository;

    @Transactional
    public SalesDealLog recordAndSync(
            SalesDeal deal,
            DealType docType,
            Long refId,
            String targetCode,
            DealStage fromStage,
            DealStage toStage,
            String fromStatus,
            String toStatus,
            ActionType actionType,
            java.time.LocalDateTime actionAt,
            ActorType actorType,
            Long actorId,
            String reason,
            java.util.List<DealLogWriteService.DiffField> diffFields
    ) {
        Objects.requireNonNull(deal, "deal must not be null");
        Objects.requireNonNull(docType, "docType must not be null");
        Objects.requireNonNull(refId, "refId must not be null");
        Objects.requireNonNull(actionType, "actionType must not be null");
        Objects.requireNonNull(actorType, "actorType must not be null");
        Objects.requireNonNull(toStage, "toStage must not be null");
        Objects.requireNonNull(toStatus, "toStatus must not be null");

        validateTransitionIfNeeded(docType, fromStatus, toStatus, actionType);

        SalesDealLog savedLog = dealLogWriteService.write(
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
                diffFields
        );

        deal.updateSnapshot(
                toStage,
                toStatus,
                docType,
                refId,
                targetCode,
                savedLog.getActionAt()
        );
        publishDealStatusChangedEventIfNeeded(deal, fromStatus, toStatus, savedLog.getActionAt());
        return savedLog;
    }

    @Transactional
    public SalesDealLog recordAndSyncWithPublicDiffs(
            SalesDeal deal,
            DealType docType,
            Long refId,
            String targetCode,
            DealStage fromStage,
            DealStage toStage,
            String fromStatus,
            String toStatus,
            ActionType actionType,
            java.time.LocalDateTime actionAt,
            ActorType actorType,
            Long actorId,
            String reason,
            Collection<DealDiffField> diffFields
    ) {
        if (diffFields != null && diffFields.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("diffFields contains null element");
        }
        List<DealLogWriteService.DiffField> internalDiffFields = diffFields == null
                ? null
                : diffFields.stream()
                .map(field -> new DealLogWriteService.DiffField(
                        field.field(),
                        field.label(),
                        field.before(),
                        field.after(),
                        field.type()
                ))
                .toList();
        return recordAndSync(
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
                internalDiffFields
        );
    }

    @Transactional
    public DealLogWriteService.ConvertLogPair recordConvertAndSync(
            DealLogWriteService.ConvertLogRequest original,
            DealLogWriteService.ConvertLogRequest created
    ) {
        Objects.requireNonNull(original, "original must not be null");
        Objects.requireNonNull(created, "created must not be null");
        Objects.requireNonNull(original.docType(), "original.docType must not be null");
        Objects.requireNonNull(original.fromStatus(), "original.fromStatus must not be null");
        Objects.requireNonNull(original.toStatus(), "original.toStatus must not be null");
        Objects.requireNonNull(created.deal(), "created.deal must not be null");
        Objects.requireNonNull(created.toStatus(), "created.toStatus must not be null");
        Objects.requireNonNull(created.docType(), "created.docType must not be null");
        Objects.requireNonNull(created.refId(), "created.refId must not be null");
        Objects.requireNonNull(created.targetCode(), "created.targetCode must not be null");

        validateTransitionIfNeeded(
                original.docType(),
                original.fromStatus(),
                original.toStatus(),
                ActionType.CONVERT
        );

        DealLogWriteService.ConvertLogPair pair = dealLogWriteService.writeConvertPair(original, created);
        created.deal().updateSnapshot(
                DealStage.CREATED,
                created.toStatus(),
                created.docType(),
                created.refId(),
                created.targetCode(),
                pair.createdLog().getActionAt()
        );
        publishDealStatusChangedEventIfNeeded(
                created.deal(),
                original.fromStatus(),
                created.toStatus(),
                pair.createdLog().getActionAt()
        );
        return pair;
    }

    public void validateTransitionOrThrow(DealType docType, String fromStatus, ActionType actionType, String toStatus) {
        docStatusTransitionValidator.validateOrThrow(docType, fromStatus, actionType, toStatus);
    }

    private void validateTransitionIfNeeded(DealType docType, String fromStatus, String toStatus, ActionType actionType) {
        if (!shouldValidateTransition(actionType, fromStatus, toStatus)) {
            return;
        }
        docStatusTransitionValidator.validateOrThrow(docType, fromStatus, actionType, toStatus);
    }

    private boolean shouldValidateTransition(ActionType actionType, String fromStatus, String toStatus) {
        if (!StringUtils.hasText(fromStatus) || !StringUtils.hasText(toStatus)) {
            return false;
        }
        if (fromStatus.equals(toStatus)) {
            return false;
        }
        return actionType != ActionType.CREATE;
    }

    private void publishDealStatusChangedEventIfNeeded(
            SalesDeal deal,
            String fromStatus,
            String toStatus,
            java.time.LocalDateTime occurredAt
    ) {
        if (!StringUtils.hasText(fromStatus) || !StringUtils.hasText(toStatus) || fromStatus.equals(toStatus)) {
            return;
        }
        if (deal.getOwnerEmp() == null || deal.getOwnerEmp().getId() == null) {
            return;
        }
        userRepository.findByEmployeeId(deal.getOwnerEmp().getId())
                .map(user -> user.getId())
                .ifPresent(userId -> notificationEventPublisher.publish(
                        new DealStatusChangedEvent(
                                userId,
                                deal.getId(),
                                fromStatus,
                                toStatus,
                                occurredAt
                        )
                ));
    }
}
