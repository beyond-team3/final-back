package com.monsoon.seedflowplus.domain.deal.log.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
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
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateActorAndActionOrThrow(actorType, actorId, actionType)
        );
    }

    @Test
    void shouldRejectWhenSystemActorHasActorId() {
        // 정책: ActorType.SYSTEM 인 경우 actorId는 반드시 null 이어야 한다.
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateActorAndActionOrThrow(ActorType.SYSTEM, 1L, ActionType.SUBMIT)
        );
    }

    @Test
    void shouldRejectWhenSystemApproves() {
        // 정책: SYSTEM + APPROVE 조합은 허용되지 않는다.
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateActorAndActionOrThrow(ActorType.SYSTEM, null, ActionType.APPROVE)
        );
    }

    @Test
    void shouldRejectWhenNonSystemActorHasNullActorId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateActorAndActionOrThrow(ActorType.SALES_REP, null, ActionType.SUBMIT)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidTargetCodes")
    void shouldRejectBlankTargetCode(String targetCode) {
        assertThrows(IllegalArgumentException.class, () -> validator.validateTargetCodeOrThrow(targetCode));
    }

    @Test
    void shouldAllowNonBlankTargetCode() {
        assertDoesNotThrow(() -> validator.validateTargetCodeOrThrow("QUO-2026-0001"));
    }

    private static Stream<Arguments> allowedActorActionCombinations() {
        return Stream.of(
                Arguments.of(ActorType.SALES_REP, 100L, ActionType.CREATE),
                Arguments.of(ActorType.SALES_REP, 100L, ActionType.UPDATE),
                Arguments.of(ActorType.SALES_REP, 100L, ActionType.SUBMIT),
                Arguments.of(ActorType.SALES_REP, 100L, ActionType.CONVERT),
                Arguments.of(ActorType.SALES_REP, 100L, ActionType.CANCEL),

                Arguments.of(ActorType.ADMIN, 200L, ActionType.APPROVE),
                Arguments.of(ActorType.ADMIN, 200L, ActionType.REJECT),
                Arguments.of(ActorType.ADMIN, 200L, ActionType.ISSUE),

                Arguments.of(ActorType.CLIENT, 300L, ActionType.APPROVE),
                Arguments.of(ActorType.CLIENT, 300L, ActionType.REJECT),
                Arguments.of(ActorType.CLIENT, 300L, ActionType.CREATE),
                Arguments.of(ActorType.CLIENT, 300L, ActionType.CANCEL),
                Arguments.of(ActorType.CLIENT, 300L, ActionType.PAY),

                Arguments.of(ActorType.SYSTEM, null, ActionType.CREATE),
                Arguments.of(ActorType.SYSTEM, null, ActionType.UPDATE),
                Arguments.of(ActorType.SYSTEM, null, ActionType.SUBMIT),
                Arguments.of(ActorType.SYSTEM, null, ActionType.PAY),
                Arguments.of(ActorType.SYSTEM, null, ActionType.EXPIRE)
        );
    }

    private static Stream<Arguments> deniedActorActionCombinations() {
        return Stream.of(
                Arguments.of(ActorType.CLIENT, 1L, ActionType.CONVERT),
                Arguments.of(ActorType.CLIENT, 1L, ActionType.ISSUE),
                Arguments.of(ActorType.SYSTEM, null, ActionType.REJECT)
        );
    }

    private static Stream<String> invalidTargetCodes() {
        return Stream.of(null, "", " ", "   ");
    }
}
