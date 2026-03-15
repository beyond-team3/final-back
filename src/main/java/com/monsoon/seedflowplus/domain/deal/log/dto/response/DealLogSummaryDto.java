package com.monsoon.seedflowplus.domain.deal.log.dto.response;

import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DealLogSummaryDto {

    private Long dealLogId;
    private DealType docType;
    private Long refId;
    private String targetCode;
    private DealStage fromStage;
    private DealStage toStage;
    private String fromStatus;
    private String toStatus;
    private ActionType actionType;
    private LocalDateTime actionAt;
    private ActorType actorType;
    private Long actorId;
}
