package com.monsoon.seedflowplus.domain.statistics.dto;

import java.time.LocalDate;
import java.util.List;

public record StatisticsFilter(
        LocalDate from,
        LocalDate to,
        StatisticsPeriod period,
        List<Long> employeeIds,
        List<Long> clientIds,
        List<String> varietyCodes,
        StatisticsRankingType type,
        Integer limit
) {

    public StatisticsFilter {
        employeeIds = employeeIds == null ? List.of() : List.copyOf(employeeIds);
        clientIds = clientIds == null ? List.of() : List.copyOf(clientIds);
        varietyCodes = varietyCodes == null ? List.of() : List.copyOf(varietyCodes);
    }

    public StatisticsFilter withLimit(Integer newLimit) {
        return new StatisticsFilter(from, to, period, employeeIds, clientIds, varietyCodes, type, newLimit);
    }
}
