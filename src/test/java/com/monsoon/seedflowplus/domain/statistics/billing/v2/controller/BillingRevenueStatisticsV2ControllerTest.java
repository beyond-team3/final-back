package com.monsoon.seedflowplus.domain.statistics.billing.v2.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.monsoon.seedflowplus.config.TestSecurityConfig;
import com.monsoon.seedflowplus.core.common.support.error.GlobalExceptionHandler;
import com.monsoon.seedflowplus.domain.statistics.billing.dto.response.MonthlyBilledRevenueDto;
import com.monsoon.seedflowplus.domain.statistics.billing.service.BillingRevenueStatisticsQueryService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = BillingRevenueStatisticsV2Controller.class,
        properties = "spring.web.resources.add-mappings=false",
        excludeFilters = @ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = GlobalExceptionHandler.class
        )
)
@Import(TestSecurityConfig.class)
class BillingRevenueStatisticsV2ControllerTest {

    private static final String BASE_PATH = "/api/v2/statistics/billing/revenue";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BillingRevenueStatisticsQueryService queryService;

    @Test
    @DisplayName("v2 월별 청구 매출은 SALES_REP 권한으로 200을 반환한다")
    @WithMockUser(roles = "SALES_REP")
    void monthlyRevenueReturns200ForSalesRep() throws Exception {
        when(queryService.getMonthlyRevenue(any(), any()))
                .thenReturn(List.of(new MonthlyBilledRevenueDto("2026-03", new BigDecimal("1200"))));

        mockMvc.perform(get(BASE_PATH + "/monthly")
                        .param("from", "2026-03-01")
                        .param("to", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].month").value("2026-03"))
                .andExpect(jsonPath("$.data[0].billedRevenue").value(1200));
    }

    @Test
    @DisplayName("v2 월별 청구 매출은 CLIENT 권한이면 403을 반환한다")
    @WithMockUser(roles = "CLIENT")
    void monthlyRevenueReturns403ForClient() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/monthly")
                        .param("from", "2026-03-01")
                        .param("to", "2026-03-31"))
                .andExpect(status().isForbidden());

        verify(queryService, never()).getMonthlyRevenue(any(), any());
    }

    @Test
    @DisplayName("v2 월별 청구 매출은 비인증이면 401을 반환한다")
    void monthlyRevenueReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/monthly")
                        .param("from", "2026-03-01")
                        .param("to", "2026-03-31"))
                .andExpect(status().isUnauthorized());

        verify(queryService, never()).getMonthlyRevenue(any(), any());
    }
}
