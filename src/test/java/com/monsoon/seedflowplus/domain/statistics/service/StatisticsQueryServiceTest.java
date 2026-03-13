package com.monsoon.seedflowplus.domain.statistics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.EmployeeRepository;
import com.monsoon.seedflowplus.domain.statistics.dto.SalesTrendDto;
import com.monsoon.seedflowplus.domain.statistics.dto.StatisticsFilter;
import com.monsoon.seedflowplus.domain.statistics.dto.StatisticsPeriod;
import com.monsoon.seedflowplus.domain.statistics.dto.StatisticsRankingType;
import com.monsoon.seedflowplus.domain.statistics.repository.StatisticsRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class StatisticsQueryServiceTest {

    @Mock
    private StatisticsRepository statisticsRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private StatisticsQueryService statisticsQueryService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("from/to가 역전되면 INVALID_INPUT_VALUE 예외가 발생한다")
    void throwsWhenFromIsAfterTo() {
        StatisticsFilter filter = new StatisticsFilter(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 1, 1),
                StatisticsPeriod.MONTHLY,
                List.of(),
                List.of(1L),
                List.of(),
                null,
                null
        );

        assertThatThrownBy(() -> statisticsQueryService.getSalesTrendByClient(principal(Role.ADMIN, 1L), filter))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_INPUT_VALUE);

        verifyNoInteractions(statisticsRepository);
    }

    @Test
    @DisplayName("선택 필터가 비어 있으면 INVALID_INPUT_VALUE 예외가 발생한다")
    void throwsWhenSelectionFilterIsEmpty() {
        StatisticsFilter filter = new StatisticsFilter(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31),
                StatisticsPeriod.MONTHLY,
                List.of(),
                List.of(),
                List.of(),
                StatisticsRankingType.CLIENT,
                null
        );

        assertThatThrownBy(() -> statisticsQueryService.getRanking(principal(Role.ADMIN, 1L), filter))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_INPUT_VALUE);

        verifyNoInteractions(statisticsRepository);
    }

    @Test
    @DisplayName("MONTHLY 추이는 누락 월을 0으로 채운다")
    void fillsMissingMonthlyPeriodsWithZero() {
        setAdminAuthentication(1L);
        StatisticsFilter filter = new StatisticsFilter(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31),
                StatisticsPeriod.MONTHLY,
                List.of(1L),
                List.of(),
                List.of(),
                null,
                null
        );

        Employee employee = Employee.builder()
                .employeeCode("EMP-1")
                .employeeName("직원1")
                .employeeEmail("e1@test.com")
                .employeePhone("010-0000-0001")
                .address("서울")
                .build();
        setEntityId(employee, 1L);

        when(employeeRepository.findAllById(List.of(1L))).thenReturn(List.of(employee));
        when(statisticsRepository.findSalesTrendByEmployee(eq(filter.withLimit(10)), eq(null)))
                .thenReturn(List.of(new StatisticsRepository.TrendBucketRow("1", "직원1", "2026-02", new BigDecimal("1500"))));

        List<SalesTrendDto> result = statisticsQueryService.getSalesTrendByEmployee(filter);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).data()).hasSize(3);
        assertThat(result.get(0).data().get(0).period()).isEqualTo("2026-01");
        assertThat(result.get(0).data().get(0).sales()).isEqualByComparingTo("0");
        assertThat(result.get(0).data().get(1).sales()).isEqualByComparingTo("1500");
        assertThat(result.get(0).data().get(2).period()).isEqualTo("2026-03");
    }

    @Test
    @DisplayName("QUARTERLY 추이는 누락 분기를 0으로 채운다")
    void fillsMissingQuarterlyPeriodsWithZero() {
        StatisticsFilter filter = new StatisticsFilter(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 9, 30),
                StatisticsPeriod.QUARTERLY,
                List.of(),
                List.of(10L),
                List.of(),
                null,
                null
        );
        Client client = Client.builder()
                .clientCode("CLIENT-10")
                .clientName("거래처10")
                .clientBrn("BRN-10")
                .ceoName("대표")
                .companyPhone("02-0000-0000")
                .address("서울")
                .clientType(com.monsoon.seedflowplus.domain.account.entity.ClientType.NURSERY)
                .managerName("담당자")
                .managerPhone("010-0000-0000")
                .managerEmail("manager@test.com")
                .build();
        setEntityId(client, 10L);

        when(clientRepository.findAllById(List.of(10L))).thenReturn(List.of(client));
        when(statisticsRepository.findSalesTrendByClient(eq(filter.withLimit(10)), eq(null)))
                .thenReturn(List.of(new StatisticsRepository.TrendBucketRow("10", "거래처10", "2026-Q2", new BigDecimal("700"))));

        List<SalesTrendDto> result = statisticsQueryService.getSalesTrendByClient(principal(Role.ADMIN, 1L), filter);

        assertThat(result.get(0).data()).extracting(item -> item.period())
                .containsExactly("2026-Q1", "2026-Q2", "2026-Q3");
        assertThat(result.get(0).data()).extracting(item -> item.sales())
                .containsExactly(new BigDecimal("0"), new BigDecimal("700"), new BigDecimal("0"));
    }

    @Test
    @DisplayName("SALES_REP by-client 조회는 담당 거래처 범위를 자동 필터링한다")
    void salesRepClientScopeIsApplied() {
        StatisticsFilter filter = new StatisticsFilter(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31),
                StatisticsPeriod.MONTHLY,
                List.of(),
                List.of(10L),
                List.of(),
                null,
                null
        );
        Client managedClient = Client.builder()
                .clientCode("CLIENT-10")
                .clientName("거래처10")
                .clientBrn("BRN-10")
                .ceoName("대표")
                .companyPhone("02-0000-0000")
                .address("서울")
                .clientType(com.monsoon.seedflowplus.domain.account.entity.ClientType.NURSERY)
                .managerName("담당자")
                .managerPhone("010-0000-0000")
                .managerEmail("manager@test.com")
                .build();
        setEntityId(managedClient, 10L);

        when(clientRepository.findAllByManagerEmployeeId(7L)).thenReturn(List.of(managedClient));
        when(clientRepository.findAllById(List.of(10L))).thenReturn(List.of(managedClient));
        when(statisticsRepository.findSalesTrendByClient(eq(filter.withLimit(10)), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(new StatisticsRepository.TrendBucketRow("10", "거래처10", "2026-01", BigDecimal.TEN)));

        List<SalesTrendDto> result = statisticsQueryService.getSalesTrendByClient(principal(Role.SALES_REP, 7L), filter);

        assertThat(result).hasSize(1);
        verify(clientRepository).findAllByManagerEmployeeId(7L);
        verify(statisticsRepository).findSalesTrendByClient(eq(filter.withLimit(10)), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("본인 매출 추이는 principal employeeId를 사용한다")
    void mySalesTrendUsesPrincipalEmployeeId() {
        StatisticsFilter filter = new StatisticsFilter(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                StatisticsPeriod.MONTHLY,
                List.of(),
                List.of(),
                List.of(),
                null,
                null
        );
        Employee employee = Employee.builder()
                .employeeCode("EMP-7")
                .employeeName("직원7")
                .employeeEmail("e7@test.com")
                .employeePhone("010-0000-0007")
                .address("서울")
                .build();
        setEntityId(employee, 7L);

        when(employeeRepository.findById(7L)).thenReturn(Optional.of(employee));
        when(statisticsRepository.findMySalesTrend(filter.withLimit(10), 7L))
                .thenReturn(List.of(new StatisticsRepository.TrendBucketRow("7", "직원7", "2026-01", BigDecimal.ONE)));

        List<SalesTrendDto> result = statisticsQueryService.getMySalesTrend(principal(Role.SALES_REP, 7L), filter);

        assertThat(result.get(0).targetName()).isEqualTo("직원7");
        verify(statisticsRepository).findMySalesTrend(filter.withLimit(10), 7L);
    }

    private CustomUserDetails principal(Role role, Long employeeId) {
        CustomUserDetails principal = org.mockito.Mockito.mock(CustomUserDetails.class);
        lenient().when(principal.getRole()).thenReturn(role);
        lenient().when(principal.getEmployeeId()).thenReturn(employeeId);
        return principal;
    }

    private void setAdminAuthentication(Long employeeId) {
        CustomUserDetails principal = principal(Role.ADMIN, employeeId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private void setEntityId(Object entity, Long id) {
        try {
            Class<?> type = entity.getClass();
            java.lang.reflect.Field field = null;
            while (type != null && field == null) {
                try {
                    field = type.getDeclaredField("id");
                } catch (NoSuchFieldException ignored) {
                    type = type.getSuperclass();
                }
            }
            if (field == null) {
                throw new NoSuchFieldException("id");
            }
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
