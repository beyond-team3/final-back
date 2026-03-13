package com.monsoon.seedflowplus.domain.statistics.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.EmployeeRepository;
import com.monsoon.seedflowplus.domain.product.entity.ProductCategory;
import com.monsoon.seedflowplus.domain.statistics.dto.SalesRankingDto;
import com.monsoon.seedflowplus.domain.statistics.dto.SalesTrendDto;
import com.monsoon.seedflowplus.domain.statistics.dto.SalesTrendItemDto;
import com.monsoon.seedflowplus.domain.statistics.dto.StatisticsFilter;
import com.monsoon.seedflowplus.domain.statistics.dto.StatisticsPeriod;
import com.monsoon.seedflowplus.domain.statistics.dto.StatisticsRankingType;
import com.monsoon.seedflowplus.domain.statistics.repository.StatisticsRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import com.querydsl.core.types.dsl.BooleanExpression;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsQueryService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final String ALL_TARGET_ID = "ALL";
    private static final String ALL_TARGET_NAME = "전체";

    private final StatisticsRepository statisticsRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;

    public List<SalesTrendDto> getMySalesTrend(CustomUserDetails principal, StatisticsFilter filter) {
        Long employeeId = requireEmployeeId(principal);
        StatisticsFilter validated = validateCommon(filter);
        String employeeName = employeeRepository.findById(employeeId)
                .map(Employee::getEmployeeName)
                .orElse(String.valueOf(employeeId));

        return fillTrendSeries(
                List.of(new TargetMeta(String.valueOf(employeeId), employeeName)),
                statisticsRepository.findMySalesTrend(validated, employeeId),
                validated.period(),
                validated.from(),
                validated.to()
        );
    }

    public List<SalesTrendDto> getAdminSalesTrend(StatisticsFilter filter) {
        StatisticsFilter validated = validateCommon(filter);
        requireAdmin();
        return fillTrendSeries(
                List.of(new TargetMeta(ALL_TARGET_ID, ALL_TARGET_NAME)),
                statisticsRepository.findAdminSalesTrend(validated),
                validated.period(),
                validated.from(),
                validated.to()
        );
    }

    public List<SalesTrendDto> getSalesTrendByEmployee(StatisticsFilter filter) {
        StatisticsFilter validated = validateForSelection(validateCommon(filter), validatedFilter -> validatedFilter.employeeIds(), "employeeIds");
        requireAdmin();
        Map<String, String> names = employeeRepository.findAllById(validated.employeeIds()).stream()
                .collect(java.util.stream.Collectors.toMap(
                        employee -> String.valueOf(employee.getId()),
                        Employee::getEmployeeName
                ));

        return fillTrendSeries(
                validated.employeeIds().stream()
                        .map(id -> new TargetMeta(String.valueOf(id), names.getOrDefault(String.valueOf(id), String.valueOf(id))))
                        .toList(),
                statisticsRepository.findSalesTrendByEmployee(validated, null),
                validated.period(),
                validated.from(),
                validated.to()
        );
    }

    public List<SalesTrendDto> getSalesTrendByClient(CustomUserDetails principal, StatisticsFilter filter) {
        StatisticsFilter validated = validateForSelection(validateCommon(filter), validatedFilter -> validatedFilter.clientIds(), "clientIds");
        Scope scope = resolveScope(principal);
        if (scope.emptyResult()) {
            return fillTrendSeries(
                    buildClientTargets(validated.clientIds()),
                    List.of(),
                    validated.period(),
                    validated.from(),
                    validated.to()
            );
        }

        return fillTrendSeries(
                buildClientTargets(validated.clientIds()),
                statisticsRepository.findSalesTrendByClient(validated, scope.condition()),
                validated.period(),
                validated.from(),
                validated.to()
        );
    }

    public List<SalesTrendDto> getSalesTrendByVariety(CustomUserDetails principal, StatisticsFilter filter) {
        StatisticsFilter validated = validateForSelection(validateCommon(filter), validatedFilter -> validatedFilter.varietyCodes(), "varietyCodes");
        Scope scope = resolveScope(principal);
        if (scope.emptyResult()) {
            return fillTrendSeries(
                    buildVarietyTargets(validated.varietyCodes()),
                    List.of(),
                    validated.period(),
                    validated.from(),
                    validated.to()
            );
        }

        return fillTrendSeries(
                buildVarietyTargets(validated.varietyCodes()),
                normalizeVarietyTrendRows(statisticsRepository.findSalesTrendByVariety(validated, scope.condition())),
                validated.period(),
                validated.from(),
                validated.to()
        );
    }

    public List<SalesRankingDto> getRanking(CustomUserDetails principal, StatisticsFilter filter) {
        StatisticsFilter validated = validateRanking(validateCommon(filter));
        Scope scope = resolveScope(principal);
        if (scope.emptyResult()) {
            return List.of();
        }

        List<StatisticsRepository.RankingRow> rankingRows = statisticsRepository.findRanking(validated, scope.condition());
        List<StatisticsRepository.RankingRow> normalizedRows = validated.type() == StatisticsRankingType.VARIETY
                ? normalizeVarietyRankingRows(rankingRows, validated.limit())
                : rankingRows;

        List<SalesRankingDto> result = new ArrayList<>();
        for (int i = 0; i < normalizedRows.size(); i++) {
            StatisticsRepository.RankingRow row = normalizedRows.get(i);
            result.add(new SalesRankingDto(i + 1, row.targetId(), row.targetName(), row.sales()));
        }
        return result;
    }

    private StatisticsFilter validateCommon(StatisticsFilter filter) {
        if (filter == null || filter.from() == null || filter.to() == null || filter.period() == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE, "from, to, period는 필수입니다.");
        }
        if (filter.from().isAfter(filter.to())) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE, "from은 to보다 이후일 수 없습니다.");
        }
        return filter.withLimit(normalizeLimit(filter.limit()));
    }

    private StatisticsFilter validateRanking(StatisticsFilter filter) {
        if (filter.type() == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE, "type은 필수입니다.");
        }
        return switch (filter.type()) {
            case EMPLOYEE -> validateForSelection(filter, StatisticsFilter::employeeIds, "employeeIds");
            case CLIENT -> validateForSelection(filter, StatisticsFilter::clientIds, "clientIds");
            case VARIETY -> validateForSelection(filter, StatisticsFilter::varietyCodes, "varietyCodes");
        };
    }

    private <T> StatisticsFilter validateForSelection(
            StatisticsFilter filter,
            Function<StatisticsFilter, List<T>> selector,
            String fieldName
    ) {
        List<T> values = selector.apply(filter);
        if (values == null || values.isEmpty()) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE, fieldName + "는 최소 1개 이상이어야 합니다.");
        }
        return filter;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private Long requireEmployeeId(CustomUserDetails principal) {
        if (principal == null || principal.getRole() == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
        if (principal.getRole() != Role.SALES_REP && principal.getRole() != Role.ADMIN) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
        if (principal.getEmployeeId() == null) {
            throw new CoreException(ErrorType.EMPLOYEE_NOT_LINKED);
        }
        return principal.getEmployeeId();
    }

    private void requireAdmin() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
        if (principal.getRole() != Role.ADMIN) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
    }

    private Scope resolveScope(CustomUserDetails principal) {
        if (principal == null || principal.getRole() == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
        if (principal.getRole() == Role.ADMIN) {
            return new Scope(null, false);
        }
        if (principal.getRole() != Role.SALES_REP) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
        Long employeeId = requireEmployeeId(principal);
        List<Long> managedClientIds = clientRepository.findAllByManagerEmployeeId(employeeId).stream()
                .map(Client::getId)
                .toList();
        if (managedClientIds.isEmpty()) {
            return new Scope(null, true);
        }
        return new Scope(com.monsoon.seedflowplus.domain.account.entity.QClient.client.id.in(managedClientIds), false);
    }

    private List<SalesTrendDto> fillTrendSeries(
            List<TargetMeta> targets,
            List<StatisticsRepository.TrendBucketRow> rows,
            StatisticsPeriod period,
            LocalDate from,
            LocalDate to
    ) {
        List<String> periods = generatePeriods(period, from, to);
        Map<String, Map<String, BigDecimal>> rowMap = new LinkedHashMap<>();
        Map<String, String> rowNames = new LinkedHashMap<>();

        for (StatisticsRepository.TrendBucketRow row : rows) {
            rowMap.computeIfAbsent(row.targetId(), key -> new LinkedHashMap<>())
                    .merge(row.period(), row.sales(), BigDecimal::add);
            if (row.targetName() != null) {
                rowNames.putIfAbsent(row.targetId(), row.targetName());
            }
        }

        List<SalesTrendDto> result = new ArrayList<>();
        for (TargetMeta target : targets) {
            Map<String, BigDecimal> salesByPeriod = rowMap.getOrDefault(target.targetId(), Map.of());
            List<SalesTrendItemDto> items = periods.stream()
                    .map(bucket -> new SalesTrendItemDto(bucket, salesByPeriod.getOrDefault(bucket, BigDecimal.ZERO)))
                    .toList();
            result.add(new SalesTrendDto(
                    target.targetId(),
                    rowNames.getOrDefault(target.targetId(), target.targetName()),
                    items
            ));
        }
        return result;
    }

    private List<String> generatePeriods(StatisticsPeriod period, LocalDate from, LocalDate to) {
        List<String> buckets = new ArrayList<>();
        if (period == StatisticsPeriod.QUARTERLY) {
            LocalDate cursor = quarterStart(from);
            LocalDate end = quarterStart(to);
            while (!cursor.isAfter(end)) {
                int quarter = ((cursor.getMonthValue() - 1) / 3) + 1;
                buckets.add(cursor.getYear() + "-Q" + quarter);
                cursor = cursor.plusMonths(3);
            }
            return buckets;
        }

        YearMonth cursor = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        while (!cursor.isAfter(end)) {
            buckets.add(cursor.toString());
            cursor = cursor.plusMonths(1);
        }
        return buckets;
    }

    private LocalDate quarterStart(LocalDate date) {
        int quarterMonth = ((date.getMonthValue() - 1) / 3) * 3 + 1;
        return LocalDate.of(date.getYear(), quarterMonth, 1);
    }

    private List<TargetMeta> buildClientTargets(List<Long> clientIds) {
        Map<String, String> names = clientRepository.findAllById(clientIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        client -> String.valueOf(client.getId()),
                        Client::getClientName
                ));
        return clientIds.stream()
                .map(id -> new TargetMeta(String.valueOf(id), names.getOrDefault(String.valueOf(id), String.valueOf(id))))
                .toList();
    }

    private List<TargetMeta> buildVarietyTargets(List<String> varietyCodes) {
        return varietyCodes.stream()
                .map(this::resolveVarietyMeta)
                .distinct()
                .toList();
    }

    private List<StatisticsRepository.TrendBucketRow> normalizeVarietyTrendRows(List<StatisticsRepository.TrendBucketRow> rows) {
        Map<VarietyPeriodKey, BigDecimal> grouped = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();

        for (StatisticsRepository.TrendBucketRow row : rows) {
            TargetMeta meta = resolveVarietyMeta(row.targetId());
            grouped.merge(new VarietyPeriodKey(meta.targetId(), row.period()), row.sales(), BigDecimal::add);
            names.putIfAbsent(meta.targetId(), meta.targetName());
        }

        return grouped.entrySet().stream()
                .map(entry -> new StatisticsRepository.TrendBucketRow(
                        entry.getKey().targetId(),
                        names.get(entry.getKey().targetId()),
                        entry.getKey().period(),
                        entry.getValue()
                ))
                .sorted(Comparator.comparing(StatisticsRepository.TrendBucketRow::targetId)
                        .thenComparing(StatisticsRepository.TrendBucketRow::period))
                .toList();
    }

    private List<StatisticsRepository.RankingRow> normalizeVarietyRankingRows(
            List<StatisticsRepository.RankingRow> rows,
            int limit
    ) {
        Map<String, BigDecimal> grouped = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();

        for (StatisticsRepository.RankingRow row : rows) {
            TargetMeta meta = resolveVarietyMeta(row.targetId());
            grouped.merge(meta.targetId(), row.sales(), BigDecimal::add);
            names.putIfAbsent(meta.targetId(), meta.targetName());
        }

        return grouped.entrySet().stream()
                .map(entry -> new StatisticsRepository.RankingRow(
                        entry.getKey(),
                        names.get(entry.getKey()),
                        entry.getValue()
                ))
                .sorted(Comparator.comparing(StatisticsRepository.RankingRow::sales).reversed()
                        .thenComparing(StatisticsRepository.RankingRow::targetName))
                .limit(limit)
                .toList();
    }

    private TargetMeta resolveVarietyMeta(String value) {
        if (value == null) {
            return new TargetMeta("UNKNOWN", "UNKNOWN");
        }

        String trimmed = value.trim();
        for (ProductCategory category : ProductCategory.values()) {
            if (category.name().equalsIgnoreCase(trimmed)
                    || category.getCode().equalsIgnoreCase(trimmed)
                    || category.getDescription().equalsIgnoreCase(trimmed)) {
                return new TargetMeta(category.name(), category.getDescription());
            }
        }

        return new TargetMeta(trimmed.toUpperCase(Locale.ROOT), trimmed);
    }

    private record Scope(BooleanExpression condition, boolean emptyResult) {
    }

    private record TargetMeta(String targetId, String targetName) {
    }

    private record VarietyPeriodKey(String targetId, String period) {
    }
}
