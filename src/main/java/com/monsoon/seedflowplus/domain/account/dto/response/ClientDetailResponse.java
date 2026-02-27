package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import com.monsoon.seedflowplus.domain.account.entity.Status;

import java.math.BigDecimal;

public record ClientDetailResponse(
        String clientCode,
        Status status,
        String clientName,
        String clientBrn,
        String ceoName,
        String companyPhone,
        String address,
        ClientType clientType,
        String managerName,
        String managerPhone,
        String managerEmail,
        BigDecimal totalCredit,
        BigDecimal usedCredit) {
    public static ClientDetailResponse from(Client client) {
        Status accountStatus = client.getAccount() != null ? client.getAccount().getStatus() : null;

        return new ClientDetailResponse(
                client.getClientCode(),
                accountStatus,
                client.getClientName(),
                client.getClientBrn(),
                client.getCeoName(),
                client.getCompanyPhone(),
                client.getAddress(),
                client.getClientType(),
                client.getManagerName(),
                client.getManagerPhone(),
                client.getManagerEmail(),
                client.getTotalCredit(),
                client.getUsedCredit());
    }
}
