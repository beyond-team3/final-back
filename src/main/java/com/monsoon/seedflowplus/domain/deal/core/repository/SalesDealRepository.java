package com.monsoon.seedflowplus.domain.deal.core.repository;

import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesDealRepository extends JpaRepository<SalesDeal, Long>, SalesDealQueryRepository {
}
