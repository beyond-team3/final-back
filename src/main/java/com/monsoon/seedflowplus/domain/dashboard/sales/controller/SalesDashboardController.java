package com.monsoon.seedflowplus.domain.dashboard.sales.controller;

import com.monsoon.seedflowplus.domain.dashboard.sales.dto.SalesDashboardResponse;
import com.monsoon.seedflowplus.domain.dashboard.sales.service.SalesDashboardService;
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

@Tag(name = "Dashboard", description = "영업사원 대시보드")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class SalesDashboardController {

    private final SalesDashboardService service;

    /**
     * GET /api/dashboard/sales
     * Vue: getSalesRepDashboard() 호출 대상
     */
    @Operation(summary = "영업사원 대시보드 조회")
    @GetMapping("/sales")
    public ResponseEntity<SalesDashboardResponse> getSalesDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || userDetails.getEmployeeId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long employeeId = userDetails.getEmployeeId();

        return ResponseEntity.ok(service.getDashboard(employeeId));
    }
}