package com.monsoon.seedflowplus.domain.account.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.account.dto.request.ClientRegisterRequest;
import com.monsoon.seedflowplus.domain.account.dto.request.EmployeeRegisterRequest;
import com.monsoon.seedflowplus.domain.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/clients/register")
    public ApiResult<?> registerClient(@RequestBody @Valid ClientRegisterRequest request) {
        accountService.registerClient(request);
        return ApiResult.success();
    }

    @PostMapping("/employees/register")
    public ApiResult<?> registerEmployee(@RequestBody @Valid EmployeeRegisterRequest request) {
        accountService.registerEmployee(request);
        return ApiResult.success();
    }
}
