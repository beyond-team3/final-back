package com.monsoon.seedflowplus.domain.deal.common.error;

import com.monsoon.seedflowplus.core.common.support.error.ErrorCodeProvider;
import com.monsoon.seedflowplus.core.common.support.error.ErrorCodeRuntimeException;

public class DealException extends ErrorCodeRuntimeException {

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

    @Override
    public ErrorCodeProvider getErrorCodeProvider() {
        return errorCode;
    }

    @Override
    public Object getData() {
        return data;
    }
}
