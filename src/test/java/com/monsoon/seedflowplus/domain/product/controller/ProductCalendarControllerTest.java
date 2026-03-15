package com.monsoon.seedflowplus.domain.product.controller;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.monsoon.seedflowplus.config.TestSecurityConfig;
import com.monsoon.seedflowplus.core.common.support.error.GlobalExceptionHandler;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductCalendarRecommendationResponse;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductHarvestImminentResponse;
import com.monsoon.seedflowplus.domain.product.service.ProductReadService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = ProductCalendarController.class,
        properties = "spring.web.resources.add-mappings=false",
        excludeFilters = @ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = GlobalExceptionHandler.class
        )
)
@Import(TestSecurityConfig.class)
class ProductCalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductReadService productReadService;

    @Test
    @DisplayName("추천 품종 조회는 영업사원 인증으로 200을 반환한다")
    void getRecommendationsReturns200ForSalesRep() throws Exception {
        when(productReadService.getCalendarRecommendations(3))
                .thenReturn(ProductCalendarRecommendationResponse.builder()
                        .month(3)
                        .items(List.of(
                                ProductCalendarRecommendationResponse.RecommendedProductItem.builder()
                                        .productId(1L)
                                        .productName("봄 수박")
                                        .productCategory("WATERMELON")
                                        .productCategoryLabel("수박")
                                        .build()
                        ))
                        .build());

        CustomUserDetails principal = salesRepPrincipal();

        mockMvc.perform(get("/api/v1/products/calendar/recommendations")
                        .param("month", "3")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.month").value(3))
                .andExpect(jsonPath("$.data.items[0].productName").value("봄 수박"));
    }

    @Test
    @DisplayName("수확 임박 조회는 비인증이면 401을 반환한다")
    void getHarvestImminentReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/products/calendar/harvest-imminent"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productReadService);
    }

    @Test
    @DisplayName("캘린더 API는 거래처 권한이면 403을 반환한다")
    void calendarEndpointsReturn403ForClientRole() throws Exception {
        CustomUserDetails principal = clientPrincipal();

        mockMvc.perform(get("/api/v1/products/calendar/recommendations")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("수확 임박 조회는 영업사원 인증으로 200을 반환한다")
    void getHarvestImminentReturns200ForSalesRep() throws Exception {
        CustomUserDetails principal = salesRepPrincipal();
        when(productReadService.getHarvestImminent(3, principal))
                .thenReturn(ProductHarvestImminentResponse.builder()
                        .month(3)
                        .nextMonth(4)
                        .clients(List.of(
                                ProductHarvestImminentResponse.ClientHarvestImminentItem.builder()
                                        .clientId(1L)
                                        .clientName("거래처A")
                                        .crops(List.of())
                                        .build()
                        ))
                        .build());

        mockMvc.perform(get("/api/v1/products/calendar/harvest-imminent")
                        .param("month", "3")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clients[0].clientName").value("거래처A"));
    }

    private CustomUserDetails salesRepPrincipal() {
        CustomUserDetails principal = Mockito.mock(CustomUserDetails.class);
        when(principal.getRole()).thenReturn(Role.SALES_REP);
        when(principal.getEmployeeId()).thenReturn(7L);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_SALES_REP"))).when(principal).getAuthorities();
        return principal;
    }

    private CustomUserDetails clientPrincipal() {
        CustomUserDetails principal = Mockito.mock(CustomUserDetails.class);
        when(principal.getRole()).thenReturn(Role.CLIENT);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))).when(principal).getAuthorities();
        return principal;
    }
}
