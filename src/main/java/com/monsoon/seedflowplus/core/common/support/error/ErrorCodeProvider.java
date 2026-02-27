package com.monsoon.seedflowplus.core.common.support.error;

import org.springframework.http.HttpStatus;

public interface ErrorCodeProvider {

    Object getCode();

    String getMessage();

    HttpStatus getHttpStatus();
}
