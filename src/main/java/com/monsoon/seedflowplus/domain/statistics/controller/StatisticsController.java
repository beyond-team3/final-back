package com.monsoon.seedflowplus.domain.statistics.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.statistics.dto.SalesRankingDto;
import com.monsoon.seedflowplus.domain.statistics.dto.SalesTrendDto;
import com.monsoon.seedflowplus.domain.statistics.dto.StatisticsFilter;
import com.monsoon.seedflowplus.domain.statistics.dto.StatisticsPeriod;
import com.monsoon.seedflowplus.domain.statistics.dto.StatisticsRankingType;
import com.monsoon.seedflowplus.domain.statistics.service.StatisticsQueryService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
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

@Tag(name = "Statistics API", description = "공통 매출 통계 조회 API")
@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsQueryService statisticsQueryService;

    @GetMapping("/sales-rep")
    public ApiResult<List<SalesTrendDto>> getSalesRepTrend(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam("period") StatisticsPeriod period
    ) {
        return ApiResult.success(statisticsQueryService.getMySalesTrend(principal, baseFilter(from, to, period)));
    }

    @GetMapping("/admin")
    public ApiResult<List<SalesTrendDto>> getAdminTrend(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam("period") StatisticsPeriod period
    ) {
        return ApiResult.success(statisticsQueryService.getAdminSalesTrend(baseFilter(from, to, period)));
    }

    @GetMapping("/by-employee")
    public ApiResult<List<SalesTrendDto>> getTrendByEmployee(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam("period") StatisticsPeriod period,
            @RequestParam("employeeIds") List<Long> employeeIds
    ) {
        return ApiResult.success(statisticsQueryService.getSalesTrendByEmployee(
                new StatisticsFilter(from, to, period, employeeIds, List.of(), List.of(), null, null)
        ));
    }

    @GetMapping("/by-client")
    public ApiResult<List<SalesTrendDto>> getTrendByClient(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam("period") StatisticsPeriod period,
            @RequestParam("clientIds") List<Long> clientIds
    ) {
        return ApiResult.success(statisticsQueryService.getSalesTrendByClient(
                principal,
                new StatisticsFilter(from, to, period, List.of(), clientIds, List.of(), null, null)
        ));
    }

    @GetMapping("/by-variety")
    public ApiResult<List<SalesTrendDto>> getTrendByVariety(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam("period") StatisticsPeriod period,
            @RequestParam("varietyCodes") List<String> varietyCodes
    ) {
        return ApiResult.success(statisticsQueryService.getSalesTrendByVariety(
                principal,
                new StatisticsFilter(from, to, period, List.of(), List.of(), varietyCodes, null, null)
        ));
    }

    @GetMapping("/ranking")
    public ApiResult<List<SalesRankingDto>> getRanking(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam("period") StatisticsPeriod period,
            @RequestParam("type") StatisticsRankingType type,
            @RequestParam(value = "employeeIds", required = false) List<Long> employeeIds,
            @RequestParam(value = "clientIds", required = false) List<Long> clientIds,
            @RequestParam(value = "varietyCodes", required = false) List<String> varietyCodes,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return ApiResult.success(statisticsQueryService.getRanking(
                principal,
                new StatisticsFilter(from, to, period, employeeIds, clientIds, varietyCodes, type, limit)
        ));
    }

    private StatisticsFilter baseFilter(LocalDate from, LocalDate to, StatisticsPeriod period) {
        return new StatisticsFilter(from, to, period, List.of(), List.of(), List.of(), null, null);
    }
}
