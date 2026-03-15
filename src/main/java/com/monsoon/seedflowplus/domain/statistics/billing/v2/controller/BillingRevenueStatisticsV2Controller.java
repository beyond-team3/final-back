package com.monsoon.seedflowplus.domain.statistics.billing.v2.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.request.BillingRevenueStatisticsFilter;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.CategoryBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.MonthlyBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.MonthlyCategoryBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.service.BillingRevenueStatisticsQueryService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Billing Statistics V2 API", description = "v2 청구 매출 통계 조회 API")
@RestController
@RequestMapping("/api/v2/statistics/billing/revenue")
@RequiredArgsConstructor
public class BillingRevenueStatisticsV2Controller {

    private final BillingRevenueStatisticsQueryService queryService;

    @Operation(summary = "v2 월별 청구 매출 조회")
    @GetMapping("/monthly")
    public ApiResult<List<MonthlyBilledRevenueDto>> getMonthlyRevenue(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "category", required = false) String category
    ) {
        return ApiResult.success(queryService.getMonthlyRevenue(toFilter(from, to, category), principal));
    }

    @Operation(summary = "v2 품종별 청구 매출 조회")
    @GetMapping("/by-category")
    public ApiResult<List<CategoryBilledRevenueDto>> getCategoryRevenue(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "category", required = false) String category
    ) {
        return ApiResult.success(queryService.getCategoryRevenue(toFilter(from, to, category), principal));
    }

    @Operation(summary = "v2 월별/품종별 청구 매출 조회")
    @GetMapping("/monthly-by-category")
    public ApiResult<List<MonthlyCategoryBilledRevenueDto>> getMonthlyCategoryRevenue(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "category", required = false) String category
    ) {
        return ApiResult.success(queryService.getMonthlyCategoryRevenue(toFilter(from, to, category), principal));
    }

    private BillingRevenueStatisticsFilter toFilter(LocalDate from, LocalDate to, String category) {
        return new BillingRevenueStatisticsFilter(from, to, category);
    }
}
