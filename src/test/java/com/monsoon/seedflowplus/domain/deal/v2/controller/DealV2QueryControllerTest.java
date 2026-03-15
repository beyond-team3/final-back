package com.monsoon.seedflowplus.domain.deal.v2.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.monsoon.seedflowplus.config.TestSecurityConfig;
import com.monsoon.seedflowplus.core.common.support.error.GlobalExceptionHandler;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.v2.dto.DealDetailDto;
import com.monsoon.seedflowplus.domain.deal.v2.dto.DealKpiDto;
import com.monsoon.seedflowplus.domain.deal.v2.dto.DealSnapshotDto;
import com.monsoon.seedflowplus.domain.deal.v2.dto.DealSummaryDto;
import com.monsoon.seedflowplus.domain.deal.v2.service.DealV2ContextQueryService;
import com.monsoon.seedflowplus.domain.deal.v2.service.DealV2KpiQueryService;
import com.monsoon.seedflowplus.domain.deal.v2.service.DealV2QueryService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = DealV2QueryController.class,
        properties = "spring.web.resources.add-mappings=false",
        excludeFilters = @ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = GlobalExceptionHandler.class
        )
)
@Import(TestSecurityConfig.class)
class DealV2QueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DealV2QueryService dealV2QueryService;

    @MockBean
    private DealV2ContextQueryService dealV2ContextQueryService;

    @MockBean
    private DealV2KpiQueryService dealV2KpiQueryService;

    @Test
    @DisplayName("deal 목록 조회는 인증 사용자로 200을 반환한다")
    void getDealsReturns200ForAuthenticatedUser() throws Exception {
        CustomUserDetails principal = adminPrincipal();
        when(dealV2QueryService.getDeals(any(), any(Pageable.class), same(principal)))
                .thenReturn(new PageImpl<>(List.of(
                        DealSummaryDto.builder()
                                .dealId(1L)
                                .clientName("거래처A")
                                .snapshot(DealSnapshotDto.builder()
                                        .currentStage(DealStage.PENDING_ADMIN)
                                        .currentStatus("WAITING_ADMIN")
                                        .lastActivityAt(LocalDateTime.of(2026, 3, 15, 10, 0))
                                        .build())
                                .build()
                )));

        mockMvc.perform(get("/api/v2/deals")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].dealId").value(1L));
    }

    @Test
    @DisplayName("deal KPI 조회는 비인증이면 401을 반환한다")
    void getKpisReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v2/deals/kpis"))
                .andExpect(status().isUnauthorized());

        verify(dealV2KpiQueryService, never()).getKpis(any(), any());
    }

    @Test
    @DisplayName("deal 상세 조회는 잘못된 날짜 파라미터가 없으면 200을 반환한다")
    void getDealReturns200() throws Exception {
        CustomUserDetails principal = adminPrincipal();
        when(dealV2QueryService.getDeal(3L, principal))
                .thenReturn(DealDetailDto.builder()
                        .dealId(3L)
                        .dealTitle("봄 수주")
                        .snapshot(DealSnapshotDto.builder()
                                .currentStage(DealStage.APPROVED)
                                .currentStatus("COMPLETED")
                                .build())
                        .build());

        mockMvc.perform(get("/api/v2/deals/3")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dealId").value(3L))
                .andExpect(jsonPath("$.data.dealTitle").value("봄 수주"));
    }

    @Test
    @DisplayName("deal KPI 조회는 인증 사용자로 200을 반환한다")
    void getKpisReturns200ForAuthenticatedUser() throws Exception {
        CustomUserDetails principal = adminPrincipal();
        when(dealV2KpiQueryService.getKpis(any(), same(principal)))
                .thenReturn(DealKpiDto.builder()
                        .dealCount(10)
                        .successfulDealCount(4)
                        .successRate(new BigDecimal("40.00"))
                        .build());

        mockMvc.perform(get("/api/v2/deals/kpis")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()
                        )))
                        .param("fromAt", "2026-03-01T00:00:00")
                        .param("toAt", "2026-03-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dealCount").value(10))
                .andExpect(jsonPath("$.data.successRate").value(40.00));
    }

    private CustomUserDetails adminPrincipal() {
        CustomUserDetails principal = Mockito.mock(CustomUserDetails.class);
        when(principal.getRole()).thenReturn(Role.ADMIN);
        when(principal.getUserId()).thenReturn(1L);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        doReturn(authorities).when(principal).getAuthorities();
        return principal;
    }
}
