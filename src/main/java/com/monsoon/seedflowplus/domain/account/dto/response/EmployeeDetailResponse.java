package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.core.common.util.AddressParser;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
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
        String addressSido,
        String addressDetail,
        String addressZip,
        Status status,
        Long accountId) {
    public static EmployeeDetailResponse from(User user) {
        AddressParser.AddressInfo addressInfo = AddressParser.parse(user.getEmployee().getAddress());

        return new EmployeeDetailResponse(
                user.getEmployee().getId(),
                user.getEmployee().getEmployeeCode(),
                user.getEmployee().getEmployeeName(),
                user.getRole(),
                user.getEmployee().getEmployeeEmail(),
                user.getEmployee().getEmployeePhone(),
                user.getEmployee().getCreatedAt(),
                addressInfo.sido(),
                addressInfo.detail(),
                addressInfo.zip(),
                user.getStatus(),
                user.getId());
    }

    public static EmployeeDetailResponse from(Employee employee) {
        AddressParser.AddressInfo addressInfo = AddressParser.parse(employee.getAddress());

        return new EmployeeDetailResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getEmployeeName(),
                null, // 계정이 없으므로 Role 없음
                employee.getEmployeeEmail(),
                employee.getEmployeePhone(),
                employee.getCreatedAt(),
                addressInfo.sido(),
                addressInfo.detail(),
                addressInfo.zip(),
                null, // 계정이 없으므로 Status 없음
                null); // 계정이 없으므로 accountId 없음
    }
}
