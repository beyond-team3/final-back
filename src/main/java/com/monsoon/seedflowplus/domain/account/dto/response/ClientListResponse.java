package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import com.monsoon.seedflowplus.domain.account.entity.Status;

public record ClientListResponse(
        String clientCode,
        String clientName,
        ClientType clientType,
        String managerName,
        String region,
        Status status) {
    public static ClientListResponse from(Client client) {
        String region = extractRegion(client.getAddress());
        Status accountStatus = client.getAccount() != null ? client.getAccount().getStatus() : null;

        return new ClientListResponse(
                client.getClientCode(),
                client.getClientName(),
                client.getClientType(),
                client.getManagerName(),
                region,
                accountStatus);
    }

    private static String extractRegion(String address) {
        if (address == null || address.isBlank()) {
            return "";
        }
        String[] parts = address.split("\\s+");
        if (parts.length >= 2) {
            return parts[0] + " " + parts[1];
        } else if (parts.length == 1) {
            return parts[0];
        }
        return "";
    }
}
