package com.monsoon.seedflowplus.domain.statistics.billing.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.monsoon.seedflowplus.domain.statistics.billing.dto.request.BillingRevenueStatisticsFilter;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.CategoryBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.MonthlyBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.MonthlyCategoryBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.service.BillingRevenueStatisticsQueryService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class BillingRevenueStatisticsControllerTest {

    @Mock
    private BillingRevenueStatisticsQueryService queryService;

    @InjectMocks
    private BillingRevenueStatisticsController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("월별 API는 from/to/category를 필터에 바인딩해 QueryService에 전달한다")
    void shouldBindParamsAndCallQueryService() throws Exception {
        when(queryService.getMonthlyRevenue(any(BillingRevenueStatisticsFilter.class), any()))
                .thenReturn(List.of(new MonthlyBilledRevenueDto("2026-01", new BigDecimal("1000"))));

        mockMvc.perform(get("/statistics/billing/revenue/monthly")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31")
                        .param("category", "수박"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].month").value("2026-01"))
                .andExpect(jsonPath("$.data[0].billedRevenue").value(1000));

        ArgumentCaptor<BillingRevenueStatisticsFilter> captor =
                ArgumentCaptor.forClass(BillingRevenueStatisticsFilter.class);
        verify(queryService).getMonthlyRevenue(captor.capture(), isNull());
        BillingRevenueStatisticsFilter captured = captor.getValue();

        org.assertj.core.api.Assertions.assertThat(captured.getFromDate().toString()).isEqualTo("2026-01-01");
        org.assertj.core.api.Assertions.assertThat(captured.getToDate().toString()).isEqualTo("2026-12-31");
        org.assertj.core.api.Assertions.assertThat(captured.getCategory()).isEqualTo("수박");
    }

    @Test
    @DisplayName("품종별 API는 success 래퍼로 응답한다")
    void shouldWrapCategoryRevenueWithSuccessResponse() throws Exception {
        when(queryService.getCategoryRevenue(any(BillingRevenueStatisticsFilter.class), any()))
                .thenReturn(List.of(new CategoryBilledRevenueDto("수박", new BigDecimal("700"))));

        mockMvc.perform(get("/statistics/billing/revenue/by-category")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].category").value("수박"))
                .andExpect(jsonPath("$.data[0].billedRevenue").value(700));
    }

    @Test
    @DisplayName("월별 품종별 API는 from/to/category를 필터에 바인딩해 QueryService에 전달한다")
    void shouldBindParamsForMonthlyCategoryApi() throws Exception {
        when(queryService.getMonthlyCategoryRevenue(any(BillingRevenueStatisticsFilter.class), any()))
                .thenReturn(List.of(new MonthlyCategoryBilledRevenueDto("2026-01", "수박", new BigDecimal("700"))));

        mockMvc.perform(get("/statistics/billing/revenue/monthly-by-category")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31")
                        .param("category", "수박"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].month").value("2026-01"))
                .andExpect(jsonPath("$.data[0].category").value("수박"))
                .andExpect(jsonPath("$.data[0].billedRevenue").value(700));

        ArgumentCaptor<BillingRevenueStatisticsFilter> captor =
                ArgumentCaptor.forClass(BillingRevenueStatisticsFilter.class);
        verify(queryService).getMonthlyCategoryRevenue(captor.capture(), isNull());
        BillingRevenueStatisticsFilter captured = captor.getValue();

        org.assertj.core.api.Assertions.assertThat(captured.getFromDate().toString()).isEqualTo("2026-01-01");
        org.assertj.core.api.Assertions.assertThat(captured.getToDate().toString()).isEqualTo("2026-12-31");
        org.assertj.core.api.Assertions.assertThat(captured.getCategory()).isEqualTo("수박");
    }

    @Test
    @DisplayName("구 경로(/api/v1/statistics/...)는 매핑되지 않아 404를 반환한다")
    void shouldReturn404ForLegacyPath() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/billing/revenue/monthly")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isNotFound());

        verify(queryService, never()).getMonthlyRevenue(any(BillingRevenueStatisticsFilter.class), any());
    }

    @Test
    @DisplayName("날짜 형식이 잘못되면 400을 반환하고 QueryService를 호출하지 않는다")
    void shouldReturn400WhenDateFormatInvalid() throws Exception {
        mockMvc.perform(get("/statistics/billing/revenue/monthly")
                        .param("from", "2026/01/01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isBadRequest());

        verify(queryService, never()).getMonthlyRevenue(any(BillingRevenueStatisticsFilter.class), any());
    }

    @Test
    @DisplayName("월별 API는 인증 사용자를 QueryService에 그대로 전달한다")
    void shouldPassPrincipalToQueryService() {
        CustomUserDetails principal = org.mockito.Mockito.mock(CustomUserDetails.class);
        BillingRevenueStatisticsFilter expectedFilter = new BillingRevenueStatisticsFilter(
                java.time.LocalDate.of(2026, 1, 1),
                java.time.LocalDate.of(2026, 12, 31),
                "수박"
        );

        when(queryService.getMonthlyRevenue(any(BillingRevenueStatisticsFilter.class), same(principal)))
                .thenReturn(List.of());

        controller.getMonthlyRevenue(principal, expectedFilter.getFromDate(), expectedFilter.getToDate(), expectedFilter.getCategory());

        ArgumentCaptor<BillingRevenueStatisticsFilter> captor =
                ArgumentCaptor.forClass(BillingRevenueStatisticsFilter.class);
        verify(queryService).getMonthlyRevenue(captor.capture(), same(principal));

        BillingRevenueStatisticsFilter captured = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(captured.getFromDate()).isEqualTo(expectedFilter.getFromDate());
        org.assertj.core.api.Assertions.assertThat(captured.getToDate()).isEqualTo(expectedFilter.getToDate());
        org.assertj.core.api.Assertions.assertThat(captured.getCategory()).isEqualTo(expectedFilter.getCategory());
    }
}
