package com.monsoon.seedflowplus.domain.dashboard.client.controller;

import com.monsoon.seedflowplus.domain.dashboard.client.dto.ClientDashboardResponse;
import com.monsoon.seedflowplus.domain.dashboard.client.service.ClientDashboardService;
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

@Tag(name = "Dashboard", description = "거래처 대시보드")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class ClientDashboardController {

    private final ClientDashboardService service;

    /**
     * GET /api/dashboard/client
     * Vue: getClientDashboard() 호출 대상
     */
    @Operation(summary = "거래처 대시보드 조회")
    @GetMapping("/client")
    public ResponseEntity<ClientDashboardResponse> getClientDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || userDetails.getClientId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long clientId = userDetails.getClientId();
        Long userId   = userDetails.getUserId();

        return ResponseEntity.ok(service.getDashboard(clientId, userId));
    }
}