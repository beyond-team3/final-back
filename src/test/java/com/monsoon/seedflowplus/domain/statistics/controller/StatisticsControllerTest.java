package com.monsoon.seedflowplus.domain.statistics.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.monsoon.seedflowplus.config.TestSecurityConfig;
import com.monsoon.seedflowplus.core.common.support.error.GlobalExceptionHandler;
import com.monsoon.seedflowplus.domain.statistics.dto.SalesRankingDto;
import com.monsoon.seedflowplus.domain.statistics.dto.SalesTrendDto;
import com.monsoon.seedflowplus.domain.statistics.dto.SalesTrendItemDto;
import com.monsoon.seedflowplus.domain.statistics.service.StatisticsQueryService;
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
        controllers = StatisticsController.class,
        properties = "spring.web.resources.add-mappings=false",
        excludeFilters = @ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = GlobalExceptionHandler.class
        )
)
@Import(TestSecurityConfig.class)
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticsQueryService statisticsQueryService;

    @Test
    @DisplayName("sales-rep 경로는 SALES_REP 권한으로 200을 반환한다")
    @WithMockUser(roles = "SALES_REP")
    void salesRepTrendReturns200ForSalesRep() throws Exception {
        when(statisticsQueryService.getMySalesTrend(any(), any()))
                .thenReturn(List.of(new SalesTrendDto(
                        "1",
                        "직원1",
                        List.of(new SalesTrendItemDto("2026-01", BigDecimal.ZERO))
                )));

        mockMvc.perform(get("/api/v1/statistics/sales-rep")
                        .param("from", "2026-01-01")
                        .param("to", "2026-03-31")
                        .param("period", "MONTHLY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].targetId").value("1"))
                .andExpect(jsonPath("$.data[0].data[0].period").value("2026-01"));
    }

    @Test
    @DisplayName("admin 경로는 ADMIN 권한으로 200을 반환한다")
    @WithMockUser(roles = "ADMIN")
    void adminTrendReturns200ForAdmin() throws Exception {
        when(statisticsQueryService.getAdminSalesTrend(any()))
                .thenReturn(List.of(new SalesTrendDto(
                        "ALL",
                        "전체",
                        List.of(new SalesTrendItemDto("2026-Q1", BigDecimal.ONE))
                )));

        mockMvc.perform(get("/api/v1/statistics/admin")
                        .param("from", "2026-01-01")
                        .param("to", "2026-03-31")
                        .param("period", "QUARTERLY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].targetName").value("전체"));
    }

    @Test
    @DisplayName("by-employee 경로는 SALES_REP 권한이면 403을 반환한다")
    @WithMockUser(roles = "SALES_REP")
    void byEmployeeReturns403ForSalesRep() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/by-employee")
                        .param("from", "2026-01-01")
                        .param("to", "2026-03-31")
                        .param("period", "MONTHLY")
                        .param("employeeIds", "1"))
                .andExpect(status().isForbidden());

        verify(statisticsQueryService, never()).getSalesTrendByEmployee(any());
    }

    @Test
    @DisplayName("by-client 경로는 비인증이면 401을 반환한다")
    void byClientReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/by-client")
                        .param("from", "2026-01-01")
                        .param("to", "2026-03-31")
                        .param("period", "MONTHLY")
                        .param("clientIds", "1"))
                .andExpect(status().isUnauthorized());

        verify(statisticsQueryService, never()).getSalesTrendByClient(any(), any());
    }

    @Test
    @DisplayName("ranking 경로는 필수 파라미터 누락 시 400을 반환한다")
    @WithMockUser(roles = "ADMIN")
    void rankingReturns400WhenRequiredParamMissing() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/ranking")
                        .param("from", "2026-01-01")
                        .param("period", "MONTHLY")
                        .param("type", "CLIENT")
                        .param("clientIds", "1"))
                .andExpect(status().isBadRequest());

        verify(statisticsQueryService, never()).getRanking(any(), any());
    }

    @Test
    @DisplayName("ranking 경로는 ADMIN 권한으로 200을 반환한다")
    @WithMockUser(roles = "ADMIN")
    void rankingReturns200ForAdmin() throws Exception {
        when(statisticsQueryService.getRanking(any(), any()))
                .thenReturn(List.of(new SalesRankingDto(1, "1", "거래처1", new BigDecimal("3000"))));

        mockMvc.perform(get("/api/v1/statistics/ranking")
                        .param("from", "2026-01-01")
                        .param("to", "2026-03-31")
                        .param("period", "MONTHLY")
                        .param("type", "CLIENT")
                        .param("clientIds", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].rank").value(1))
                .andExpect(jsonPath("$.data[0].sales").value(3000));
    }
}
