package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Status;

public record EmployeeManagedClientResponse(
        String clientCode,
        String clientName,
        Status accountStatus) {
    public static EmployeeManagedClientResponse from(Client client) {
        return new EmployeeManagedClientResponse(
                client.getClientCode(),
                client.getClientName(),
                client.getAccount() != null ? client.getAccount().getStatus() : Status.DEACTIVATE);
    }
}
