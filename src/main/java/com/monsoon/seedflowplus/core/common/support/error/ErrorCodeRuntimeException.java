package com.monsoon.seedflowplus.core.common.support.error;

public abstract class ErrorCodeRuntimeException extends RuntimeException implements ErrorCodeException {

    protected ErrorCodeRuntimeException(String message) {
        super(message);
    }
}
