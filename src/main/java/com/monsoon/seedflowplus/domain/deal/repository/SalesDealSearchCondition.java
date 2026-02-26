package com.monsoon.seedflowplus.domain.deal.repository;

import com.monsoon.seedflowplus.domain.deal.entity.DealStage;
import com.monsoon.seedflowplus.domain.deal.entity.DealType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class SalesDealSearchCondition {

    private Long ownerEmpId;
    private Long clientId;
    private DealStage currentStage;
    private DealType latestDocType;
    private Boolean isClosed;
    private String keyword;
    private LocalDateTime fromAt;
    private LocalDateTime toAt;
}
