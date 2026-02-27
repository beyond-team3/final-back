package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.domain.account.entity.Client;

public record UnregisteredClientResponse(
        Long clientId,
        String clientCode,
        String clientName) {
    public static UnregisteredClientResponse from(Client client) {
        return new UnregisteredClientResponse(
                client.getId(),
                client.getClientCode(),
                client.getClientName());
    }
}
