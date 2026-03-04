package com.monsoon.seedflowplus.domain.statistics.billing.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.request.BillingRevenueStatisticsFilter;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.MonthlyBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.repository.BillingRevenueStatisticsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillingRevenueStatisticsQueryServiceTest {

    @Mock
    private BillingRevenueStatisticsRepository repository;

    @InjectMocks
    private BillingRevenueStatisticsQueryService queryService;

    @Test
    @DisplayName("fromDate가 toDate보다 이후면 INVALID_INPUT_VALUE 예외가 발생한다")
    void shouldThrowWhenFromDateAfterToDate() {
        BillingRevenueStatisticsFilter filter = filter(
                LocalDate.of(2026, 12, 31),
                LocalDate.of(2026, 1, 1),
                null
        );

        assertThatThrownBy(() -> queryService.getMonthlyRevenue(filter))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_INPUT_VALUE);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("조회 기간이 24개월 초과면 INVALID_INPUT_VALUE 예외가 발생한다")
    void shouldThrowWhenDateRangeOver24Months() {
        BillingRevenueStatisticsFilter filter = filter(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2026, 2, 1),
                null
        );

        assertThatThrownBy(() -> queryService.getCategoryRevenue(filter))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_INPUT_VALUE);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("조회 기간이 24개월 이하면 repository를 1회 호출한다")
    void shouldCallRepositoryWhenDateRangeWithin24Months() {
        BillingRevenueStatisticsFilter filter = filter(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2026, 1, 31),
                null
        );

        when(repository.findMonthlyRevenue(filter))
                .thenReturn(List.of(new MonthlyBilledRevenueDto("2026-01", BigDecimal.TEN)));

        queryService.getMonthlyRevenue(filter);

        verify(repository).findMonthlyRevenue(filter);
    }

    private BillingRevenueStatisticsFilter filter(LocalDate from, LocalDate to, String category) {
        BillingRevenueStatisticsFilter filter = new BillingRevenueStatisticsFilter();
        filter.setFromDate(from);
        filter.setToDate(to);
        filter.setCategory(category);
        return filter;
    }
}
