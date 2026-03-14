package com.monsoon.seedflowplus.domain.deal.v2.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RevisionInfoDto {

    private Long sourceDocumentId;
    private String revisionGroupKey;
    private Integer revisionNo;
    private Integer latestRevisionNo;
    private boolean revisionStartable;
}
