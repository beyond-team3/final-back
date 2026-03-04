package com.monsoon.seedflowplus.domain.statistics.billing.api;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.request.BillingRevenueStatisticsFilter;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.CategoryBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.MonthlyBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.MonthlyCategoryBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.service.BillingRevenueStatisticsQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Billing Statistics API", description = "청구 매출 통계 조회 API")
// `/api/v1` 프리픽스는 의도적으로 제외한다.
// 관련 검증: BillingRevenueStatisticsControllerTest.shouldReturn404ForLegacyPath
@RestController("billingRevenueStatisticsApiController")
@RequestMapping("/statistics/billing/revenue")
@RequiredArgsConstructor
public class BillingRevenueStatisticsController {

    private final BillingRevenueStatisticsQueryService queryService;

    @Operation(summary = "월별 청구 매출 조회", description = "기간 내 월별 청구 매출을 조회합니다.")
    @GetMapping("/monthly")
    public ApiResult<List<MonthlyBilledRevenueDto>> getMonthlyRevenue(
            @Parameter(example = "2026-01-01")
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @Parameter(example = "2026-12-31")
            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @Parameter(example = "수박")
            @RequestParam(value = "category", required = false)
            String category
    ) {
        return ApiResult.success(queryService.getMonthlyRevenue(toFilter(from, to, category)));
    }

    @Operation(summary = "품종별 청구 매출 조회", description = "기간 내 품종별 청구 매출을 조회합니다.")
    @GetMapping("/by-category")
    public ApiResult<List<CategoryBilledRevenueDto>> getCategoryRevenue(
            @Parameter(example = "2026-01-01")
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @Parameter(example = "2026-12-31")
            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @Parameter(example = "수박")
            @RequestParam(value = "category", required = false)
            String category
    ) {
        return ApiResult.success(queryService.getCategoryRevenue(toFilter(from, to, category)));
    }

    @Operation(summary = "월별/품종별 청구 매출 조회", description = "기간 내 월별/품종별 청구 매출을 조회합니다.")
    @GetMapping("/monthly-by-category")
    public ApiResult<List<MonthlyCategoryBilledRevenueDto>> getMonthlyCategoryRevenue(
            @Parameter(example = "2026-01-01")
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @Parameter(example = "2026-12-31")
            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @Parameter(example = "수박")
            @RequestParam(value = "category", required = false)
            String category
    ) {
        return ApiResult.success(queryService.getMonthlyCategoryRevenue(toFilter(from, to, category)));
    }

    private BillingRevenueStatisticsFilter toFilter(LocalDate from, LocalDate to, String category) {
        return new BillingRevenueStatisticsFilter(from, to, category);
    }
}
