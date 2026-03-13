package com.monsoon.seedflowplus.domain.deal.core.repository;

import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface SalesDealRepository extends JpaRepository<SalesDeal, Long>, SalesDealQueryRepository {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM SalesDeal d WHERE d.id = :id")
    Optional<SalesDeal> findByIdWithLock(@Param("id") Long id);

    Optional<SalesDeal> findTopByClientIdAndClosedAtIsNullOrderByLastActivityAtDesc(Long clientId);

    Optional<SalesDeal> findTopByClientIdOrderByLastActivityAtDesc(Long clientId);
}
