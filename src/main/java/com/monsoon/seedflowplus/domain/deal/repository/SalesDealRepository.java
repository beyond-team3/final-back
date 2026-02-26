package com.monsoon.seedflowplus.domain.deal.repository;

import com.monsoon.seedflowplus.domain.deal.entity.SalesDeal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesDealRepository extends JpaRepository<SalesDeal, Long>, SalesDealQueryRepository {
}
