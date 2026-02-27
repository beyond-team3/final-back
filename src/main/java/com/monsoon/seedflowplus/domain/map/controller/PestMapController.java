package com.monsoon.seedflowplus.domain.map.controller;

import com.monsoon.seedflowplus.domain.map.dto.request.PestMapSearchRequest;
import com.monsoon.seedflowplus.domain.map.dto.response.PestMapSearchResponse;
import com.monsoon.seedflowplus.domain.map.dto.response.SalesOfficeResponse;
import com.monsoon.seedflowplus.domain.map.service.PestMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class PestMapController {

    private final PestMapService pestMapService;

    @GetMapping("/forecasts")
    public ResponseEntity<PestMapSearchResponse> getForecasts(@ModelAttribute PestMapSearchRequest request) {
        return ResponseEntity.ok(pestMapService.getPestMapData(request));
    }

    @GetMapping("/offices")
    public ResponseEntity<List<SalesOfficeResponse>> getSalesOffices() {
        return ResponseEntity.ok(pestMapService.getAllSalesOffices());
    }
}