package com.monsoon.seedflowplus.domain.deal.core.repository;

import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalesDealRepository extends JpaRepository<SalesDeal, Long>, SalesDealQueryRepository {

    Optional<SalesDeal> findTopByClientIdAndClosedAtIsNullOrderByLastActivityAtDesc(Long clientId);

    Optional<SalesDeal> findTopByClientIdOrderByLastActivityAtDesc(Long clientId);
}
