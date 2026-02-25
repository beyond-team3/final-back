package com.monsoon.seedflowplus.domain.sales.contract.entity;

import com.monsoon.seedflowplus.domain.pipeline.entity.DocumentStatus;

public enum ContractStatus implements DocumentStatus {
    WAITING_ADMIN,
    REJECTED_ADMIN,
    WAITING_CLIENT,
    REJECTED_CLIENT,
    COMPLETED,
    EXPIRED
}
