package com.monsoon.seedflowplus.domain.deal.log.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DealLogDetailDto {

    private Long dealLogId;
    private String targetCode;
    private String reason;
    private String diffJson;
    private LocalDateTime createdAt;
}
