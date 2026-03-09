package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.core.common.util.AddressParser;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import com.monsoon.seedflowplus.domain.account.entity.Status;

public record ClientListResponse(
        Long id,
        String clientCode,
        String clientName,
        ClientType clientType,
        String managerName,
        Long managerId,
        String addressSido,
        Status status,
        Long accountId) {
    public static ClientListResponse from(Client client) {
        AddressParser.AddressInfo addressInfo = AddressParser.parse(client.getAddress());
        Status accountStatus = client.getAccount() != null ? client.getAccount().getStatus() : null;
        Long accountId = client.getAccount() != null ? client.getAccount().getId() : null;
        Long managerEmployeeId = client.getManagerEmployee() != null ? client.getManagerEmployee().getId() : null;

        return new ClientListResponse(
                client.getId(),
                client.getClientCode(),
                client.getClientName(),
                client.getClientType(),
                client.getManagerName(),
                managerEmployeeId,
                addressInfo.sido(),
                accountStatus,
                accountId);
    }
}
