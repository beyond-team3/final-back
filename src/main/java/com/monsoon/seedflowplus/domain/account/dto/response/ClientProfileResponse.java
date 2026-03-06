package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.core.common.util.AddressParser;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;

public record ClientProfileResponse(
        String clientCode,
        String clientName,
        String clientBrn,
        String ceoName,
        String companyPhone,
        String addressSido,
        String addressDetail,
        String addressZip,
        ClientType clientType,
        String managerName,
        String managerPhone,
        String managerEmail) {
    public static ClientProfileResponse from(Client client) {
        AddressParser.AddressInfo addressInfo = AddressParser.parse(client.getAddress());

        return new ClientProfileResponse(
                client.getClientCode(),
                client.getClientName(),
                client.getClientBrn(),
                client.getCeoName(),
                client.getCompanyPhone(),
                addressInfo.sido(),
                addressInfo.detail(),
                addressInfo.zip(),
                client.getClientType(),
                client.getManagerName(),
                client.getManagerPhone(),
                client.getManagerEmail());
    }
}
