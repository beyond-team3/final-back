package com.monsoon.seedflowplus.domain.billing.invoice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.monsoon.seedflowplus.config.TestSecurityConfig;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.billing.invoice.dto.response.InvoiceDetailResponse;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatus;
import com.monsoon.seedflowplus.domain.billing.invoice.service.InvoiceService;
import org.mockito.Mockito;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InvoiceController.class)
@Import(TestSecurityConfig.class)
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoiceService invoiceService;

    @Test
    @DisplayName("관리자는 계약 기준 수동 청구서 초안 생성을 호출할 수 있다")
    void createInvoiceDraftManuallyReturns200ForAdmin() throws Exception {
        when(invoiceService.createDraftInvoiceByAdmin(10L))
                .thenReturn(InvoiceDetailResponse.builder()
                        .invoiceId(41L)
                        .invoiceCode("INV-20260315-001")
                        .contractId(10L)
                        .status(InvoiceStatus.DRAFT)
                        .build());

        CustomUserDetails principal = adminPrincipal();
        mockMvc.perform(post("/api/v1/invoices/contracts/10/manual-draft")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.invoiceId").value(41L))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @DisplayName("영업사원은 계약 기준 수동 청구서 초안 생성에 접근할 수 없다")
    @WithMockUser(roles = "SALES_REP")
    void createInvoiceDraftManuallyReturns403ForSalesRep() throws Exception {
        mockMvc.perform(post("/api/v1/invoices/contracts/10/manual-draft"))
                .andExpect(status().isForbidden());

        verify(invoiceService, never()).createDraftInvoiceByAdmin(any());
    }

    @Test
    @DisplayName("비인증 사용자는 계약 기준 수동 청구서 초안 생성에 접근할 수 없다")
    void createInvoiceDraftManuallyReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/invoices/contracts/10/manual-draft"))
                .andExpect(status().isUnauthorized());

        verify(invoiceService, never()).createDraftInvoiceByAdmin(any());
    }

    private CustomUserDetails adminPrincipal() {
        User user = User.builder()
                .loginId("admin")
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.ADMIN)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 1L);
        return new CustomUserDetails(user);
    }
}
