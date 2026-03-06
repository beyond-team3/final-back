package com.monsoon.seedflowplus.domain.statistics.billing.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.request.BillingRevenueStatisticsFilter;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.CategoryBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.MonthlyBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.repository.BillingRevenueStatisticsRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
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
    @DisplayName("유효한 기간이면 월별 매출 조회가 정상 실행된다")
    void shouldSucceedWhenDateRangeIsValid() {
        BillingRevenueStatisticsFilter filter = filter(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2025, 12, 31),
                null
        );
        CustomUserDetails principal = principal(Role.ADMIN, 999L);

        when(repository.findMonthlyRevenue(filter, null))
                .thenReturn(List.of(new MonthlyBilledRevenueDto("2025-12", BigDecimal.TEN)));

        queryService.getMonthlyRevenue(filter, principal);

        verify(repository).findMonthlyRevenue(filter, null);
    }

    @Test
    @DisplayName("조회 기간이 포함 기준 정확히 24개월이면 허용된다")
    void shouldAllowWhenDateRangeIsExactly24Months() {
        BillingRevenueStatisticsFilter filter = filter(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2025, 12, 31),
                null
        );
        CustomUserDetails principal = principal(Role.ADMIN, 999L);

        when(repository.findCategoryRevenue(filter, null))
                .thenReturn(List.of(new CategoryBilledRevenueDto("수박", BigDecimal.ONE)));

        queryService.getCategoryRevenue(filter, principal);

        verify(repository).findCategoryRevenue(filter, null);
    }

    @Test
    @DisplayName("조회 기간이 포함 기준 25개월이면 INVALID_INPUT_VALUE 예외가 발생한다")
    void shouldThrowWhenDateRangeExceeds24Months() {
        BillingRevenueStatisticsFilter filter = filter(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2026, 1, 31),
                null
        );

        assertThatThrownBy(() -> queryService.getMonthlyCategoryRevenue(filter, principal(Role.ADMIN, 1L)))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_INPUT_VALUE);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("ADMIN은 전체 통계를 조회하므로 employee 범위 없이 리포지토리를 호출한다")
    void shouldQueryAllRevenueForAdmin() {
        BillingRevenueStatisticsFilter filter = filter(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                null
        );
        CustomUserDetails principal = principal(Role.ADMIN, 999L);

        when(repository.findMonthlyRevenue(filter, null))
                .thenReturn(List.of(new MonthlyBilledRevenueDto("2024-01", BigDecimal.TEN)));

        queryService.getMonthlyRevenue(filter, principal);

        verify(repository).findMonthlyRevenue(filter, null);
    }

    @Test
    @DisplayName("SALES_REP는 본인 employeeId 범위로만 조회한다")
    void shouldScopeRevenueToSalesRepEmployeeId() {
        BillingRevenueStatisticsFilter filter = filter(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                null
        );
        CustomUserDetails principal = principal(Role.SALES_REP, 321L);

        when(repository.findCategoryRevenue(filter, 321L))
                .thenReturn(List.of(new CategoryBilledRevenueDto("수박", BigDecimal.ONE)));

        queryService.getCategoryRevenue(filter, principal);

        verify(repository).findCategoryRevenue(filter, 321L);
    }

    @Test
    @DisplayName("직원 정보가 없는 SALES_REP는 EMPLOYEE_NOT_LINKED 예외가 발생한다")
    void shouldThrowWhenSalesRepHasNoEmployeeId() {
        BillingRevenueStatisticsFilter filter = filter(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                null
        );
        CustomUserDetails principal = principal(Role.SALES_REP, null);

        assertThatThrownBy(() -> queryService.getMonthlyRevenue(filter, principal))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.EMPLOYEE_NOT_LINKED);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("허용되지 않은 역할은 ACCESS_DENIED 예외가 발생한다")
    void shouldThrowWhenRoleIsNotAllowed() {
        BillingRevenueStatisticsFilter filter = filter(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                null
        );
        CustomUserDetails principal = principal(Role.CLIENT, null);

        assertThatThrownBy(() -> queryService.getMonthlyRevenue(filter, principal))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ACCESS_DENIED);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("fromDate가 toDate보다 이후면 INVALID_INPUT_VALUE 예외가 발생한다")
    void shouldThrowWhenFromDateIsAfterToDate() {
        BillingRevenueStatisticsFilter filter = filter(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2024, 1, 1),
                null
        );

        assertThatThrownBy(() -> queryService.getMonthlyRevenue(filter, principal(Role.ADMIN, 1L)))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_INPUT_VALUE);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("fromDate가 없으면 INVALID_INPUT_VALUE 예외가 발생한다")
    void shouldThrowWhenFromDateIsNull() {
        BillingRevenueStatisticsFilter filter = filter(
                null,
                LocalDate.of(2025, 12, 31),
                null
        );

        assertThatThrownBy(() -> queryService.getCategoryRevenue(filter, principal(Role.ADMIN, 1L)))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_INPUT_VALUE);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("toDate가 없으면 INVALID_INPUT_VALUE 예외가 발생한다")
    void shouldThrowWhenToDateIsNull() {
        BillingRevenueStatisticsFilter filter = filter(
                LocalDate.of(2024, 1, 1),
                null,
                null
        );

        assertThatThrownBy(() -> queryService.getMonthlyRevenue(filter, principal(Role.ADMIN, 1L)))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_INPUT_VALUE);

        verifyNoInteractions(repository);
    }

    private BillingRevenueStatisticsFilter filter(LocalDate from, LocalDate to, String category) {
        return new BillingRevenueStatisticsFilter(from, to, category);
    }

    private CustomUserDetails principal(Role role, Long employeeId) {
        CustomUserDetails principal = org.mockito.Mockito.mock(CustomUserDetails.class);
        lenient().when(principal.getRole()).thenReturn(role);
        lenient().when(principal.getEmployeeId()).thenReturn(employeeId);
        return principal;
    }
}
