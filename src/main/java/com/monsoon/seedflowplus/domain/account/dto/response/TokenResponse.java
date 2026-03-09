package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.domain.account.entity.Role;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        Role role) {
    public static TokenResponse of(String accessToken, String refreshToken, Long expiresIn, Role role) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn, role);
    }
}