package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Status;

import java.time.LocalDateTime;

public record EmployeeManagedClientResponse(
        Long clientId,
        String clientCode,
        String clientName,
        Status accountStatus,
        LocalDateTime lastLoginAt) {
    public static EmployeeManagedClientResponse from(Client client) {
        return new EmployeeManagedClientResponse(
                client.getId(),
                client.getClientCode(),
                client.getClientName(),
                client.getAccount() != null ? client.getAccount().getStatus() : Status.DEACTIVATE,
                client.getAccount() != null ? client.getAccount().getLastLoginAt() : null);
    }
}
