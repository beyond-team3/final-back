package com.monsoon.seedflowplus.domain.sales.contract.repository;

import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository extends JpaRepository<ContractHeader, Long> {

    List<ContractHeader> findAllByStatus(ContractStatus status);
}
