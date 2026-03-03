package com.monsoon.seedflowplus.domain.sales.request.repository;

import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuotationRequestRepository extends JpaRepository<QuotationRequestHeader, Long> {
}
