package com.monsoon.seedflowplus.domain.sales.rfq.entity;

import com.monsoon.seedflowplus.domain.pipeline.entity.DocumentStatus;

public enum RfqStatus implements DocumentStatus {
    PENDING,
    REVIEWING,
    COMPLETED,
    CANCELED
}
