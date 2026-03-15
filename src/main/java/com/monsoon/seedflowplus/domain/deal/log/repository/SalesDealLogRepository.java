package com.monsoon.seedflowplus.domain.deal.log.repository;

import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.log.entity.SalesDealLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SalesDealLogRepository extends JpaRepository<SalesDealLog, Long> {

    Sort DEFAULT_TIMELINE_SORT = Sort.by(
            Sort.Order.desc("actionAt"),
            Sort.Order.asc("targetCode").nullsLast(),
            Sort.Order.asc("id")
    );

    @Query("select l from SalesDealLog l where l.deal.id = :dealId")
    Page<SalesDealLog> findByDealId(@Param("dealId") Long dealId, Pageable pageable);

    @Query("select l from SalesDealLog l where l.deal.id = :dealId and l.deal.ownerEmp.id = :ownerEmpId")
    Page<SalesDealLog> findByDealIdAndOwnerEmpId(
            @Param("dealId") Long dealId,
            @Param("ownerEmpId") Long ownerEmpId,
            Pageable pageable
    );

    @Query("select l from SalesDealLog l where l.deal.id = :dealId and l.client.id = :clientId")
    Page<SalesDealLog> findByDealIdAndClientId(
            @Param("dealId") Long dealId,
            @Param("clientId") Long clientId,
            Pageable pageable
    );

    @Query("select l from SalesDealLog l where l.client.id = :clientId")
    Page<SalesDealLog> findByClientId(@Param("clientId") Long clientId, Pageable pageable);

    @Query("select l from SalesDealLog l where l.client.id = :clientId and l.deal.ownerEmp.id = :ownerEmpId")
    Page<SalesDealLog> findByClientIdAndOwnerEmpId(
            @Param("clientId") Long clientId,
            @Param("ownerEmpId") Long ownerEmpId,
            Pageable pageable
    );

    @Query("select l from SalesDealLog l where l.docType = :docType and l.refId = :refId")
    Page<SalesDealLog> findByDocTypeAndRefId(@Param("docType") DealType docType, @Param("refId") Long refId, Pageable pageable);

    @Query("select l from SalesDealLog l where l.docType = :docType and l.refId = :refId and l.deal.ownerEmp.id = :ownerEmpId")
    Page<SalesDealLog> findByDocTypeAndRefIdAndOwnerEmpId(
            @Param("docType") DealType docType,
            @Param("refId") Long refId,
            @Param("ownerEmpId") Long ownerEmpId,
            Pageable pageable
    );

    @Query("select l from SalesDealLog l where l.docType = :docType and l.refId = :refId and l.client.id = :clientId")
    Page<SalesDealLog> findByDocTypeAndRefIdAndClientId(
            @Param("docType") DealType docType,
            @Param("refId") Long refId,
            @Param("clientId") Long clientId,
            Pageable pageable
    );

    Optional<SalesDealLog> findTopByDocTypeAndRefIdOrderByActionAtDescIdDesc(DealType docType, Long refId);

    @Query("""
            select l
            from SalesDealLog l
            where l.deal.id = :dealId
              and l.docType = :docType
              and l.refId = :refId
            order by l.actionAt desc, l.id desc
            """)
    List<SalesDealLog> findRecentByDealIdAndDocTypeAndRefId(
            @Param("dealId") Long dealId,
            @Param("docType") DealType docType,
            @Param("refId") Long refId,
            Pageable pageable
    );

    static Pageable withDefaultTimelineSort(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_TIMELINE_SORT);
    }
}
