package com.monsoon.seedflowplus.domain.account.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.account.dto.request.*;
import com.monsoon.seedflowplus.domain.account.dto.response.*;
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

    @PatchMapping("/employees/{employeeId}")
    public ApiResult<?> updateEmployeeInfo(@PathVariable Long employeeId, @RequestBody @Valid EmployeeUpdateRequest request) {
        accountService.updateEmployeeInfo(employeeId, request);
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

    @PostMapping("/clients/{clientId}/crops")
    public ApiResult<?> addClientCrop(@PathVariable Long clientId, @RequestBody @Valid ClientCropRequest request) {
        accountService.addClientCrop(clientId, request);
        return ApiResult.success();
    }

    @DeleteMapping("/clients/crops/{cropId}")
    public ApiResult<?> deleteClientCrop(@PathVariable Long cropId) {
        accountService.deleteClientCrop(cropId);
        return ApiResult.success();
    }

    @PatchMapping("/password")
    public ApiResult<?> changePassword(@RequestBody @Valid PasswordChangeRequest request) {
        accountService.changePassword(request);
        return ApiResult.success();
    }

    // 조회 - 영업사원
    @GetMapping("/employees")
    public ApiResult<List<EmployeeListResponse>> getAllEmployees() {
        return ApiResult.success(accountService.getAllEmployees());
    }

    @GetMapping("/employees/{employeeId}")
    public ApiResult<EmployeeDetailResponse> getEmployeeDetail(@PathVariable Long employeeId) {
        return ApiResult.success(accountService.getEmployeeDetail(employeeId));
    }

    // 조회 - 거래처
    @GetMapping("/clients/for-document")
    public ApiResult<List<ClientListForDocumentResponse>> getClientsForDocument() {
        return ApiResult.success(accountService.getClientsForDocument());
    }

    @GetMapping("/clients")
    public ApiResult<List<ClientListResponse>> getAllClients() {
        return ApiResult.success(accountService.getAllClients());
    }

    @GetMapping("/clients/{clientId}")
    public ApiResult<ClientDetailResponse> getClientDetail(@PathVariable Long clientId) {
        return ApiResult.success(accountService.getClientDetail(clientId));
    }

    // 마이페이지
    @GetMapping("/clients/me")
    public ApiResult<ClientProfileResponse> getMyClientProfile() {
        return ApiResult.success(accountService.getMyClientProfile());
    }

    // 마이페이지 - 거래처 -담당영업사원 정보 조회
    @GetMapping("/clients/{clientId}/manager")
    public ApiResult<AssignedEmployeeResponse> getAssignedEmployee(@PathVariable Long clientId) {
        return ApiResult.success(accountService.getAssignedEmployee(clientId));
    }

    // 계정등록 - 영업사원(미등록)
    @GetMapping("/employees/unregistered")
    public ApiResult<List<UnregisteredEmployeeResponse>> getUnregisteredEmployees() {
        return ApiResult.success(accountService.getUnregisteredEmployees());
    }

    // 계정등록 - 거래처(미등록)
    @GetMapping("/clients/unregistered")
    public ApiResult<List<UnregisteredClientResponse>> getUnregisteredClients() {
        return ApiResult.success(accountService.getUnregisteredClients());
    }

}
