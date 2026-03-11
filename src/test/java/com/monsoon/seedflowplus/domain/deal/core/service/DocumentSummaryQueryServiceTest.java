package com.monsoon.seedflowplus.domain.deal.core.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.monsoon.seedflowplus.domain.deal.core.repository.DocumentSummaryRepository;
import com.monsoon.seedflowplus.domain.deal.core.repository.DocumentSummarySearchCondition;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
}
