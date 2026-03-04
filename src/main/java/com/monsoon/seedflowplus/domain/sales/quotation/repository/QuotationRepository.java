package com.monsoon.seedflowplus.domain.sales.quotation.repository;

import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuotationRepository extends JpaRepository<QuotationHeader, Long> {
    Optional<QuotationHeader> findByQuotationCode(String quotationCode);

    List<QuotationHeader> findAllByStatus(QuotationStatus status);

    List<QuotationHeader> findAllByStatusAndAuthorId(QuotationStatus status, Long authorId);
}
