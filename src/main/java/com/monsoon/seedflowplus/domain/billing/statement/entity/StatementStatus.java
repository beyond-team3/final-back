package com.monsoon.seedflowplus.domain.billing.statement.entity;

import com.monsoon.seedflowplus.domain.pipeline.entity.DocumentStatus;

public enum StatementStatus implements DocumentStatus {
    ISSUED,    // 발급
    CANCELED   // 취소
}
