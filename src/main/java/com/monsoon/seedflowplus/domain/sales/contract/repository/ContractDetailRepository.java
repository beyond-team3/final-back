package com.monsoon.seedflowplus.domain.sales.contract.repository;

import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractDetailRepository extends JpaRepository<ContractDetail, Long> {
}
