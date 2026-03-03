package com.monsoon.seedflowplus.domain.sales.request.repository;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuotationRequestRepository extends JpaRepository<QuotationRequestHeader, Long> {
    List<QuotationRequestHeader> findByStatus(QuotationRequestStatus status);

    List<QuotationRequestHeader> findByStatusAndClient(QuotationRequestStatus status, Client client);

    List<QuotationRequestHeader> findByStatusAndClientManagerEmployeeId(QuotationRequestStatus status,
                                                                        Long managerEmployeeId);
}
