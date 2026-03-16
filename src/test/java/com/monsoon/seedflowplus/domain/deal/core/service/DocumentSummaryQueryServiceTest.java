package com.monsoon.seedflowplus.domain.deal.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.dto.response.DocumentSummaryResponse;
import com.monsoon.seedflowplus.domain.deal.core.entity.DocumentSummary;
import com.monsoon.seedflowplus.domain.deal.core.repository.DocumentSummaryRepository;
import com.monsoon.seedflowplus.domain.deal.core.repository.DocumentSummarySearchCondition;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class DocumentSummaryQueryServiceTest {

    @Mock
    private DocumentSummaryRepository documentSummaryRepository;

    @Test
    @DisplayName("권한 정보가 없으면 문서 목록 조회를 거부한다")
    void getDocumentsRejectsMissingRole() {
        DocumentSummaryQueryService service = new DocumentSummaryQueryService(documentSummaryRepository);
        CustomUserDetails principal = mock(CustomUserDetails.class);

        assertThatThrownBy(() -> service.getDocuments(
                DocumentSummarySearchCondition.builder().build(),
                PageRequest.of(0, 20),
                principal
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("사용자 권한 정보가 없습니다.");

        verifyNoInteractions(documentSummaryRepository);
    }

    @Test
    @DisplayName("문서 목록 응답에 거래처명과 담당자명을 매핑한다")
    void getDocumentsMapsClientAndOwnerNames() {
        DocumentSummaryQueryService service = new DocumentSummaryQueryService(documentSummaryRepository);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getRole()).thenReturn(Role.ADMIN);

        DocumentSummary documentSummary = mock(DocumentSummary.class);
        when(documentSummary.getSurrogateId()).thenReturn("STMT-1");
        when(documentSummary.getDocType()).thenReturn(DealType.STMT);
        when(documentSummary.getDocId()).thenReturn(1L);
        when(documentSummary.getDocCode()).thenReturn("STMT-001");
        when(documentSummary.getStatus()).thenReturn("ISSUED");
        when(documentSummary.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 3, 10, 9, 0));
        when(documentSummary.getClientName()).thenReturn("거래처A");
        when(documentSummary.getOwnerEmployeeName()).thenReturn("담당자A");

        when(documentSummaryRepository.searchDocuments(
                org.mockito.ArgumentMatchers.any(DocumentSummarySearchCondition.class),
                org.mockito.ArgumentMatchers.any(Pageable.class),
                org.mockito.ArgumentMatchers.same(principal)
        )).thenReturn(new PageImpl<>(List.of(documentSummary)));

        List<DocumentSummaryResponse> result = service.getDocuments(
                DocumentSummarySearchCondition.builder().build(),
                PageRequest.of(0, 20),
                principal
        ).getContent();

        assertThat(result)
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.clientName()).isEqualTo("거래처A");
                    assertThat(response.ownerEmployeeName()).isEqualTo("담당자A");
                });
    }
}
