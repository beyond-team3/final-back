package com.monsoon.seedflowplus.domain.dashboard.admin.controller;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.dashboard.admin.dto.AdminDashboardResponse;
import com.monsoon.seedflowplus.domain.dashboard.admin.service.AdminDashboardService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "관리자 대시보드")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService service;

    /**
     * GET /api/dashboard/admin
     * Vue: getAdminDashboard() 호출 대상
     */
    @Operation(summary = "관리자 대시보드 조회")
    @GetMapping("/admin")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (userDetails.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(service.getDashboard());
    }
}