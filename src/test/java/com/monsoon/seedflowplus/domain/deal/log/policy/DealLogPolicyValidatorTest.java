package com.monsoon.seedflowplus.domain.deal.log.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.error.DealException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

class DealLogPolicyValidatorTest {

    private final DealLogPolicyValidator validator = new DealLogPolicyValidator();

    @ParameterizedTest(name = "{index}: allow actor={0}, action={2}")
    @MethodSource("allowedActorActionCombinations")
    void shouldAllowConfiguredActorActionCombinations(ActorType actorType, Long actorId, ActionType actionType) {
        assertDoesNotThrow(() -> validator.validateActorAndActionOrThrow(actorType, actorId, actionType));
    }

    @ParameterizedTest(name = "{index}: reject actor={0}, action={2}")
    @MethodSource("deniedActorActionCombinations")
    void shouldRejectInvalidActorActionCombinations(ActorType actorType, Long actorId, ActionType actionType) {
        assertThrows(DealException.class, () -> validator.validateActorAndActionOrThrow(actorType, actorId, actionType));
    }

    @Test
    void shouldRejectWhenSystemActorHasActorId() {
        // 정책: ActorType.SYSTEM 인 경우 actorId는 반드시 null 이어야 한다.
        assertThrows(
                DealException.class,
                () -> validator.validateActorAndActionOrThrow(ActorType.SYSTEM, 1L, ActionType.SUBMIT)
        );
    }

    @Test
    void shouldRejectWhenSystemApproves() {
        // 정책: SYSTEM + APPROVE 조합은 허용되지 않는다.
        assertThrows(
                DealException.class,
                () -> validator.validateActorAndActionOrThrow(ActorType.SYSTEM, null, ActionType.APPROVE)
        );
    }

    @Test
    void shouldRejectWhenNonSystemActorHasNullActorId() {
        assertThrows(
                DealException.class,
                () -> validator.validateActorAndActionOrThrow(ActorType.SALES_REP, null, ActionType.SUBMIT)
        );
    }

    @Test
    void shouldRejectWhenNonSystemActorHasNonPositiveActorId() {
        assertThrows(
                DealException.class,
                () -> validator.validateActorAndActionOrThrow(ActorType.SALES_REP, 0L, ActionType.SUBMIT)
        );
        assertThrows(
                DealException.class,
                () -> validator.validateActorAndActionOrThrow(ActorType.CLIENT, -1L, ActionType.PAY)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidTargetCodes")
    void shouldRejectBlankTargetCode(String targetCode) {
        assertThrows(DealException.class, () -> validator.validateTargetCodeOrThrow(targetCode));
    }

    @Test
    void shouldAllowNonBlankTargetCode() {
        assertDoesNotThrow(() -> validator.validateTargetCodeOrThrow("QUO-2026-0001"));
    }

    private static Stream<Arguments> allowedActorActionCombinations() {
        return allActorActionCombinations()
                .filter(args -> isAllowed((ActorType) args.get()[0], (ActionType) args.get()[2]));
    }

    private static Stream<Arguments> deniedActorActionCombinations() {
        return allActorActionCombinations()
                .filter(args -> !isAllowed((ActorType) args.get()[0], (ActionType) args.get()[2]));
    }

    private static Stream<Arguments> allActorActionCombinations() {
        return Stream.of(ActorType.values())
                .flatMap(actorType -> Stream.of(ActionType.values())
                        .map(actionType -> Arguments.of(actorType, actorIdFor(actorType), actionType)));
    }

    private static Long actorIdFor(ActorType actorType) {
        return actorType == ActorType.SYSTEM ? null : 1L;
    }

    private static boolean isAllowed(ActorType actorType, ActionType actionType) {
        Map<ActorType, Set<ActionType>> allowedByActor = new EnumMap<>(ActorType.class);
        Set<ActionType> staffActions = EnumSet.of(
                ActionType.CREATE,
                ActionType.UPDATE,
                ActionType.SUBMIT,
                ActionType.RESUBMIT,
                ActionType.CONVERT,
                ActionType.APPROVE,
                ActionType.REJECT,
                ActionType.CONFIRM,
                ActionType.ISSUE,
                ActionType.PAY,
                ActionType.EXPIRE,
                ActionType.CANCEL
        );
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
        allowedByActor.put(ActorType.SALES_REP, staffActions);
        allowedByActor.put(ActorType.ADMIN, staffActions);
        allowedByActor.put(ActorType.CLIENT, clientActions);
        allowedByActor.put(ActorType.SYSTEM, systemActions);
        Set<ActionType> allowed = allowedByActor.get(actorType);
        assertTrue(allowed != null);
        return allowed.contains(actionType);
    }

    private static Stream<String> invalidTargetCodes() {
        return Stream.of(null, "", " ", "   ");
    }
}
