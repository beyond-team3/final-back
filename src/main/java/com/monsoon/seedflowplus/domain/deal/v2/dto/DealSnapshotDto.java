package com.monsoon.seedflowplus.domain.deal.v2.dto;

import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.v2.common.DocumentApprovalStatus;
import com.monsoon.seedflowplus.domain.deal.v2.common.DocumentLifecycleStatus;
import com.monsoon.seedflowplus.domain.deal.v2.common.DocumentRole;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DealSnapshotDto {

    private DealStage currentStage;
    private String currentStatus;
    private DealType representativeDocumentType;
    private Long representativeDocumentId;
    private DocumentLifecycleStatus lifecycleStatus;
    private DocumentApprovalStatus approvalStatus;
    private DocumentRole documentRole;
    private LocalDateTime lastActivityAt;
}
