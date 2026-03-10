package com.monsoon.seedflowplus.domain.deal.core.repository;

import com.monsoon.seedflowplus.domain.deal.core.entity.DocumentSummary;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.Repository;

public interface DocumentSummaryRepository
        extends Repository<DocumentSummary, String>,
        QuerydslPredicateExecutor<DocumentSummary>,
        DocumentSummaryQueryRepository {
}
