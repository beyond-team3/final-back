package com.monsoon.seedflowplus.domain.deal.core.dto.response;

import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SalesDealListItemDto {

    private Long dealId;
    private Long clientId;
    private String clientName;
    private Long ownerEmpId;
    private String ownerEmpName;
    private DealStage currentStage;
    private String currentStatus;
    private DealType latestDocType;
    private Long latestRefId;
    private String latestTargetCode;
    private LocalDateTime lastActivityAt;
    private LocalDateTime closedAt;
    private String summaryMemo;
}
