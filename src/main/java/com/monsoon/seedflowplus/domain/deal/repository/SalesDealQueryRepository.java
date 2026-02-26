package com.monsoon.seedflowplus.domain.deal.repository;

import com.monsoon.seedflowplus.domain.deal.entity.SalesDeal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SalesDealQueryRepository {

    Page<SalesDeal> searchDeals(SalesDealSearchCondition cond, Pageable pageable);
}
