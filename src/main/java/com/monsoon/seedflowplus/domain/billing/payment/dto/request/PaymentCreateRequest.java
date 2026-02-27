package com.monsoon.seedflowplus.domain.billing.payment.dto.request;

import com.monsoon.seedflowplus.domain.billing.payment.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class PaymentCreateRequest {

    @NotNull(message = "청구서 ID는 필수입니다.")
    @Positive(message = "청구서 ID는 양수여야 합니다.")
    private Long invoiceId;

    @NotNull(message = "결제 수단은 필수입니다.")
    private PaymentMethod paymentMethod;
}