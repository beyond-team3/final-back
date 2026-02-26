package com.monsoon.seedflowplus.domain.deal.service;

import com.monsoon.seedflowplus.domain.account.entity.Role;

// TODO: 인증 Principal 타입이 정리되면 교체
// domain.account.entity.User(loginId, role, employee, client)를 기준으로 필요한 식별자만 담는다.
public record TempUser(
        Long userId,
        String loginId,
        Role role,
        Long employeeId,
        Long clientId
) {
}
