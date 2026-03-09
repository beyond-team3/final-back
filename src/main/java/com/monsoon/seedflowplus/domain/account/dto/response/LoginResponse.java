package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답")
public record LoginResponse (

    @Schema(description = "Access Token")
    String accessToken,

    @Schema(description = "Refresh Token")
    String refreshToken,

    @Schema(description = "Access Token 만료 시간 (ms)")
    Long accessTokenExpiresAt,

    @Schema(description = "사용자 권한")
    Role role,

    @Schema(description = "로그인 ID")
    String loginId,

    @Schema(description = "사용자 이름")
    String userName
){}
