package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.domain.account.entity.Client;

public record ClientListForDocumentResponse(
        Long id,
        String clientCode,
        String clientName,
        String managerName) {
    public static ClientListForDocumentResponse from(Client client) {
        if (client == null) {
            return null;
        }
        return new ClientListForDocumentResponse(
                client.getId(),
                client.getClientCode(),
                client.getClientName(),
                client.getManagerName());
    }
}
