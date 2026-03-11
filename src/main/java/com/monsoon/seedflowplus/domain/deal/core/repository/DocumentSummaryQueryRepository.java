package com.monsoon.seedflowplus.domain.deal.core.repository;

import com.monsoon.seedflowplus.domain.deal.core.entity.DocumentSummary;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DocumentSummaryQueryRepository {

    Page<DocumentSummary> searchDocuments(
            DocumentSummarySearchCondition cond,
            Pageable pageable,
            CustomUserDetails userDetails
    );
}
