package com.monsoon.seedflowplus.domain.deal.v2.dto;

import com.monsoon.seedflowplus.domain.deal.common.DealType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DealDocumentCommandResultDto {

    private Long dealId;
    private DealType documentType;
    private Long documentId;
    private String documentCode;
    private RevisionInfoDto revisionInfo;
}
