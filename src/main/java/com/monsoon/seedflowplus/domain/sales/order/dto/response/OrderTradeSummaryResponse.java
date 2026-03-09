package com.monsoon.seedflowplus.domain.sales.order.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@Schema(description = "거래처 거래 요약 응답 (영업사원/관리자 전용)")
public class OrderTradeSummaryResponse {

    @Schema(description = "이번달 거래 요약")
    private ThisMonth thisMonth;

    @Schema(description = "여신 한도", example = "50000000")
    private BigDecimal totalCredit;

    @Schema(description = "현재 미수금 (사용 여신)", example = "12000000")
    private BigDecimal usedCredit;

    @Schema(description = "잔여 여신 (여신 한도 - 미수금)", example = "38000000")
    private BigDecimal remainingCredit;

    @Getter
    @Builder
    @Schema(description = "이번달 주문 집계")
    public static class ThisMonth {

        @Schema(description = "이번달 총 주문 금액", example = "5500000")
        private BigDecimal totalAmount;

        @Schema(description = "진행 중인 주문 건수 (PENDING)", example = "3")
        private long inProgressCount;

        @Schema(description = "완료된 주문 건수 (CONFIRMED)", example = "2")
        private long completedCount;
    }
}