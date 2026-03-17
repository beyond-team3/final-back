package com.monsoon.seedflowplus.domain.deal.log.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatus;
import com.monsoon.seedflowplus.domain.billing.payment.entity.PaymentStatus;
import com.monsoon.seedflowplus.domain.billing.statement.entity.StatementStatus;
import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderStatus;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

class DocStatusTransitionPolicyTest {

    @ParameterizedTest(name = "{index}: {0} {1} --{2}--> {3}")
    @MethodSource("allowedTransitions")
    void shouldAllowAllConfiguredTransitions(DealType dealType, String fromStatus, ActionType actionType, String toStatus) {
        assertTrue(DocStatusTransitionPolicy.isAllowed(dealType, fromStatus, actionType, toStatus));
    }

    @ParameterizedTest(name = "{index}: reject {0} {1} --{2}--> {3}")
    @MethodSource("deniedTransitions")
    void shouldRejectInvalidTransitions(DealType dealType, String fromStatus, ActionType actionType, String toStatus) {
        assertFalse(DocStatusTransitionPolicy.isAllowed(dealType, fromStatus, actionType, toStatus));
    }

    @Test
    void shouldReturnExpectedAllowedToStatuses() {
        Set<String> quoResubmitTargets = DocStatusTransitionPolicy.allowedToStatuses(
                DealType.QUO,
                QuotationStatus.REJECTED_CLIENT.name(),
                ActionType.RESUBMIT
        );
        assertEquals(Set.of(QuotationStatus.WAITING_CLIENT.name()), quoResubmitTargets);
    }

    private static Stream<Arguments> allowedTransitions() {
        return Stream.of(
                Arguments.of(DealType.RFQ, QuotationRequestStatus.PENDING.name(), ActionType.SUBMIT, QuotationRequestStatus.REVIEWING.name()),
                Arguments.of(DealType.RFQ, QuotationRequestStatus.REVIEWING.name(), ActionType.APPROVE, QuotationRequestStatus.COMPLETED.name()),
                Arguments.of(DealType.RFQ, QuotationRequestStatus.REVIEWING.name(), ActionType.REJECT, QuotationRequestStatus.PENDING.name()),

                Arguments.of(DealType.QUO, QuotationStatus.WAITING_ADMIN.name(), ActionType.APPROVE, QuotationStatus.WAITING_CLIENT.name()),
                Arguments.of(DealType.QUO, QuotationStatus.WAITING_ADMIN.name(), ActionType.REJECT, QuotationStatus.REJECTED_ADMIN.name()),
                Arguments.of(DealType.QUO, QuotationStatus.REJECTED_ADMIN.name(), ActionType.RESUBMIT, QuotationStatus.WAITING_ADMIN.name()),
                Arguments.of(DealType.QUO, QuotationStatus.WAITING_CLIENT.name(), ActionType.APPROVE, QuotationStatus.FINAL_APPROVED.name()),
                Arguments.of(DealType.QUO, QuotationStatus.WAITING_CLIENT.name(), ActionType.REJECT, QuotationStatus.REJECTED_CLIENT.name()),
                Arguments.of(DealType.QUO, QuotationStatus.REJECTED_CLIENT.name(), ActionType.RESUBMIT, QuotationStatus.WAITING_CLIENT.name()),
                Arguments.of(DealType.QUO, QuotationStatus.FINAL_APPROVED.name(), ActionType.CONVERT, QuotationStatus.COMPLETED.name()),
                Arguments.of(DealType.QUO, QuotationStatus.WAITING_ADMIN.name(), ActionType.EXPIRE, QuotationStatus.EXPIRED.name()),
                Arguments.of(DealType.QUO, QuotationStatus.WAITING_CLIENT.name(), ActionType.EXPIRE, QuotationStatus.EXPIRED.name()),

                Arguments.of(DealType.CNT, ContractStatus.WAITING_ADMIN.name(), ActionType.APPROVE, ContractStatus.WAITING_CLIENT.name()),
                Arguments.of(DealType.CNT, ContractStatus.WAITING_ADMIN.name(), ActionType.REJECT, ContractStatus.REJECTED_ADMIN.name()),
                Arguments.of(DealType.CNT, ContractStatus.REJECTED_ADMIN.name(), ActionType.RESUBMIT, ContractStatus.WAITING_ADMIN.name()),
                Arguments.of(DealType.CNT, ContractStatus.WAITING_CLIENT.name(), ActionType.APPROVE, ContractStatus.COMPLETED.name()),
                Arguments.of(DealType.CNT, ContractStatus.WAITING_CLIENT.name(), ActionType.APPROVE, ContractStatus.ACTIVE_CONTRACT.name()),
                Arguments.of(DealType.CNT, ContractStatus.WAITING_CLIENT.name(), ActionType.REJECT, ContractStatus.REJECTED_CLIENT.name()),
                Arguments.of(DealType.CNT, ContractStatus.REJECTED_CLIENT.name(), ActionType.RESUBMIT, ContractStatus.WAITING_CLIENT.name()),
                Arguments.of(DealType.CNT, ContractStatus.WAITING_ADMIN.name(), ActionType.EXPIRE, ContractStatus.EXPIRED.name()),
                Arguments.of(DealType.CNT, ContractStatus.WAITING_CLIENT.name(), ActionType.EXPIRE, ContractStatus.EXPIRED.name()),

                Arguments.of(DealType.ORD, OrderStatus.PENDING.name(), ActionType.CONFIRM, OrderStatus.CONFIRMED.name()),
                Arguments.of(DealType.ORD, OrderStatus.PENDING.name(), ActionType.CANCEL, OrderStatus.CANCELED.name()),

                Arguments.of(DealType.STMT, StatementStatus.ISSUED.name(), ActionType.CANCEL, StatementStatus.CANCELED.name()),

                Arguments.of(DealType.INV, InvoiceStatus.DRAFT.name(), ActionType.ISSUE, InvoiceStatus.PUBLISHED.name()),
                Arguments.of(DealType.INV, InvoiceStatus.DRAFT.name(), ActionType.CANCEL, InvoiceStatus.CANCELED.name()),
                Arguments.of(DealType.INV, InvoiceStatus.PUBLISHED.name(), ActionType.PAY, InvoiceStatus.PAID.name()),
                Arguments.of(DealType.INV, InvoiceStatus.PUBLISHED.name(), ActionType.CANCEL, InvoiceStatus.CANCELED.name()),

                Arguments.of(DealType.PAY, PaymentStatus.PENDING.name(), ActionType.PAY, PaymentStatus.COMPLETED.name()),
                Arguments.of(DealType.PAY, PaymentStatus.PENDING.name(), ActionType.CANCEL, PaymentStatus.FAILED.name())
        );
    }

    private static Stream<Arguments> deniedTransitions() {
        return Stream.of(
                Arguments.of(DealType.QUO, QuotationStatus.WAITING_ADMIN.name(), ActionType.PAY, QuotationStatus.WAITING_CLIENT.name()),
                Arguments.of(DealType.QUO, QuotationStatus.COMPLETED.name(), ActionType.APPROVE, QuotationStatus.WAITING_CLIENT.name()),
                Arguments.of(DealType.QUO, QuotationStatus.EXPIRED.name(), ActionType.RESUBMIT, QuotationStatus.WAITING_ADMIN.name()),
                Arguments.of(DealType.CNT, ContractStatus.EXPIRED.name(), ActionType.RESUBMIT, ContractStatus.WAITING_ADMIN.name()),
                Arguments.of(DealType.INV, InvoiceStatus.PAID.name(), ActionType.CANCEL, InvoiceStatus.CANCELED.name())
        );
    }
}
