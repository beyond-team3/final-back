package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;

public record ClientProfileResponse(
        String clientCode,
        String clientName,
        String clientBrn,
        String ceoName,
        String companyPhone,
        String address,
        ClientType clientType,
        String managerName,
        String managerPhone,
        String managerEmail) {
    public static ClientProfileResponse from(Client client) {
        return new ClientProfileResponse(
                client.getClientCode(),
                client.getClientName(),
                client.getClientBrn(),
                client.getCeoName(),
                client.getCompanyPhone(),
                client.getAddress(),
                client.getClientType(),
                client.getManagerName(),
                client.getManagerPhone(),
                client.getManagerEmail());
    }
}
