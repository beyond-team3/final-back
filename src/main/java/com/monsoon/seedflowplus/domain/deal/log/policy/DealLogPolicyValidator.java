package com.monsoon.seedflowplus.domain.deal.log.policy;

import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DealLogPolicyValidator {

    /**
     * ActorType + ActionType 허용 조합 표
     *
     * SALES_REP:
     * - CREATE, UPDATE, SUBMIT, RESUBMIT, CONVERT, APPROVE, REJECT, CONFIRM, ISSUE, PAY, EXPIRE, CANCEL
     *
     * ADMIN:
     * - CREATE, UPDATE, SUBMIT, RESUBMIT, CONVERT, APPROVE, REJECT, CONFIRM, ISSUE, PAY, EXPIRE, CANCEL
     *
     * CLIENT:
     * - CREATE, UPDATE, SUBMIT, RESUBMIT, APPROVE, REJECT, PAY, CANCEL
     *
     * SYSTEM:
     * - CREATE, UPDATE, SUBMIT, RESUBMIT, CONVERT, CONFIRM, ISSUE, PAY, EXPIRE, CANCEL
     */
    private static final Map<ActorType, Set<ActionType>> ALLOWED_ACTIONS_BY_ACTOR = createAllowedActionsByActor();

    public void validateActorAndActionOrThrow(ActorType actorType, Long actorId, ActionType actionType) {
        Objects.requireNonNull(actorType, "actorType은 null값이 될 수 없습니다.");
        Objects.requireNonNull(actionType, "actionType은 null값이 될 수 없습니다.");

        if (actorType == ActorType.SYSTEM && actorId != null) {
            // TODO: 프로젝트 표준 예외(ErrorCode/ErrorType)로 치환
            throw new IllegalArgumentException("actorType이 SYSTEM이면 actorId는 null이어야 합니다.");
        }
        if (actorType != ActorType.SYSTEM && actorId == null) {
            // TODO: 프로젝트 표준 예외(ErrorCode/ErrorType)로 치환
            throw new IllegalArgumentException("actorType이 SYSTEM이 아니면 actorId는 null일 수 없습니다.");
        }

        Set<ActionType> allowedActions = ALLOWED_ACTIONS_BY_ACTOR.getOrDefault(actorType, Set.of());
        if (!allowedActions.contains(actionType)) {
            // TODO: 프로젝트 표준 예외(ErrorCode/ErrorType)로 치환
            throw new IllegalArgumentException(
                    "허용되지 않은 actor/action 조합입니다. actorType=" + actorType + ", actionType=" + actionType
            );
        }
    }

    public void validateTargetCodeOrThrow(String targetCode) {
        if (!StringUtils.hasText(targetCode)) {
            // TODO: 프로젝트 표준 예외(ErrorCode/ErrorType)로 치환
            throw new IllegalArgumentException("targetCode는 null/blank일 수 없습니다.");
        }
    }

    private static Map<ActorType, Set<ActionType>> createAllowedActionsByActor() {
        Map<ActorType, Set<ActionType>> map = new EnumMap<>(ActorType.class);

        Set<ActionType> staffActions = EnumSet.allOf(ActionType.class);
        Set<ActionType> clientActions = EnumSet.of(
                ActionType.CREATE,
                ActionType.UPDATE,
                ActionType.SUBMIT,
                ActionType.RESUBMIT,
                ActionType.APPROVE,
                ActionType.REJECT,
                ActionType.PAY,
                ActionType.CANCEL
        );
        Set<ActionType> systemActions = EnumSet.of(
                ActionType.CREATE,
                ActionType.UPDATE,
                ActionType.SUBMIT,
                ActionType.RESUBMIT,
                ActionType.CONVERT,
                ActionType.CONFIRM,
                ActionType.ISSUE,
                ActionType.PAY,
                ActionType.EXPIRE,
                ActionType.CANCEL
        );

        map.put(ActorType.SALES_REP, Collections.unmodifiableSet(EnumSet.copyOf(staffActions)));
        map.put(ActorType.ADMIN, Collections.unmodifiableSet(EnumSet.copyOf(staffActions)));
        map.put(ActorType.CLIENT, Collections.unmodifiableSet(EnumSet.copyOf(clientActions)));
        map.put(ActorType.SYSTEM, Collections.unmodifiableSet(EnumSet.copyOf(systemActions)));

        return Collections.unmodifiableMap(map);
    }
}
