package com.monsoon.seedflowplus.core.common.support.error;

public interface ErrorCodeException {

    ErrorCodeProvider getErrorCodeProvider();

    Object getData();

    static ErrorCodeProvider from(Exception e) {
        if (e instanceof ErrorCodeException errorCodeException) {
            return errorCodeException.getErrorCodeProvider();
        }
        throw new IllegalArgumentException("지원되지 않는 예외 타입입니다: " + e.getClass().getName());
    }
}
