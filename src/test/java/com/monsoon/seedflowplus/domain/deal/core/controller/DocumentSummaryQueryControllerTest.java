package com.monsoon.seedflowplus.domain.deal.core.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.monsoon.seedflowplus.config.TestSecurityConfig;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.core.common.support.error.GlobalExceptionHandler;
import com.monsoon.seedflowplus.domain.deal.core.dto.response.DocumentSummaryResponse;
import com.monsoon.seedflowplus.domain.deal.core.repository.DocumentSummarySearchCondition;
import com.monsoon.seedflowplus.domain.deal.core.service.DocumentSummaryQueryService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = DocumentSummaryQueryController.class,
        properties = "spring.web.resources.add-mappings=false",
        excludeFilters = @ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = GlobalExceptionHandler.class
        )
)
@Import(TestSecurityConfig.class)
class DocumentSummaryQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentSummaryQueryService documentSummaryQueryService;

    @Test
    @DisplayName("sort=createdAt,asc 요청은 오름차순 Pageable로 QueryService에 전달된다")
    void getDocumentsPassesAscendingSortToService() throws Exception {
        CustomUserDetails principal = org.mockito.Mockito.mock(CustomUserDetails.class);
        Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(principal.getRole()).thenReturn(Role.ADMIN);
        doReturn(authorities).when(principal).getAuthorities();

        when(documentSummaryQueryService.getDocuments(any(DocumentSummarySearchCondition.class), any(Pageable.class), same(principal)))
                .thenReturn(new PageImpl<>(List.of(new DocumentSummaryResponse(
                        "STMT-1",
                        null,
                        1L,
                        "STMT-001",
                        null,
                        null,
                        "ISSUED",
                        LocalDateTime.of(2026, 3, 10, 9, 0),
                        null,
                        null
                ))));

        mockMvc.perform(get("/api/v1/documents")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.getAuthorities()
                        )))
                        .param("docType", "STMT")
                        .param("sort", "createdAt,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].docCode").value("STMT-001"));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(documentSummaryQueryService).getDocuments(any(DocumentSummarySearchCondition.class), pageableCaptor.capture(), same(principal));

        Pageable pageable = pageableCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(pageable.getSort().getOrderFor("createdAt"))
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.ASC);
    }
}
