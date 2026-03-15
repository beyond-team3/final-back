package com.monsoon.seedflowplus.core.common.support.error;

public class CoreException extends ErrorCodeRuntimeException {

    private final ErrorType errorType;

    private final Object data;

    public CoreException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.data = null;
    }

    public CoreException(ErrorType errorType, Object data) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.data = data;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    @Override
    public ErrorCodeProvider getErrorCodeProvider() {
        return errorType;
    }

    public Object getData() {
        return data;
    }

}
