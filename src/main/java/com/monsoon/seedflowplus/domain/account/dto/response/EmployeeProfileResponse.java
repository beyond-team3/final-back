package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.core.common.util.AddressParser;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.User;

import java.time.LocalDateTime;

public record EmployeeProfileResponse(
        Long employeeId,
        Role role,
        String employeeName,
        String employeeCode,
        String employeeEmail,
        String employeePhone,
        LocalDateTime createdAt,
        String addressDetail,
        String addressZip) {
    public static EmployeeProfileResponse from(User user) {
        AddressParser.AddressInfo addressInfo = AddressParser.parse(user.getEmployee().getAddress());

        return new EmployeeProfileResponse(
                user.getEmployee().getId(),
                user.getRole(),
                user.getEmployee().getEmployeeName(),
                user.getEmployee().getEmployeeCode(),
                user.getEmployee().getEmployeeEmail(),
                user.getEmployee().getEmployeePhone(),
                user.getEmployee().getCreatedAt(),
                addressInfo.detail(),
                addressInfo.zip());
    }
}
