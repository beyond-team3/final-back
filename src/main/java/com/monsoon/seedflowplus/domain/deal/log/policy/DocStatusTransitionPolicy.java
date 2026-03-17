package com.monsoon.seedflowplus.domain.deal.log.policy;

import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.common.DocumentStatusValidator;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderStatus;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;
import com.monsoon.seedflowplus.domain.billing.statement.entity.StatementStatus;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatus;
import com.monsoon.seedflowplus.domain.billing.payment.entity.PaymentStatus;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DocStatusTransitionPolicy {

    private static final Set<ActionType> FINAL_ACTIONS = EnumSet.of(
            ActionType.APPROVE,
            ActionType.REJECT,
            ActionType.PAY,
            ActionType.EXPIRE,
            ActionType.CONFIRM,
            ActionType.CANCEL
    );

    private static final Map<DealType, Set<String>> TERMINAL_STATUSES = createTerminalStatuses();
    private static final Map<DealType, Map<String, Map<ActionType, Set<String>>>> RULES = createRules();

    private DocStatusTransitionPolicy() {
    }

    public static boolean isAllowed(DealType dealType, String fromStatus, ActionType actionType, String toStatus) {
        Objects.requireNonNull(dealType, "dealType은 null값이 될 수 없습니다.");
        Objects.requireNonNull(actionType, "actionType은 null값이 될 수 없습니다.");
        DocumentStatusValidator.validateRequired(dealType, fromStatus, "fromStatus");
        DocumentStatusValidator.validateRequired(dealType, toStatus, "toStatus");

        if (isTerminalStatus(dealType, fromStatus) && FINAL_ACTIONS.contains(actionType)) {
            return false;
        }

        return RULES.getOrDefault(dealType, Map.of())
                .getOrDefault(fromStatus, Map.of())
                .getOrDefault(actionType, Set.of())
                .contains(toStatus);
    }

    public static boolean isTerminalStatus(DealType dealType, String status) {
        Objects.requireNonNull(dealType, "dealType은 null값이 될 수 없습니다.");
        DocumentStatusValidator.validateRequired(dealType, status, "status");
        return TERMINAL_STATUSES.getOrDefault(dealType, Set.of()).contains(status);
    }

    public static Set<String> allowedToStatuses(DealType dealType, String fromStatus, ActionType actionType) {
        Objects.requireNonNull(dealType, "dealType은 null값이 될 수 없습니다.");
        Objects.requireNonNull(actionType, "actionType은 null값이 될 수 없습니다.");
        DocumentStatusValidator.validateRequired(dealType, fromStatus, "fromStatus");

        Set<String> allowed = RULES.getOrDefault(dealType, Map.of())
                .getOrDefault(fromStatus, Map.of())
                .getOrDefault(actionType, Set.of());
        return Collections.unmodifiableSet(allowed);
    }

    private static Map<DealType, Set<String>> createTerminalStatuses() {
        Map<DealType, Set<String>> map = new EnumMap<>(DealType.class);
        map.put(DealType.RFQ, Set.of(QuotationRequestStatus.COMPLETED.name()));
        map.put(DealType.QUO, Set.of(QuotationStatus.COMPLETED.name(), QuotationStatus.EXPIRED.name()));
        map.put(DealType.CNT, Set.of(ContractStatus.COMPLETED.name(), ContractStatus.EXPIRED.name()));
        map.put(DealType.ORD, Set.of(OrderStatus.CONFIRMED.name(), OrderStatus.CANCELED.name()));
        map.put(DealType.STMT, Set.of(StatementStatus.CANCELED.name()));
        map.put(DealType.INV, Set.of(InvoiceStatus.PAID.name(), InvoiceStatus.CANCELED.name()));
        map.put(DealType.PAY, Set.of(PaymentStatus.COMPLETED.name(), PaymentStatus.FAILED.name()));
        return Collections.unmodifiableMap(map);
    }

    private static Map<DealType, Map<String, Map<ActionType, Set<String>>>> createRules() {
        Map<DealType, Map<String, Map<ActionType, Set<String>>>> map = new EnumMap<>(DealType.class);

        map.put(DealType.RFQ, rules(
                transition(QuotationRequestStatus.PENDING, ActionType.SUBMIT, QuotationRequestStatus.REVIEWING),
                transition(QuotationRequestStatus.REVIEWING, ActionType.APPROVE, QuotationRequestStatus.COMPLETED),
                transition(QuotationRequestStatus.REVIEWING, ActionType.REJECT, QuotationRequestStatus.PENDING)
        ));

        map.put(DealType.QUO, rules(
                transition(QuotationStatus.WAITING_ADMIN, ActionType.APPROVE, QuotationStatus.WAITING_CLIENT),
                transition(QuotationStatus.WAITING_ADMIN, ActionType.REJECT, QuotationStatus.REJECTED_ADMIN),
                transition(QuotationStatus.REJECTED_ADMIN, ActionType.RESUBMIT, QuotationStatus.WAITING_ADMIN),
                transition(QuotationStatus.WAITING_CLIENT, ActionType.APPROVE, QuotationStatus.FINAL_APPROVED),
                transition(QuotationStatus.WAITING_CLIENT, ActionType.REJECT, QuotationStatus.REJECTED_CLIENT),
                transition(QuotationStatus.REJECTED_CLIENT, ActionType.RESUBMIT, QuotationStatus.WAITING_CLIENT),
                transition(QuotationStatus.FINAL_APPROVED, ActionType.CONVERT, QuotationStatus.COMPLETED),
                transition(QuotationStatus.WAITING_CONTRACT, ActionType.CONVERT, QuotationStatus.COMPLETED),
                transition(QuotationStatus.WAITING_ADMIN, ActionType.EXPIRE, QuotationStatus.EXPIRED),
                transition(QuotationStatus.WAITING_CLIENT, ActionType.EXPIRE, QuotationStatus.EXPIRED)
        ));

        map.put(DealType.CNT, rules(
                transition(ContractStatus.WAITING_ADMIN, ActionType.APPROVE, ContractStatus.WAITING_CLIENT),
                transition(ContractStatus.WAITING_ADMIN, ActionType.REJECT, ContractStatus.REJECTED_ADMIN),
                transition(ContractStatus.REJECTED_ADMIN, ActionType.RESUBMIT, ContractStatus.WAITING_ADMIN),
                transition(ContractStatus.WAITING_CLIENT, ActionType.APPROVE, ContractStatus.COMPLETED),
                transition(ContractStatus.WAITING_CLIENT, ActionType.APPROVE, ContractStatus.ACTIVE_CONTRACT),
                transition(ContractStatus.WAITING_CLIENT, ActionType.REJECT, ContractStatus.REJECTED_CLIENT),
                transition(ContractStatus.REJECTED_CLIENT, ActionType.RESUBMIT, ContractStatus.WAITING_CLIENT),
                transition(ContractStatus.WAITING_ADMIN, ActionType.EXPIRE, ContractStatus.EXPIRED),
                transition(ContractStatus.WAITING_CLIENT, ActionType.EXPIRE, ContractStatus.EXPIRED)
        ));

        map.put(DealType.ORD, rules(
                transition(OrderStatus.PENDING, ActionType.CONFIRM, OrderStatus.CONFIRMED),
                transition(OrderStatus.PENDING, ActionType.CANCEL, OrderStatus.CANCELED)
        ));

        map.put(DealType.STMT, rules(
                transition(StatementStatus.ISSUED, ActionType.CANCEL, StatementStatus.CANCELED)
        ));

        map.put(DealType.INV, rules(
                transition(InvoiceStatus.DRAFT, ActionType.ISSUE, InvoiceStatus.PUBLISHED),
                transition(InvoiceStatus.DRAFT, ActionType.CANCEL, InvoiceStatus.CANCELED),
                transition(InvoiceStatus.PUBLISHED, ActionType.PAY, InvoiceStatus.PAID),
                transition(InvoiceStatus.PUBLISHED, ActionType.CANCEL, InvoiceStatus.CANCELED)
        ));

        map.put(DealType.PAY, rules(
                transition(PaymentStatus.PENDING, ActionType.PAY, PaymentStatus.COMPLETED),
                transition(PaymentStatus.PENDING, ActionType.CANCEL, PaymentStatus.FAILED)
        ));

        return Collections.unmodifiableMap(map);
    }

    private static Map<String, Map<ActionType, Set<String>>> rules(TransitionRule... rules) {
        Map<String, Map<ActionType, Set<String>>> map = new HashMap<>();
        for (TransitionRule rule : rules) {
            map.computeIfAbsent(rule.fromStatus(), key -> new EnumMap<>(ActionType.class))
                    .computeIfAbsent(rule.actionType(), key -> new HashSet<>())
                    .add(rule.toStatus());
        }

        Map<String, Map<ActionType, Set<String>>> unmodifiable = new HashMap<>();
        for (Map.Entry<String, Map<ActionType, Set<String>>> entry : map.entrySet()) {
            Map<ActionType, Set<String>> actionMap = new EnumMap<>(ActionType.class);
            for (Map.Entry<ActionType, Set<String>> actionEntry : entry.getValue().entrySet()) {
                actionMap.put(actionEntry.getKey(), Set.copyOf(actionEntry.getValue()));
            }
            unmodifiable.put(entry.getKey(), Collections.unmodifiableMap(actionMap));
        }
        return Collections.unmodifiableMap(unmodifiable);
    }

    private static TransitionRule transition(Enum<?> fromStatus, ActionType actionType, Enum<?> toStatus) {
        return new TransitionRule(fromStatus.name(), actionType, toStatus.name());
    }

    private record TransitionRule(String fromStatus, ActionType actionType, String toStatus) {
    }
}
