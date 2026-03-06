package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.core.common.util.AddressParser;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;

import java.time.LocalDateTime;

public record EmployeeDetailResponse(
        Long employeeId,
        String employeeCode,
        String employeeName,
        Role role,
        String employeeEmail,
        String employeePhone,
        LocalDateTime createdAt,
        String addressDetail,
        String addressZip,
        Status status) {
    public static EmployeeDetailResponse from(User user) {
        if (user.getEmployee() == null) {
            return null;
        }
        AddressParser.AddressInfo addressInfo = AddressParser.parse(user.getEmployee().getAddress());

        return new EmployeeDetailResponse(
                user.getEmployee().getId(),
                user.getEmployee().getEmployeeCode(),
                user.getEmployee().getEmployeeName(),
                user.getRole(),
                user.getEmployee().getEmployeeEmail(),
                user.getEmployee().getEmployeePhone(),
                user.getEmployee().getCreatedAt(),
                addressInfo.detail(),
                addressInfo.zip(),
                user.getStatus());
    }
}
