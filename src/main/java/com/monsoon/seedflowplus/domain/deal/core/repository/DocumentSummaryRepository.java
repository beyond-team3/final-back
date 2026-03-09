package com.monsoon.seedflowplus.domain.deal.core.repository;

import com.monsoon.seedflowplus.domain.deal.core.entity.DocumentSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentSummaryRepository
        extends JpaRepository<DocumentSummary, String>, DocumentSummaryQueryRepository {
}
