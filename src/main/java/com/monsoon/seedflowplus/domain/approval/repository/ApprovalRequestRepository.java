package com.monsoon.seedflowplus.domain.approval.repository;

import com.monsoon.seedflowplus.domain.approval.entity.ApprovalRequest;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStatus;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    Optional<ApprovalRequest> findByDealTypeAndTargetId(DealType dealType, Long targetId);

    boolean existsByDealTypeAndTargetIdAndStatus(DealType dealType, Long targetId, ApprovalStatus status);

    @Query(
            value = """
                    select ar
                    from ApprovalRequest ar
                    where (:status is null or ar.status = :status)
                      and (:dealType is null or ar.dealType = :dealType)
                      and (:targetId is null or ar.targetId = :targetId)
                    """,
            countQuery = """
                    select count(ar)
                    from ApprovalRequest ar
                    where (:status is null or ar.status = :status)
                      and (:dealType is null or ar.dealType = :dealType)
                      and (:targetId is null or ar.targetId = :targetId)
                    """
    )
    Page<ApprovalRequest> search(
            @Param("status") ApprovalStatus status,
            @Param("dealType") DealType dealType,
            @Param("targetId") Long targetId,
            Pageable pageable
    );
}
