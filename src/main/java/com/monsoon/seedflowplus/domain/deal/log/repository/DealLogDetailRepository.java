package com.monsoon.seedflowplus.domain.deal.log.repository;

import com.monsoon.seedflowplus.domain.deal.log.entity.DealLogDetail;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DealLogDetailRepository extends JpaRepository<DealLogDetail, Long> {

    @Query("select d from DealLogDetail d where d.dealLog.id = :dealLogId")
    Optional<DealLogDetail> findByDealLogId(@Param("dealLogId") Long dealLogId);
}
