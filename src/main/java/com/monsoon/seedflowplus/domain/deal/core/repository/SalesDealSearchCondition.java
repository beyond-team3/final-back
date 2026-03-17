package com.monsoon.seedflowplus.domain.deal.core.repository;

import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class SalesDealSearchCondition {

    private Long ownerEmpId;
    private Long clientId;
    private Boolean clientPostAdminApprovalOnly;
    private DealStage currentStage;
    private DealType latestDocType;
    private Boolean isClosed;
    private String keyword;
    private LocalDateTime fromAt;
    private LocalDateTime toAt;
}
