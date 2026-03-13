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

    // 상태와 만료일을 기준으로 견적서 조회
    List<QuotationHeader> findByStatusAndExpiredDateLessThanEqual(QuotationStatus status, java.time.LocalDate date);
}
