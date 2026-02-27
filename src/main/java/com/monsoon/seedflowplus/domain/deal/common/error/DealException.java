package com.monsoon.seedflowplus.domain.deal.common.error;

public class DealException extends RuntimeException {

    private final DealErrorCode errorCode;
    private final Object data;

    public DealException(DealErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.data = null;
    }

    public DealException(DealErrorCode errorCode, Object data) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.data = data;
    }

    public DealErrorCode getErrorCode() {
        return errorCode;
    }

    public Object getData() {
        return data;
    }
}
