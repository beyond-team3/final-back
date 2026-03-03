package com.monsoon.seedflowplus.domain.map.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SalesOfficeResponse {
    private String id;
    private String name;
    private Double lat;
    private Double lng;
    private Integer score;
    private String[] handledCrops;
}