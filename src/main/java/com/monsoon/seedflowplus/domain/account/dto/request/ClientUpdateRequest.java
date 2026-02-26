package com.monsoon.seedflowplus.domain.account.dto.request;

import com.monsoon.seedflowplus.domain.account.entity.ClientType;

import java.math.BigDecimal;

public record ClientUpdateRequest(
        String clientName,
        String clientBrn,
        String ceoName,
        String companyPhone,
        String address,
        ClientType clientType,
        String managerName,
        String managerPhone,
        String managerEmail,
        BigDecimal totalCredit) {
}
