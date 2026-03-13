package com.monsoon.seedflowplus.domain.sales.quotation.repository;

import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuotationRepository extends JpaRepository<QuotationHeader, Long> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE QuotationHeader q SET q.status = :newStatus " +
            "WHERE q.status = :oldStatus AND q.expiredDate <= :today")
    int updateStatusForExpiration(@Param("oldStatus") QuotationStatus oldStatus,
                                  @Param("newStatus") QuotationStatus newStatus,
                                  @Param("today") LocalDate today);

    Optional<QuotationHeader> findByQuotationCode(String quotationCode);

    List<QuotationHeader> findByQuotationRequestId(Long quotationRequestId);

    List<QuotationHeader> findByDealId(Long dealId);

    List<QuotationHeader> findAllByStatus(QuotationStatus status);

    List<QuotationHeader> findAllByStatusAndAuthorId(QuotationStatus status, Long authorId);

    @Query("SELECT q FROM QuotationHeader q WHERE q.author.id = :authorId AND q.status IN :statuses")
    List<QuotationHeader> findByAuthorIdAndStatuses(@Param("authorId") Long authorId, @Param("statuses") List<QuotationStatus> statuses);

    /**
     * 반려/만료된 건 중 재작성이 필요한 '활성' 건만 조회합니다.
     * 1. 해당 Deal에 진행 중(승인 대기 등)인 다른 견적서가 없어야 함.
     * 2. 동일 Deal 내 반려 건 중 가장 최신(ID 기준) 건만 노출.
     */
    @Query("SELECT q FROM QuotationHeader q " +
            "WHERE (q.author.id = :employeeId OR q.client.managerEmployee.id = :employeeId) " +
            "AND q.status IN :statuses " +
            "AND q.id = (SELECT MAX(q2.id) FROM QuotationHeader q2 " +
            "            WHERE q2.deal.id = q.deal.id " +
            "            AND q2.status <> :deletedStatus) " +
            "AND NOT EXISTS (SELECT 1 FROM QuotationHeader q3 " +
            "                WHERE q3.deal.id = q.deal.id " +
            "                AND q3.id > q.id " +
            "                AND q3.status NOT IN :statuses " +
            "                AND q3.status <> :deletedStatus)")
    List<QuotationHeader> findActiveRejectedQuotations(
            @Param("employeeId") Long employeeId,
            @Param("statuses") List<QuotationStatus> statuses,
            @Param("deletedStatus") QuotationStatus deletedStatus);

    // 상태와 만료일을 기준으로 견적서 조회
    List<QuotationHeader> findByStatusAndExpiredDateLessThanEqual(QuotationStatus status, java.time.LocalDate date);
}
