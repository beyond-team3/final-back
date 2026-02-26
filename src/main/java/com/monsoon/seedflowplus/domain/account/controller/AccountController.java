package com.monsoon.seedflowplus.domain.account.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.account.dto.request.*;
import com.monsoon.seedflowplus.domain.account.dto.response.ClientCropResponse;
import com.monsoon.seedflowplus.domain.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/users/create")
    public ApiResult<?> createAccount(@RequestBody @Valid UserCreateRequest request) {
        accountService.createAccount(request);
        return ApiResult.success();
    }

    @PatchMapping("/users/status")
    public ApiResult<?> updateStatus(@RequestBody @Valid UserStatusUpdateRequest request) {
        accountService.updateUserStatus(request);
        return ApiResult.success();
    }

    @PatchMapping("/clients/{clientId}")
    public ApiResult<?> updateClientInfo(@PathVariable Long clientId, @RequestBody @Valid ClientUpdateRequest request) {
        accountService.updateClientInfo(clientId, request);
        return ApiResult.success();
    }

    @GetMapping("/clients/{clientId}/crops")
    public ApiResult<List<ClientCropResponse>> getClientCrops(@PathVariable Long clientId) {
        return ApiResult.success(accountService.getClientCrops(clientId));
    }
}
