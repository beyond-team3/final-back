package com.monsoon.seedflowplus.core.common.support.error;

import lombok.Getter;

@Getter
public class ErrorMessage {
    private final String code;
    private final String message;
    private final Object data;

    public ErrorMessage(ErrorCodeProvider errorCode) {
        this.code = String.valueOf(errorCode.getCode());
        this.message = errorCode.getMessage();
        this.data = null;
    }

    public ErrorMessage(ErrorCodeProvider errorCode, Object data) {
        this.code = String.valueOf(errorCode.getCode());
        this.message = errorCode.getMessage();
        this.data = data;
    }
}
