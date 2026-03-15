package com.monsoon.seedflowplus.domain.deal.v2.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DealDetailDto {

    private Long dealId;
    private String dealCode;
    private String dealTitle;
    private Long clientId;
    private String clientName;
    private Long ownerEmpId;
    private String ownerEmpName;
    private String summaryMemo;
    private DealSnapshotDto snapshot;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
}
