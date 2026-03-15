package com.monsoon.seedflowplus.domain.sales.quotation.v2.controller;

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
import com.monsoon.seedflowplus.domain.sales.quotation.v2.service.QuotationV2CommandService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(QuotationV2Controller.class)
@Import(TestSecurityConfig.class)
class QuotationV2ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QuotationV2CommandService quotationV2CommandService;

    @Test
    @DisplayName("견적서 v2 생성은 SALES_REP 권한으로 200을 반환한다")
    @WithMockUser(roles = "SALES_REP")
    void createQuotationReturns200ForSalesRep() throws Exception {
        when(quotationV2CommandService.createQuotation(any()))
                .thenReturn(DealDocumentCommandResultDto.builder()
                        .dealId(1L)
                        .documentType(DealType.QUO)
                        .documentId(10L)
                        .documentCode("QUO-20260315-10")
                        .build());

        mockMvc.perform(post("/api/v2/quotations")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new QuotationCreateBody(
                                        null,
                                        null,
                                        1L,
                                        List.of(new ItemBody(1L, "상추", "채소", 10, "EA", 1000)),
                                        "메모"
                                )
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentType").value("QUO"));
    }

    @Test
    @DisplayName("견적서 v2 생성은 ADMIN 권한이면 403을 반환한다")
    @WithMockUser(roles = "ADMIN")
    void createQuotationReturns403ForAdmin() throws Exception {
        mockMvc.perform(post("/api/v2/quotations")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new QuotationCreateBody(
                                        null,
                                        null,
                                        1L,
                                        List.of(new ItemBody(1L, "상추", "채소", 10, "EA", 1000)),
                                        "메모"
                                )
                        )))
                .andExpect(status().isForbidden());

        verify(quotationV2CommandService, never()).createQuotation(any());
    }

    @Test
    @DisplayName("견적서 v2 취소는 비인증이면 401을 반환한다")
    void cancelQuotationReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(patch("/api/v2/quotations/3/cancel"))
                .andExpect(status().isUnauthorized());

        verify(quotationV2CommandService, never()).cancelQuotation(any());
    }

    private record QuotationCreateBody(
            Long requestId,
            Long dealId,
            Long clientId,
            List<ItemBody> items,
            String memo
    ) {
    }

    private record ItemBody(
            Long productId,
            String productName,
            String productCategory,
            Integer quantity,
            String unit,
            Integer unitPrice
    ) {
    }
}
