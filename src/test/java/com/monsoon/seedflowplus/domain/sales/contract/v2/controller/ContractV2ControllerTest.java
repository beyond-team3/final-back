package com.monsoon.seedflowplus.domain.sales.contract.v2.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monsoon.seedflowplus.config.TestSecurityConfig;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.v2.dto.DealDocumentCommandResultDto;
import com.monsoon.seedflowplus.domain.sales.contract.v2.service.ContractV2CommandService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ContractV2Controller.class)
@Import(TestSecurityConfig.class)
class ContractV2ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContractV2CommandService contractV2CommandService;

    @Test
    @DisplayName("계약서 v2 생성은 SALES_REP 권한으로 200을 반환한다")
    @WithMockUser(roles = "SALES_REP")
    void createContractReturns200ForSalesRep() throws Exception {
        when(contractV2CommandService.createContract(any()))
                .thenReturn(DealDocumentCommandResultDto.builder()
                        .dealId(1L)
                        .documentType(DealType.CNT)
                        .documentId(20L)
                        .documentCode("CNT-20260315-20")
                        .build());

        mockMvc.perform(post("/api/v2/contracts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new ContractCreateBody(
                                        null,
                                        null,
                                        1L,
                                        "2026-03-15",
                                        "2026-06-15",
                                        "MONTHLY",
                                        "특약",
                                        "메모",
                                        List.of(new ItemBody(1L, "상추", "채소", 10, "EA", 1000))
                                )
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentType").value("CNT"));
    }

    @Test
    @DisplayName("계약서 v2 생성은 CLIENT 권한이면 403을 반환한다")
    @WithMockUser(roles = "CLIENT")
    void createContractReturns403ForClient() throws Exception {
        mockMvc.perform(post("/api/v2/contracts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new ContractCreateBody(
                                        null,
                                        null,
                                        1L,
                                        "2026-03-15",
                                        "2026-06-15",
                                        "MONTHLY",
                                        "특약",
                                        "메모",
                                        List.of(new ItemBody(1L, "상추", "채소", 10, "EA", 1000))
                                )
                        )))
                .andExpect(status().isForbidden());

        verify(contractV2CommandService, never()).createContract(any());
    }

    @Test
    @DisplayName("계약서 v2 취소는 비인증이면 401을 반환한다")
    void cancelContractReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(patch("/api/v2/contracts/5/cancel"))
                .andExpect(status().isUnauthorized());

        verify(contractV2CommandService, never()).cancelContract(any());
    }

    private record ContractCreateBody(
            Long quotationId,
            Long dealId,
            Long clientId,
            String startDate,
            String endDate,
            String billingCycle,
            String specialTerms,
            String memo,
            List<ItemBody> items
    ) {
    }

    private record ItemBody(
            Long productId,
            String productName,
            String productCategory,
            Integer totalQuantity,
            String unit,
            Integer unitPrice
    ) {
    }
}
