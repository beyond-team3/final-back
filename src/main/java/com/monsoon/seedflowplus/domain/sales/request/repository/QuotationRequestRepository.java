package com.monsoon.seedflowplus.domain.sales.request.repository;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuotationRequestRepository extends JpaRepository<QuotationRequestHeader, Long> {
    @EntityGraph(attributePaths = "client")
    List<QuotationRequestHeader> findByStatus(QuotationRequestStatus status);

    @EntityGraph(attributePaths = "client")
    List<QuotationRequestHeader> findByStatusAndClient(QuotationRequestStatus status, Client client);

    @EntityGraph(attributePaths = "client")
    List<QuotationRequestHeader> findByStatusAndClientManagerEmployeeId(QuotationRequestStatus status, Long managerEmployeeId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE QuotationRequestHeader r SET r.status = :newStatus " +
            "WHERE r.status = :oldStatus " +
            "AND EXISTS (SELECT 1 FROM QuotationHeader q WHERE q.quotationRequest = r AND q.status = :quoStatus)")
    int recoverStatusByExpiredQuotation(@Param("oldStatus") QuotationRequestStatus oldStatus,
                                        @Param("newStatus") QuotationRequestStatus newStatus,
                                        @Param("quoStatus") com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus quoStatus);
}
