package com.monsoon.seedflowplus.domain.account.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.account.dto.request.LoginRequest;
import com.monsoon.seedflowplus.domain.account.dto.request.RefreshRequest;
import com.monsoon.seedflowplus.domain.account.dto.response.TokenResponse;
import com.monsoon.seedflowplus.domain.account.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResult<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        return ApiResult.success(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResult<?> logout(@RequestBody @Valid RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ApiResult.success();
    }

    @PostMapping("/refresh")
    public ApiResult<TokenResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        return ApiResult.success(authService.refresh(request.refreshToken()));
    }
}
