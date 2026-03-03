package com.monsoon.seedflowplus.domain.scoring.controller;

import com.monsoon.seedflowplus.domain.scoring.dto.AccountPriorityResponse;
import com.monsoon.seedflowplus.domain.scoring.service.ScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/scoring")
@RequiredArgsConstructor
public class ScoringController {

    private final ScoringService scoringService;

    @GetMapping("/priority-list")
    public ResponseEntity<List<AccountPriorityResponse>> getPriorityList() {
        return ResponseEntity.ok(scoringService.getRankedAccounts());
    }
}
