package com.monsoon.seedflowplus.domain.deal.core.service;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.deal.core.dto.response.DocumentSummaryResponse;
import com.monsoon.seedflowplus.domain.deal.core.entity.DocumentSummary;
import com.monsoon.seedflowplus.domain.deal.core.repository.DocumentSummaryRepository;
import com.monsoon.seedflowplus.domain.deal.core.repository.DocumentSummarySearchCondition;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentSummaryQueryService {

    private final DocumentSummaryRepository documentSummaryRepository;

    public Page<DocumentSummaryResponse> getDocuments(
            DocumentSummarySearchCondition condition,
            Pageable pageable,
            CustomUserDetails userDetails
    ) {
        validateUserDetails(userDetails);

        return documentSummaryRepository.searchDocuments(condition, pageable, userDetails)
                .map(this::toResponse);
    }

    private void validateUserDetails(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getRole() == null) {
            throw new AccessDeniedException("사용자 권한 정보가 없습니다.");
        }

        Role role = userDetails.getRole();
        if (role == Role.ADMIN) {
            return;
        }

        if (role == Role.SALES_REP) {
            if (userDetails.getEmployeeId() == null) {
                throw new AccessDeniedException("영업사원 사용자에 employeeId가 없습니다.");
            }
            return;
        }

        if (role == Role.CLIENT) {
            if (userDetails.getClientId() == null) {
                throw new AccessDeniedException("거래처 사용자에 clientId가 없습니다.");
            }
            return;
        }

        throw new AccessDeniedException("허용되지 않은 역할입니다: " + role);
    }

    private DocumentSummaryResponse toResponse(DocumentSummary documentSummary) {
        return new DocumentSummaryResponse(
                documentSummary.getSurrogateId(),
                documentSummary.getDocType(),
                documentSummary.getDocId(),
                documentSummary.getDocCode(),
                documentSummary.getAmount(),
                documentSummary.getExpiredDate(),
                documentSummary.getStatus(),
                documentSummary.getCreatedAt(),
                documentSummary.getClientName(),
                documentSummary.getOwnerEmployeeName()
        );
    }
}
