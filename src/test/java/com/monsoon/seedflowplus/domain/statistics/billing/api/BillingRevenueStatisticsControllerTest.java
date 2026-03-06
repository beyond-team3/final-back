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

import com.monsoon.seedflowplus.config.TestSecurityConfig;
import com.monsoon.seedflowplus.core.common.support.error.GlobalExceptionHandler;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.request.BillingRevenueStatisticsFilter;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.CategoryBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.MonthlyBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.MonthlyCategoryBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.service.BillingRevenueStatisticsQueryService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = BillingRevenueStatisticsController.class,
        properties = "spring.web.resources.add-mappings=false",
        excludeFilters = @ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = GlobalExceptionHandler.class
        )
)
@Import(TestSecurityConfig.class)
class BillingRevenueStatisticsControllerTest {

    private static final String BASE_PATH = "/api/v1/statistics/billing/revenue";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BillingRevenueStatisticsController controller;

    @MockBean
    private BillingRevenueStatisticsQueryService queryService;

    @Test
    @DisplayName("월별 API는 from/to/category를 필터에 바인딩해 QueryService에 전달한다")
    @WithMockUser(roles = "ADMIN")
    void shouldBindParamsAndCallQueryService() throws Exception {
        when(queryService.getMonthlyRevenue(any(BillingRevenueStatisticsFilter.class), isNull()))
                .thenReturn(List.of(new MonthlyBilledRevenueDto("2026-01", new BigDecimal("1000"))));

        mockMvc.perform(get(BASE_PATH + "/monthly")
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
    @WithMockUser(roles = "ADMIN")
    void shouldWrapCategoryRevenueWithSuccessResponse() throws Exception {
        when(queryService.getCategoryRevenue(any(BillingRevenueStatisticsFilter.class), isNull()))
                .thenReturn(List.of(new CategoryBilledRevenueDto("수박", new BigDecimal("700"))));

        mockMvc.perform(get(BASE_PATH + "/by-category")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].category").value("수박"))
                .andExpect(jsonPath("$.data[0].billedRevenue").value(700));
    }

    @Test
    @DisplayName("월별 품종별 API는 from/to/category를 필터에 바인딩해 QueryService에 전달한다")
    @WithMockUser(roles = "ADMIN")
    void shouldBindParamsForMonthlyCategoryApi() throws Exception {
        when(queryService.getMonthlyCategoryRevenue(any(BillingRevenueStatisticsFilter.class), isNull()))
                .thenReturn(List.of(new MonthlyCategoryBilledRevenueDto("2026-01", "수박", new BigDecimal("700"))));

        mockMvc.perform(get(BASE_PATH + "/monthly-by-category")
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
    @DisplayName("구 경로(/statistics/...)는 매핑되지 않아 404를 반환한다")
    @WithMockUser(roles = "ADMIN")
    void legacyPathReturns404() throws Exception {
        mockMvc.perform(get("/statistics/billing/revenue/monthly")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isNotFound());

        verify(queryService, never()).getMonthlyRevenue(any(BillingRevenueStatisticsFilter.class), any());
    }

    @Test
    @DisplayName("날짜 형식이 잘못되면 400을 반환하고 QueryService를 호출하지 않는다")
    @WithMockUser(roles = "ADMIN")
    void shouldReturn400WhenDateFormatInvalid() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/monthly")
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

    @Test
    @DisplayName("월별 API는 SALES_REP 권한으로 접근 가능하다")
    @WithMockUser(roles = "SALES_REP")
    void monthlyAllowsSalesRep() throws Exception {
        when(queryService.getMonthlyRevenue(any(BillingRevenueStatisticsFilter.class), isNull())).thenReturn(List.of());

        mockMvc.perform(get(BASE_PATH + "/monthly")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("월별 API는 ADMIN 권한으로 접근 가능하다")
    @WithMockUser(roles = "ADMIN")
    void monthlyAllowsAdmin() throws Exception {
        when(queryService.getMonthlyRevenue(any(BillingRevenueStatisticsFilter.class), isNull())).thenReturn(List.of());

        mockMvc.perform(get(BASE_PATH + "/monthly")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("월별 API는 허용되지 않은 역할이면 403을 반환한다")
    @WithMockUser(roles = "CLIENT")
    void monthlyReturns403ForClient() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/monthly")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isForbidden());

        verify(queryService, never()).getMonthlyRevenue(any(BillingRevenueStatisticsFilter.class), any());
    }

    @Test
    @DisplayName("월별 API는 비인증 요청이면 401을 반환한다")
    void monthlyReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/monthly")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isUnauthorized());

        verify(queryService, never()).getMonthlyRevenue(any(BillingRevenueStatisticsFilter.class), any());
    }

    @Test
    @DisplayName("품종별 API는 SALES_REP 권한으로 접근 가능하다")
    @WithMockUser(roles = "SALES_REP")
    void categoryAllowsSalesRep() throws Exception {
        when(queryService.getCategoryRevenue(any(BillingRevenueStatisticsFilter.class), isNull())).thenReturn(List.of());

        mockMvc.perform(get(BASE_PATH + "/by-category")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("품종별 API는 ADMIN 권한으로 접근 가능하다")
    @WithMockUser(roles = "ADMIN")
    void categoryAllowsAdmin() throws Exception {
        when(queryService.getCategoryRevenue(any(BillingRevenueStatisticsFilter.class), isNull())).thenReturn(List.of());

        mockMvc.perform(get(BASE_PATH + "/by-category")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("품종별 API는 허용되지 않은 역할이면 403을 반환한다")
    @WithMockUser(roles = "CLIENT")
    void categoryReturns403ForClient() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/by-category")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isForbidden());

        verify(queryService, never()).getCategoryRevenue(any(BillingRevenueStatisticsFilter.class), any());
    }

    @Test
    @DisplayName("품종별 API는 비인증 요청이면 401을 반환한다")
    void categoryReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/by-category")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isUnauthorized());

        verify(queryService, never()).getCategoryRevenue(any(BillingRevenueStatisticsFilter.class), any());
    }

    @Test
    @DisplayName("월별 품종별 API는 SALES_REP 권한으로 접근 가능하다")
    @WithMockUser(roles = "SALES_REP")
    void monthlyCategoryAllowsSalesRep() throws Exception {
        when(queryService.getMonthlyCategoryRevenue(any(BillingRevenueStatisticsFilter.class), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get(BASE_PATH + "/monthly-by-category")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("월별 품종별 API는 ADMIN 권한으로 접근 가능하다")
    @WithMockUser(roles = "ADMIN")
    void monthlyCategoryAllowsAdmin() throws Exception {
        when(queryService.getMonthlyCategoryRevenue(any(BillingRevenueStatisticsFilter.class), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get(BASE_PATH + "/monthly-by-category")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("월별 품종별 API는 허용되지 않은 역할이면 403을 반환한다")
    @WithMockUser(roles = "CLIENT")
    void monthlyCategoryReturns403ForClient() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/monthly-by-category")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isForbidden());

        verify(queryService, never()).getMonthlyCategoryRevenue(any(BillingRevenueStatisticsFilter.class), any());
    }

    @Test
    @DisplayName("월별 품종별 API는 비인증 요청이면 401을 반환한다")
    void monthlyCategoryReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/monthly-by-category")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isUnauthorized());

        verify(queryService, never()).getMonthlyCategoryRevenue(any(BillingRevenueStatisticsFilter.class), any());
    }
}
