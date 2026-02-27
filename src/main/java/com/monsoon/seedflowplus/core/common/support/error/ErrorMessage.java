package com.monsoon.seedflowplus.core.common.support.error;

import com.monsoon.seedflowplus.domain.deal.common.error.DealErrorCode;
import lombok.Getter;

@Getter
public class ErrorMessage {
    private final String code;
    private final String message;
    private final Object data;

    public ErrorMessage(ErrorType errorType) {
        this.code = errorType.getCode().name();
        this.message = errorType.getMessage();
        this.data = null;
    }

    public ErrorMessage(ErrorType errorType, Object data) {
        this.code = errorType.getCode().name();
        this.message = errorType.getMessage();
        this.data = data;
    }

    public ErrorMessage(DealErrorCode errorCode) {
        this.code = errorCode.name();
        this.message = errorCode.getMessage();
        this.data = null;
    }

    public ErrorMessage(DealErrorCode errorCode, Object data) {
        this.code = errorCode.name();
        this.message = errorCode.getMessage();
        this.data = data;
    }
}
