package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.core.common.util.AddressParser;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import com.monsoon.seedflowplus.domain.account.entity.Status;

import java.math.BigDecimal;

public record ClientDetailResponse(
        Long id,
        String clientCode,
        Status status,
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
        String managerEmail,
        Long managerId,
        String managerEmployeeName, // 추가
        BigDecimal totalCredit,
        BigDecimal usedCredit,
        Long accountId) {
    public static ClientDetailResponse from(Client client) {
        Status accountStatus = client.getAccount() != null ? client.getAccount().getStatus() : null;
        Long accountId = client.getAccount() != null ? client.getAccount().getId() : null;
        AddressParser.AddressInfo addressInfo = AddressParser.parse(client.getAddress());
        Long managerEmployeeId = client.getManagerEmployee() != null ? client.getManagerEmployee().getId() : null;
        String managerEmployeeName = client.getManagerEmployee() != null ? client.getManagerEmployee().getEmployeeName()
                : null;

        return new ClientDetailResponse(
                client.getId(),
                client.getClientCode(),
                accountStatus,
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
                client.getManagerEmail(),
                managerEmployeeId,
                managerEmployeeName,
                client.getTotalCredit(),
                client.getUsedCredit(),
                accountId);
    }
}
