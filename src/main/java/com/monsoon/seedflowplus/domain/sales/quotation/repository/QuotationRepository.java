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

    // 상태와 만료일을 기준으로 견적서 조회
    List<QuotationHeader> findByStatusAndExpiredDateLessThanEqual(QuotationStatus status, java.time.LocalDate date);
}
