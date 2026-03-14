package com.monsoon.seedflowplus.domain.sales.request.repository;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.QueryHints;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuotationRequestRepository extends JpaRepository<QuotationRequestHeader, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT r FROM QuotationRequestHeader r WHERE r.id = :id")
    Optional<QuotationRequestHeader> findByIdWithLock(@Param("id") Long id);

    List<QuotationRequestHeader> findByDealId(Long dealId);

    @EntityGraph(attributePaths = "client")
    List<QuotationRequestHeader> findByStatus(QuotationRequestStatus status);

    @EntityGraph(attributePaths = "client")
    List<QuotationRequestHeader> findByStatusAndClient(QuotationRequestStatus status, Client client);

    @EntityGraph(attributePaths = "client")
    List<QuotationRequestHeader> findByStatusAndClientManagerEmployeeId(QuotationRequestStatus status,
                                                                        Long managerEmployeeId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE QuotationRequestHeader r SET r.status = :newStatus " +
            "WHERE r.status = :oldStatus " +
            "AND EXISTS (SELECT 1 FROM QuotationHeader q WHERE q.quotationRequest = r AND q.status = :quoStatus) "
            +
            "AND NOT EXISTS (SELECT 1 FROM QuotationHeader q2 WHERE q2.quotationRequest = r AND q2.status <> :quoStatus)")
    int recoverStatusByExpiredQuotation(@Param("oldStatus") QuotationRequestStatus oldStatus,
                                        @Param("newStatus") QuotationRequestStatus newStatus,
                                        @Param("quoStatus") QuotationStatus quoStatus);

    @EntityGraph(attributePaths = "client")
    @Query("SELECT r FROM QuotationRequestHeader r " +
            "WHERE r.status = :status " +
            "AND r.client.managerEmployee.id = :managerId " +
            "AND EXISTS (SELECT 1 FROM QuotationHeader q WHERE q.quotationRequest = r) " +
            "AND NOT EXISTS (SELECT 1 FROM QuotationHeader q2 WHERE q2.quotationRequest = r " +
            "AND q2.status NOT IN (:rejectedStatuses))")
    List<QuotationRequestHeader> findRejectedRequests(
            @Param("status") QuotationRequestStatus status,
            @Param("managerId") Long managerId,
            @Param("rejectedStatuses") List<QuotationStatus> rejectedStatuses);
}
