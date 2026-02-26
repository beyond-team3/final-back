package com.monsoon.seedflowplus.domain.deal.entity;

import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatus;
import com.monsoon.seedflowplus.domain.billing.payment.entity.PaymentStatus;
import com.monsoon.seedflowplus.domain.billing.statement.entity.StatementStatus;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderStatus;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class DocumentStatusValidator {

    private static final Map<DealType, Set<String>> ALLOWED_STATUS_BY_DEAL_TYPE = createAllowedStatusMap();

    private DocumentStatusValidator() {
    }

    public static void validateRequired(DealType dealType, String status, String fieldName) {
        Objects.requireNonNull(fieldName, "fieldName은 null값이 될 수 없습니다.");
        if (status == null) {
            throw new IllegalArgumentException(fieldName + "는 null값이 될 수 없습니다.");
        }
        validate(dealType, status, fieldName);
    }

    public static void validateNullable(DealType dealType, String status, String fieldName) {
        Objects.requireNonNull(fieldName, "fieldName은 null값이 될 수 없습니다.");
        if (status == null) {
            return;
        }
        validate(dealType, status, fieldName);
    }

    private static void validate(DealType dealType, String status, String fieldName) {
        Objects.requireNonNull(dealType, "dealType은 null값이 될 수 없습니다.");
        Set<String> allowedStatuses = ALLOWED_STATUS_BY_DEAL_TYPE.get(dealType);
        if (!allowedStatuses.contains(status)) {
            throw new IllegalArgumentException(
                    fieldName + " 값이 유효하지 않습니다. dealType=" + dealType + ", status=" + status
            );
        }
    }

    private static Map<DealType, Set<String>> createAllowedStatusMap() {
        Map<DealType, Set<String>> statusMap = new EnumMap<>(DealType.class);
        statusMap.put(DealType.RFQ, enumNames(QuotationRequestStatus.class));
        statusMap.put(DealType.QUO, enumNames(QuotationStatus.class));
        statusMap.put(DealType.CNT, enumNames(ContractStatus.class));
        statusMap.put(DealType.ORD, enumNames(OrderStatus.class));
        statusMap.put(DealType.STMT, enumNames(StatementStatus.class));
        statusMap.put(DealType.INV, enumNames(InvoiceStatus.class));
        statusMap.put(DealType.PAY, enumNames(PaymentStatus.class));
        return statusMap;
    }

    private static Set<String> enumNames(Class<? extends Enum<?>> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
    }
}
