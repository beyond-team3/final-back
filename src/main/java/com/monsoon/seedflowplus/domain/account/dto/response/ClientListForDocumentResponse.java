package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.domain.account.entity.Client;

public record ClientListForDocumentResponse(
        String clientCode,
        String clientName,
        String managerName) {
    public static ClientListForDocumentResponse from(Client client) {
        if (client == null) {
            return null;
        }
        return new ClientListForDocumentResponse(
                client.getClientCode(),
                client.getClientName(),
                client.getManagerName());
    }
}
