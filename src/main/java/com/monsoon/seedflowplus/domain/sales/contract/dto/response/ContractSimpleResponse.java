package com.monsoon.seedflowplus.domain.sales.contract.dto.response;

import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import java.time.LocalDate;

public record ContractSimpleResponse(
        Long id,
        String contractCode,
        String status,
        LocalDate startDate,
        LocalDate endDate) {
    
    public static ContractSimpleResponse from(ContractHeader entity) {
        return new ContractSimpleResponse(
                entity.getId(),
                entity.getContractCode(),
                entity.getStatus().name(),
                entity.getStartDate(),
                entity.getEndDate()
        );
    }
}
