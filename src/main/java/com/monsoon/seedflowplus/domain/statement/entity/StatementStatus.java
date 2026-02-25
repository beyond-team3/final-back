package com.monsoon.seedflowplus.domain.statement.entity;

import com.monsoon.seedflowplus.domain.pipeline.enums.DocumentStatus;

public enum StatementStatus implements DocumentStatus {
    ISSUED,    // 발급
    CANCELED   // 취소
}
